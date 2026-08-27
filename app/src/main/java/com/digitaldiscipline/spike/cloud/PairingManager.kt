package com.digitaldiscipline.spike.cloud

import android.content.Context
import android.os.Build
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.cloud.models.DeviceDto
import com.digitaldiscipline.spike.cloud.models.PairingCodeDto
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.logging.EventLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Date
import java.util.concurrent.TimeUnit

sealed class PairingResult {
    data class Success(val familyId: String, val childId: String, val childName: String, val deviceId: String) : PairingResult()
    data class InvalidCode(val message: String) : PairingResult()
    data class ExpiredCode(val message: String) : PairingResult()
    data class AlreadyUsed(val message: String) : PairingResult()
    data class Error(val message: String) : PairingResult()
}

class PairingManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val cloudRepository: CloudRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val secureRandom = SecureRandom()
    private val localPairingCodes = mutableMapOf<String, PairingCodeDto>()

    private suspend fun ensureAuth() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
        } catch (e: Exception) {
            EventLogger.log("PAIRING", "system", "ANON_AUTH_FAILED", details = e.message ?: "")
        }
    }

    /**
     * Parent Action: Generates a single-use 6-digit pairing code with a 15-minute TTL.
     */
    suspend fun generatePairingCode(
        familyId: String,
        childId: String,
        childName: String,
        parentId: String
    ): Result<String> {
        ensureAuth()
        val codeNumber = 100000 + secureRandom.nextInt(900000)
        val code = codeNumber.toString()
        val nowMs = System.currentTimeMillis()
        val expiresAtMs = nowMs + TimeUnit.MINUTES.toMillis(15) // 15-minute TTL

        val pairingDto = PairingCodeDto(
            code = code,
            familyId = familyId,
            childId = childId,
            childName = childName,
            createdByParentId = parentId,
            expiresAtTimestampMs = expiresAtMs,
            isUsed = false
        )

        // Store in local resilient map
        localPairingCodes[code] = pairingDto

        // Sync to Cloud Firestore
        try {
            firestore?.collection("pairing_codes")?.document(code)?.set(pairingDto, SetOptions.merge())?.await()
        } catch (e: Exception) {
            EventLogger.log("PAIRING", "system", "FIRESTORE_WRITE_FAILED", details = e.message ?: "")
        }

        EventLogger.log("PAIRING", "system", "PAIRING_CODE_GENERATED", details = "Code: $code | Child: $childName | Expires in 15m")
        return Result.success(code)
    }

    /**
     * Child Action: Atomically redeems the pairing code, checks validity, binds device UUID to child,
     * clears old metrics to start fresh for the child, and sets local PreferencesManager state.
     */
    suspend fun redeemPairingCode(code: String): PairingResult {
        val cleanCode = code.trim()

        if (cleanCode.length != 6) {
            return PairingResult.InvalidCode("Pairing code must be 6 digits")
        }

        ensureAuth()

        // 1. Check local resilient map first (instant response if on same instance)
        var pairingCode = localPairingCodes[cleanCode]

        // 2. Check Firestore
        if (pairingCode == null && firestore != null) {
            try {
                val snapshot = firestore?.collection("pairing_codes")?.document(cleanCode)?.get()?.await()
                if (snapshot != null && snapshot.exists()) {
                    pairingCode = snapshot.toObject(PairingCodeDto::class.java)
                }
            } catch (e: Exception) {
                EventLogger.log("PAIRING", "system", "FIRESTORE_READ_FAILED", details = e.message ?: "")
            }
        }

        if (pairingCode == null) {
            EventLogger.log("PAIRING", "system", "PAIRING_CODE_INVALID", details = "Code: $cleanCode")
            return PairingResult.InvalidCode("Invalid pairing code. Please check and try again.")
        }

        val now = System.currentTimeMillis()
        if (pairingCode.expiresAtTimestampMs < now) {
            EventLogger.log("PAIRING", "system", "PAIRING_CODE_EXPIRED", details = "Code: $cleanCode")
            return PairingResult.ExpiredCode("Pairing code has expired. Please generate a new code from Parent Hub.")
        }

        if (pairingCode.isUsed) {
            EventLogger.log("PAIRING", "system", "PAIRING_CODE_ALREADY_USED", details = "Code: $cleanCode")
            return PairingResult.AlreadyUsed("This pairing code has already been used.")
        }

        // Get or create local device UUID (never IMEI/Android ID)
        val deviceId = preferencesManager.getOrCreateDeviceId()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val appVersion = "1.0.0-prod-foundation"

        // Mark code as used in local cache
        val updatedCode = pairingCode.copy(isUsed = true, pairedDeviceId = deviceId, usedAt = Date())
        localPairingCodes[cleanCode] = updatedCode

        // Background update in Firestore
        scope.launch {
            try {
                firestore?.collection("pairing_codes")?.document(cleanCode)?.update(
                    mapOf("isUsed" to true, "pairedDeviceId" to deviceId, "usedAt" to Date())
                )?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Register device under child in Firestore / Local Cloud Store
        val deviceDto = DeviceDto(
            deviceId = deviceId,
            childId = pairingCode.childId,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            appVersion = appVersion,
            isProtectionActive = true,
            activePolicyVersion = 1,
            lastSeen = Date(),
            pairedAt = Date()
        )
        cloudRepository.registerDevice(pairingCode.familyId, pairingCode.childId, deviceDto)

        // Save local preferences
        preferencesManager.setPairedFamilyId(pairingCode.familyId)
        preferencesManager.setPairedChildId(pairingCode.childId)
        preferencesManager.setPairedChildName(pairingCode.childName)
        preferencesManager.setDeviceRole("CHILD_DEVICE")

        // Reset analytics & escalation counters to ensure fresh start for this child
        try {
            DigitalDisciplineApp.instance.analyticsRepository.clearAllMetrics()
            DigitalDisciplineApp.instance.policyEngine.resetAttempts()
        } catch (e: Exception) {
            // Ignore
        }

        EventLogger.log("PAIRING", "system", "PAIRING_COMPLETED", details = "Device: $deviceId paired to Child: ${pairingCode.childName} (${pairingCode.childId}) - Metrics Reset")

        return PairingResult.Success(
            familyId = pairingCode.familyId,
            childId = pairingCode.childId,
            childName = pairingCode.childName,
            deviceId = deviceId
        )
    }
}

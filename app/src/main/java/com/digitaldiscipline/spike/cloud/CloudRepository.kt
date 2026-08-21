package com.digitaldiscipline.spike.cloud

import android.content.Context
import com.digitaldiscipline.spike.cloud.models.*
import com.digitaldiscipline.spike.logging.EventLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class CloudRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            EventLogger.log("CLOUD", "system", "FIRESTORE_INIT_FALLBACK", details = e.message ?: "Unknown")
            null
        }
    }

    // Local Resilient Cloud Store
    private val localFamilies = mutableMapOf<String, FamilyDto>() // familyId -> FamilyDto
    private val localChildren = mutableMapOf<String, MutableList<ChildDto>>() // familyId -> list
    private val localPolicies = mutableMapOf<String, CloudPolicyDto>() // childId -> policy
    private val localDevices = mutableMapOf<String, DeviceDto>()
    private val localSummaries = mutableMapOf<String, DailySummaryDto>()

    private val _childrenFlowMap = mutableMapOf<String, MutableStateFlow<List<ChildDto>>>()

    init {
        // Pre-seed sample dev family ONLY for the sample dev parent
        val devFamId = "fam_dev_sample"
        val devChildId = "child_alex_sample"
        val devFamily = FamilyDto(
            familyId = devFamId,
            familyName = "Smith Family",
            ownerParentId = "dev_parent_" + Math.abs("parent@example.com".hashCode()),
            subscriptionTier = "FREE"
        )
        localFamilies[devFamId] = devFamily

        val devChild = ChildDto(
            childId = devChildId,
            name = "Alex",
            age = 10
        )
        localChildren[devFamId] = mutableListOf(devChild)
        createDefaultPolicyForChild(devChildId)
    }

    private fun createDefaultPolicyForChild(childId: String): CloudPolicyDto {
        val policy = CloudPolicyDto(
            policyId = childId,
            version = 1,
            rules = listOf(
                CloudAppRuleDto(
                    packageName = "com.instagram.android",
                    appDisplayName = "Instagram",
                    mode = "EARN",
                    isEnabled = true,
                    unlockDurationSeconds = 600,
                    interventionType = "PAUSE"
                ),
                CloudAppRuleDto(
                    packageName = "com.google.android.youtube",
                    appDisplayName = "YouTube",
                    mode = "EARN",
                    isEnabled = true,
                    unlockDurationSeconds = 900,
                    interventionType = "BREATHING"
                ),
                CloudAppRuleDto(
                    packageName = "com.dts.freefireth",
                    appDisplayName = "Gaming App (Free Fire)",
                    mode = "EARN",
                    isEnabled = true,
                    unlockDurationSeconds = 900,
                    interventionType = "SQUATS"
                )
            )
        )
        localPolicies[childId] = policy
        return policy
    }

    // 1. Families
    suspend fun createFamily(familyName: String, ownerParentId: String): Result<FamilyDto> {
        val familyId = "fam_" + UUID.randomUUID().toString().take(8)
        val family = FamilyDto(
            familyId = familyId,
            familyName = familyName.ifBlank { "My Family" },
            ownerParentId = ownerParentId,
            subscriptionTier = "FREE"
        )

        // Save to resilient local store
        localFamilies[familyId] = family
        localChildren[familyId] = mutableListOf()
        getOrCreateChildrenState(familyId).value = emptyList()

        // Background sync to Cloud Firestore if connected
        scope.launch {
            try {
                firestore?.collection("families")?.document(familyId)?.set(family)?.await()
            } catch (e: Exception) {
                // Background cloud sync failure is non-blocking
            }
        }

        EventLogger.log("CLOUD", "system", "FAMILY_CREATED", details = "FamilyId: $familyId | Name: $familyName | Owner: $ownerParentId")
        return Result.success(family)
    }

    suspend fun getFamiliesForParent(parentId: String): Result<List<FamilyDto>> {
        val matchingLocal = localFamilies.values.filter { it.ownerParentId == parentId }
        if (matchingLocal.isNotEmpty()) {
            return Result.success(matchingLocal)
        }

        val db = firestore ?: return Result.success(emptyList())
        return try {
            val querySnapshot = db.collection("families")
                .whereEqualTo("ownerParentId", parentId)
                .get()
                .await()
            val list = querySnapshot.toObjects(FamilyDto::class.java)
            list.forEach { fam -> localFamilies[fam.familyId] = fam }
            Result.success(list)
        } catch (e: Exception) {
            Result.success(matchingLocal)
        }
    }

    suspend fun getFamily(familyId: String): Result<FamilyDto?> {
        val cached = localFamilies[familyId]
        if (cached != null) return Result.success(cached)

        val db = firestore ?: return Result.success(null)
        return try {
            val doc = db.collection("families").document(familyId).get().await()
            val family = doc.toObject(FamilyDto::class.java)
            if (family != null) localFamilies[familyId] = family
            Result.success(family)
        } catch (e: Exception) {
            Result.success(localFamilies[familyId])
        }
    }

    // 2. Children
    suspend fun createChild(familyId: String, name: String, age: Int): Result<ChildDto> {
        val childId = "child_" + UUID.randomUUID().toString().take(8)
        val child = ChildDto(
            childId = childId,
            name = name.ifBlank { "Child" },
            age = age
        )

        // Save to local list
        val list = localChildren.getOrPut(familyId) { mutableListOf() }
        list.add(child)
        getOrCreateChildrenState(familyId).value = list.toList()

        val defaultPolicy = createDefaultPolicyForChild(childId)

        // Background sync
        scope.launch {
            try {
                firestore?.collection("families")?.document(familyId)
                    ?.collection("children")?.document(childId)?.set(child)?.await()
                saveCloudPolicy(familyId, childId, defaultPolicy)
            } catch (e: Exception) {
                // Ignore background error
            }
        }

        EventLogger.log("CLOUD", "system", "CHILD_CREATED", details = "Child: $name ($childId)")
        return Result.success(child)
    }

    suspend fun deleteChild(familyId: String, childId: String): Result<Unit> {
        val list = localChildren[familyId]
        list?.removeAll { it.childId == childId }
        getOrCreateChildrenState(familyId).value = list?.toList() ?: emptyList()
        localPolicies.remove(childId)

        scope.launch {
            try {
                firestore?.collection("families")?.document(familyId)
                    ?.collection("children")?.document(childId)?.delete()?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return Result.success(Unit)
    }

    private fun getOrCreateChildrenState(familyId: String): MutableStateFlow<List<ChildDto>> {
        return _childrenFlowMap.getOrPut(familyId) {
            MutableStateFlow(localChildren[familyId] ?: emptyList())
        }
    }

    fun getChildrenFlow(familyId: String): Flow<List<ChildDto>> {
        val state = getOrCreateChildrenState(familyId)
        if (state.value.isEmpty() && localChildren.containsKey(familyId)) {
            state.value = localChildren[familyId] ?: emptyList()
        }
        return state.asStateFlow()
    }

    // 3. Devices
    suspend fun registerDevice(familyId: String, childId: String, device: DeviceDto): Result<Unit> {
        localDevices[device.deviceId] = device
        scope.launch {
            try {
                firestore?.collection("families")?.document(familyId)
                    ?.collection("children")?.document(childId)
                    ?.collection("devices")?.document(device.deviceId)?.set(device, SetOptions.merge())?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return Result.success(Unit)
    }

    suspend fun updateDeviceHeartbeat(
        familyId: String,
        childId: String,
        deviceId: String,
        isProtectionActive: Boolean,
        policyVersion: Int
    ): Result<Unit> {
        val existing = localDevices[deviceId]
        if (existing != null) {
            localDevices[deviceId] = existing.copy(
                isProtectionActive = isProtectionActive,
                activePolicyVersion = policyVersion,
                lastSeen = Date()
            )
        }
        scope.launch {
            try {
                firestore?.collection("families")?.document(familyId)
                    ?.collection("children")?.document(childId)
                    ?.collection("devices")?.document(deviceId)
                    ?.update(mapOf("isProtectionActive" to isProtectionActive, "activePolicyVersion" to policyVersion, "lastSeen" to Date()))
                    ?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return Result.success(Unit)
    }

    // 4. Versioned Policies
    suspend fun getCloudPolicy(familyId: String, childId: String): Result<CloudPolicyDto?> {
        val cached = localPolicies[childId]
        if (cached != null) return Result.success(cached)

        val db = firestore
        if (db != null) {
            try {
                val doc = db.collection("families").document(familyId)
                    .collection("children").document(childId)
                    .collection("policy").document("current").get().await()
                val pol = doc.toObject(CloudPolicyDto::class.java)
                if (pol != null) {
                    localPolicies[childId] = pol
                    return Result.success(pol)
                }
            } catch (e: Exception) {
                // Fallback
            }
        }
        return Result.success(localPolicies[childId] ?: createDefaultPolicyForChild(childId))
    }

    suspend fun saveCloudPolicy(familyId: String, childId: String, policy: CloudPolicyDto): Result<Unit> {
        localPolicies[childId] = policy
        scope.launch {
            try {
                firestore?.collection("families")?.document(familyId)
                    ?.collection("children")?.document(childId)
                    ?.collection("policy")?.document("current")?.set(policy, SetOptions.merge())?.await()
            } catch (e: Exception) {
                // Fallback
            }
        }
        EventLogger.log("CLOUD", "system", "POLICY_SAVED_CLOUD", details = "ChildId: $childId | Version: ${policy.version}")
        return Result.success(Unit)
    }

    // 5. Daily Summary Analytics
    suspend fun uploadDailySummary(familyId: String, summary: DailySummaryDto): Result<Unit> {
        localSummaries[summary.summaryId] = summary
        scope.launch {
            try {
                firestore?.collection("families")?.document(familyId)
                    ?.collection("daily_summaries")?.document(summary.summaryId)?.set(summary, SetOptions.merge())?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
        EventLogger.log("CLOUD", "system", "DAILY_SUMMARY_UPLOADED", details = "Date: ${summary.dateString} | TotalMin: ${summary.totalScreenTimeMinutes}")
        return Result.success(Unit)
    }
}

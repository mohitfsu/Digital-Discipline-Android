package com.digitaldiscipline.spike.security

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.digitaldiscipline.spike.logging.EventLogger
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed class PinVerificationResult {
    object Success : PinVerificationResult()
    data class IncorrectPin(val attemptsRemaining: Int) : PinVerificationResult()
    data class LockedOut(val remainingLockoutSeconds: Long) : PinVerificationResult()
    object PinNotSet : PinVerificationResult()
}

class ParentPinManager(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "digital_discipline_secure_pin_store"
        private const val KEY_PIN_HASH = "parent_pin_hash"
        private const val KEY_PIN_SALT = "parent_pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL_ELAPSED = "lockout_until_elapsed"

        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 300_000L // 5 minutes
        private const val PBKDF2_ITERATIONS = 12_000
        private const val HASH_KEY_LENGTH = 256
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        // Set default initial PIN to 1234 if not configured
        if (!isPinSet()) {
            setPin("1234")
        }
    }

    fun isPinSet(): Boolean {
        return securePrefs.contains(KEY_PIN_HASH) && securePrefs.contains(KEY_PIN_SALT)
    }

    fun setPin(newPin: String): Boolean {
        if (newPin.length < 4) return false
        val salt = generateSalt()
        val hash = hashPin(newPin, salt)

        securePrefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L)
            .apply()

        EventLogger.log(
            source = "SECURITY",
            packageName = context.packageName,
            eventType = "PARENT_PIN_CONFIGURED",
            details = "Salt + PBKDF2 Hash generated & stored securely"
        )
        return true
    }

    fun verifyPin(enteredPin: String): PinVerificationResult {
        if (!isPinSet()) {
            setPin("1234")
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        val lockoutUntil = securePrefs.getLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L)

        if (nowElapsed < lockoutUntil) {
            val remainingSec = ((lockoutUntil - nowElapsed) / 1000L).coerceAtLeast(1L)
            EventLogger.log(
                source = "SECURITY",
                packageName = context.packageName,
                eventType = "PIN_VERIFICATION_REJECTED",
                details = "Device in lockout state: ${remainingSec}s remaining"
            )
            return PinVerificationResult.LockedOut(remainingSec)
        }

        val saltBase64 = securePrefs.getString(KEY_PIN_SALT, null) ?: return PinVerificationResult.PinNotSet
        val storedHashBase64 = securePrefs.getString(KEY_PIN_HASH, null) ?: return PinVerificationResult.PinNotSet

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val computedHash = hashPin(enteredPin, salt)
        val computedHashBase64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)

        return if (storedHashBase64 == computedHashBase64) {
            // Reset failure counter on success
            securePrefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L)
                .apply()

            EventLogger.log(
                source = "SECURITY",
                packageName = context.packageName,
                eventType = "PARENT_PIN_VERIFIED_SUCCESS"
            )
            PinVerificationResult.Success
        } else {
            val failedCount = securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            if (failedCount >= MAX_FAILED_ATTEMPTS) {
                val newLockoutUntil = nowElapsed + LOCKOUT_DURATION_MS
                securePrefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCKOUT_UNTIL_ELAPSED, newLockoutUntil)
                    .apply()

                EventLogger.log(
                    source = "SECURITY",
                    packageName = context.packageName,
                    eventType = "PIN_LOCKOUT_TRIGGERED",
                    details = "Failed attempts reached limit. Locked for 5 mins."
                )
                PinVerificationResult.LockedOut(LOCKOUT_DURATION_MS / 1000L)
            } else {
                securePrefs.edit().putInt(KEY_FAILED_ATTEMPTS, failedCount).apply()
                val attemptsLeft = MAX_FAILED_ATTEMPTS - failedCount
                EventLogger.log(
                    source = "SECURITY",
                    packageName = context.packageName,
                    eventType = "PIN_VERIFICATION_FAILED",
                    details = "Attempts left: $attemptsLeft"
                )
                PinVerificationResult.IncorrectPin(attemptsLeft)
            }
        }
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        return try {
            val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } catch (e: Exception) {
            throw RuntimeException("PBKDF2 hashing failed", e)
        }
    }
}

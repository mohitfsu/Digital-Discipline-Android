package com.digitaldiscipline.spike.logging

import android.content.Context
import android.util.Log
import com.digitaldiscipline.spike.data.local.DigitalDisciplineDatabase
import com.digitaldiscipline.spike.data.local.entities.DiagnosticEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * High-performance, non-blocking local diagnostic event logger.
 * Records internal state transitions, sync lifecycles, and permission health
 * strictly on-device in Room for debugging and observability.
 */
object DiagnosticLogger {

    private const val TAG = "DiagnosticLogger"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var database: DigitalDisciplineDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            database = DigitalDisciplineDatabase.getInstance(context.applicationContext)
        }
    }

    fun log(
        eventType: String,
        packageName: String? = null,
        policyVersion: Int = 1,
        details: String? = null
    ) {
        Log.i(TAG, "[$eventType] pkg=$packageName v=$policyVersion: $details")
        scope.launch {
            try {
                val db = database ?: return@launch
                val dao = db.diagnosticEventDao()
                dao.insertEvent(
                    DiagnosticEventEntity(
                        timestampMs = System.currentTimeMillis(),
                        eventType = eventType,
                        packageName = packageName,
                        policyVersion = policyVersion,
                        details = details
                    )
                )
                // Periodically prune old records to avoid unbounded growth
                dao.pruneOldEvents(keepCount = 200)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist diagnostic event: ${e.message}")
            }
        }
    }

    fun logServiceStarted(serviceName: String) =
        log(eventType = "SERVICE_STARTED", details = "Service initialized: $serviceName")

    fun logServiceStopped(serviceName: String) =
        log(eventType = "SERVICE_STOPPED", details = "Service destroyed: $serviceName")

    fun logOverlayPermissionMissing() =
        log(eventType = "OVERLAY_PERMISSION_MISSING", details = "SYSTEM_ALERT_WINDOW permission revoked or missing")

    fun logAccessibilityDisabled() =
        log(eventType = "ACCESSIBILITY_DISABLED", details = "Accessibility Service unbound or disabled in settings")

    fun logPolicyLoaded(version: Int, ruleCount: Int) =
        log(eventType = "POLICY_LOADED", policyVersion = version, details = "Loaded $ruleCount active rules from Room")

    fun logPolicySyncStarted(childId: String) =
        log(eventType = "POLICY_SYNC_STARTED", details = "Sync initiated for child: $childId")

    fun logPolicySyncSucceeded(newVersion: Int) =
        log(eventType = "POLICY_SYNC_SUCCEEDED", policyVersion = newVersion, details = "Policy successfully synchronized to version $newVersion")

    fun logPolicySyncFailed(reason: String) =
        log(eventType = "POLICY_SYNC_FAILED", details = "Sync failed: $reason")

    fun logPolicyVersionChanged(oldVersion: Int, newVersion: Int) =
        log(eventType = "POLICY_VERSION_CHANGED", policyVersion = newVersion, details = "Policy version updated from v$oldVersion to v$newVersion")

    fun logUnlockCreated(packageName: String, durationSec: Int) =
        log(eventType = "UNLOCK_CREATED", packageName = packageName, details = "Temporary unlock granted for ${durationSec}s")

    fun logUnlockExpired(packageName: String, reason: String) =
        log(eventType = "UNLOCK_EXPIRED", packageName = packageName, details = "Temporary unlock revoked: $reason")

    fun logBootCompleted() =
        log(eventType = "BOOT_COMPLETED", details = "Device restart detected; verifying Room DB and protection readiness")

    fun getRecentEventsFlow(context: Context): Flow<List<DiagnosticEventEntity>> {
        initialize(context)
        return database!!.diagnosticEventDao().getRecentEventsFlow(limit = 100)
    }

    suspend fun getRecentEvents(context: Context, limit: Int = 100): List<DiagnosticEventEntity> {
        initialize(context)
        return database!!.diagnosticEventDao().getRecentEvents(limit)
    }

    suspend fun clearLogs(context: Context) {
        initialize(context)
        database!!.diagnosticEventDao().clearAllEvents()
    }
}

package com.digitaldiscipline.spike.sync

import android.content.Context
import androidx.work.*
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.ScheduleEntity
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context) {

    private val workManager: WorkManager? by lazy {
        try {
            WorkManager.getInstance(context)
        } catch (_: Throwable) {
            null
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val PERIODIC_POLICY_SYNC_TAG = "periodic_policy_sync"
        const val DAILY_ANALYTICS_UPLOAD_TAG = "daily_analytics_upload"
        const val IMMEDIATE_SYNC_TAG = "immediate_policy_sync"
    }

    /**
     * Initializes background sync schedules. Respects battery and network constraints.
     */
    fun initializeSchedules() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Periodic Policy Sync: Runs every 2 hours when connected to network
            val policySyncRequest = PeriodicWorkRequestBuilder<PolicySyncWorker>(
                repeatInterval = 2,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(PERIODIC_POLICY_SYNC_TAG)
                .build()

            workManager?.enqueueUniquePeriodicWork(
                PERIODIC_POLICY_SYNC_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                policySyncRequest
            )

            // Daily Analytics Upload: Runs once every 24 hours
            val analyticsUploadRequest = PeriodicWorkRequestBuilder<DailyAnalyticsUploadWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(DAILY_ANALYTICS_UPLOAD_TAG)
                .build()

            workManager?.enqueueUniquePeriodicWork(
                DAILY_ANALYTICS_UPLOAD_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                analyticsUploadRequest
            )

            EventLogger.log("SYNC", "system", "WORKMANAGER_SCHEDULED", details = "Policy: 2h interval | Analytics: 24h interval")
        } catch (t: Throwable) {
            EventLogger.log("SYNC", "system", "WORKMANAGER_SCHEDULE_ERROR", details = t.message ?: "Failed to schedule work")
        }
    }

    /**
     * Triggers an immediate policy sync (both synchronously via Coroutine and asynchronously via WorkManager).
     */
    fun triggerImmediateSync() {
        // 1. Direct Instant Coroutine Sync (0ms delay)
        scope.launch {
            try {
                val app = DigitalDisciplineApp.instance
                val prefs = app.preferencesManager
                val familyId = prefs.pairedFamilyIdFlow.first()
                val childId = prefs.pairedChildIdFlow.first()
                val deviceId = prefs.getOrCreateDeviceId()

                if (!familyId.isNullOrBlank() && !childId.isNullOrBlank()) {
                    com.digitaldiscipline.spike.logging.DiagnosticLogger.logPolicySyncStarted(childId)
                    val policyResult = app.cloudRepository.getCloudPolicy(familyId, childId)
                    val cloudPolicy = policyResult.getOrNull()

                    if (cloudPolicy != null) {
                        val currentVersion = prefs.policyVersionFlow.first()
                        val newRules = cloudPolicy.rules.map { ruleDto ->
                            AppRuleEntity(
                                packageName = ruleDto.packageName,
                                appDisplayName = ruleDto.appDisplayName,
                                mode = try { RuleMode.valueOf(ruleDto.mode) } catch (e: Exception) { RuleMode.EARN },
                                isEnabled = ruleDto.isEnabled,
                                dailyLimitMinutes = ruleDto.dailyLimitMinutes,
                                unlockDurationSeconds = ruleDto.unlockDurationSeconds,
                                interventionType = ruleDto.interventionType,
                                pauseDurationSeconds = if (ruleDto.pauseDurationSeconds > 0) ruleDto.pauseDurationSeconds else cloudPolicy.pauseDurationSeconds,
                                breathingDurationSeconds = if (ruleDto.breathingDurationSeconds > 0) ruleDto.breathingDurationSeconds else cloudPolicy.breathingDurationSeconds,
                                squatsTargetCount = if (ruleDto.squatsTargetCount > 0) ruleDto.squatsTargetCount else cloudPolicy.squatsTargetCount,
                                updatedAt = System.currentTimeMillis()
                            )
                        }

                        val newSchedules = cloudPolicy.schedules.map { schedDto ->
                            ScheduleEntity(
                                label = if (schedDto.label.isNotBlank()) schedDto.label else "Schedule",
                                packageName = schedDto.packageName,
                                dayOfWeek = schedDto.dayOfWeek,
                                startHour = schedDto.startHour,
                                startMinute = schedDto.startMinute,
                                endHour = schedDto.endHour,
                                endMinute = schedDto.endMinute,
                                isBlocked = schedDto.isBlocked,
                                restrictionMode = if (schedDto.restrictionMode.isNotBlank()) schedDto.restrictionMode else "BLOCK",
                                isEnabled = schedDto.isEnabled,
                                daysOfWeekCsv = schedDto.daysOfWeekCsv
                            )
                        }

                        // Atomic Room Transaction
                        app.policyRepository.transactionalUpdatePolicy(newRules, newSchedules)
                        prefs.setPolicyVersion(cloudPolicy.version)
                        prefs.setLastPolicySync(System.currentTimeMillis())

                        com.digitaldiscipline.spike.logging.DiagnosticLogger.logPolicySyncSucceeded(cloudPolicy.version)
                        if (cloudPolicy.version != currentVersion) {
                            com.digitaldiscipline.spike.logging.DiagnosticLogger.logPolicyVersionChanged(currentVersion, cloudPolicy.version)
                        }

                        EventLogger.log(
                            source = "SYNC",
                            packageName = "system",
                            eventType = "IMMEDIATE_SYNC_COMPLETED",
                            details = "Updated to Policy v${cloudPolicy.version} (${newRules.size} rules)"
                        )
                    }
                }
            } catch (e: Exception) {
                com.digitaldiscipline.spike.logging.DiagnosticLogger.logPolicySyncFailed(e.message ?: "Network or Firebase Error")
                EventLogger.log("SYNC", "system", "IMMEDIATE_SYNC_ERROR", details = e.message ?: "Unknown")
            }
        }

        // 2. Also enqueue WorkManager task
        try {
            val immediateSync = OneTimeWorkRequestBuilder<PolicySyncWorker>()
                .addTag(IMMEDIATE_SYNC_TAG)
                .build()

            workManager?.enqueueUniqueWork(
                IMMEDIATE_SYNC_TAG,
                ExistingWorkPolicy.REPLACE,
                immediateSync
            )

            EventLogger.log("SYNC", "system", "IMMEDIATE_SYNC_REQUESTED")
        } catch (_: Throwable) {}
    }
}

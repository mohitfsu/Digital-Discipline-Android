package com.digitaldiscipline.spike.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.ScheduleEntity
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.flow.first

class PolicySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DigitalDisciplineApp ?: return Result.failure()
        val prefs = app.preferencesManager
        val cloudRepo = app.cloudRepository
        val policyRepo = app.policyRepository

        val familyId = prefs.pairedFamilyIdFlow.first()
        val childId = prefs.pairedChildIdFlow.first()
        val deviceId = prefs.getOrCreateDeviceId()
        val localVersion = prefs.policyVersionFlow.first()

        if (familyId.isNullOrBlank() || childId.isNullOrBlank()) {
            EventLogger.log("SYNC", "system", "POLICY_SYNC_SKIPPED", details = "Device not paired to a child")
            return Result.success()
        }

        return try {
            EventLogger.log("SYNC", "system", "POLICY_SYNC_STARTED", details = "FamilyId: $familyId | ChildId: $childId | LocalVersion: $localVersion")

            val policyResult = cloudRepo.getCloudPolicy(familyId, childId)
            if (policyResult.isFailure) {
                EventLogger.log("SYNC", "system", "POLICY_SYNC_NETWORK_RETRY", details = policyResult.exceptionOrNull()?.message ?: "Unknown")
                return Result.retry()
            }

            val cloudPolicy = policyResult.getOrNull()
            if (cloudPolicy != null && cloudPolicy.version >= localVersion) {
                // Map Cloud App Rules -> Room AppRuleEntities
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

                // Map Cloud Schedules -> Room ScheduleEntities
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

                // Transactional Atomic Persistence in Room
                policyRepo.transactionalUpdatePolicy(newRules, newSchedules)
                prefs.setPolicyVersion(cloudPolicy.version)
                prefs.setLastPolicySync(System.currentTimeMillis())

                com.digitaldiscipline.spike.logging.DiagnosticLogger.logPolicySyncSucceeded(cloudPolicy.version)
                if (cloudPolicy.version != localVersion) {
                    com.digitaldiscipline.spike.logging.DiagnosticLogger.logPolicyVersionChanged(localVersion, cloudPolicy.version)
                }

                // Update Device Heartbeat in Cloud
                val isProtectionActive = app.tamperDetector.isAccessibilityActive() && app.policyEngine.overlayManager.canDrawOverlays()
                cloudRepo.updateDeviceHeartbeat(
                    familyId = familyId,
                    childId = childId,
                    deviceId = deviceId,
                    isProtectionActive = isProtectionActive,
                    policyVersion = cloudPolicy.version
                )

                EventLogger.log(
                    source = "SYNC",
                    packageName = "system",
                    eventType = "POLICY_SYNC_SUCCESS",
                    details = "Activated Cloud Policy Version ${cloudPolicy.version} (${newRules.size} rules)"
                )
            } else {
                EventLogger.log("SYNC", "system", "POLICY_SYNC_UP_TO_DATE", details = "Version $localVersion is current")
            }

            Result.success()
        } catch (e: Exception) {
            com.digitaldiscipline.spike.logging.DiagnosticLogger.logPolicySyncFailed(e.message ?: "Unknown WorkManager sync error")
            EventLogger.log("SYNC", "system", "POLICY_SYNC_EXCEPTION", details = e.message ?: "Unknown error")
            // Retain local known-good policy and retry when network is stable
            Result.retry()
        }
    }
}

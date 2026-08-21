package com.digitaldiscipline.spike.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.cloud.models.DailyAppUsageDto
import com.digitaldiscipline.spike.cloud.models.DailySummaryDto
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyAnalyticsUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DigitalDisciplineApp ?: return Result.failure()
        val prefs = app.preferencesManager
        val cloudRepo = app.cloudRepository
        val analyticsRepo = app.analyticsRepository

        val familyId = prefs.pairedFamilyIdFlow.first()
        val childId = prefs.pairedChildIdFlow.first()
        val deviceId = prefs.getOrCreateDeviceId()

        if (familyId.isNullOrBlank() || childId.isNullOrBlank()) {
            return Result.success() // Not paired
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        return try {
            val usageList = analyticsRepo.getTodayUsageFlow().first()
            if (usageList.isEmpty()) {
                EventLogger.log("SYNC", "system", "ANALYTICS_UPLOAD_SKIPPED", details = "No recorded usage for $todayDate")
                return Result.success()
            }

            val totalScreenTimeMin = usageList.sumOf { (it.totalForegroundSeconds / 60) }.toInt()
            val totalBlocks = usageList.sumOf { it.blockCount }
            val totalUnlocks = usageList.sumOf { it.unlockCount }
            val totalAttempts = usageList.sumOf { it.attempts }
            val totalEarnedMinutes = usageList.sumOf { it.earnedMinutes }
            val totalPauses = usageList.sumOf { it.pauseCount }
            val totalBreathing = usageList.sumOf { it.breathingCount }
            val totalSquats = usageList.sumOf { it.squatsCount }
            val hir = analyticsRepo.calculateHabitInterruptionRate(todayDate)

            val topAppsDto = usageList.map { usage ->
                DailyAppUsageDto(
                    packageName = usage.packageName,
                    appDisplayName = usage.appDisplayName,
                    usageMinutes = (usage.totalForegroundSeconds / 60).toInt(),
                    openCount = usage.openCount,
                    blockCount = usage.blockCount,
                    unlockCount = usage.unlockCount,
                    attempts = usage.attempts,
                    earnedMinutes = usage.earnedMinutes,
                    habitInterruptionRate = usage.habitInterruptionRate
                )
            }

            val summaryDto = DailySummaryDto(
                summaryId = "${childId}_$todayDate",
                familyId = familyId,
                childId = childId,
                deviceId = deviceId,
                dateString = todayDate,
                totalScreenTimeMinutes = totalScreenTimeMin,
                totalInterventionsCompleted = totalUnlocks,
                totalBlocksTriggered = totalBlocks,
                totalAttempts = totalAttempts,
                totalEarnedMinutes = totalEarnedMinutes,
                habitInterruptionRate = hir,
                pauseCount = totalPauses,
                breathingCount = totalBreathing,
                squatsCount = totalSquats,
                topApps = topAppsDto
            )

            // Strictly 1 single document write to Cloud Firestore
            val uploadResult = cloudRepo.uploadDailySummary(familyId, summaryDto)
            if (uploadResult.isSuccess) {
                EventLogger.log(
                    source = "SYNC",
                    packageName = "system",
                    eventType = "DAILY_ROLLUP_UPLOAD_SUCCESS",
                    details = "Date: $todayDate | HIR: $hir% | Apps: ${topAppsDto.size} | TotalMin: $totalScreenTimeMin"
                )
                Result.success()
            } else {
                EventLogger.log("SYNC", "system", "ANALYTICS_UPLOAD_RETRY", details = uploadResult.exceptionOrNull()?.message ?: "Unknown")
                Result.retry()
            }
        } catch (e: Exception) {
            EventLogger.log("SYNC", "system", "ANALYTICS_UPLOAD_EXCEPTION", details = e.message ?: "Unknown")
            Result.retry()
        }
    }
}

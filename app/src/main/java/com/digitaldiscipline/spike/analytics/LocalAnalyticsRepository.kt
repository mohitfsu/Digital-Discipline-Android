package com.digitaldiscipline.spike.analytics

import com.digitaldiscipline.spike.data.local.dao.DailyUsageDao
import com.digitaldiscipline.spike.data.local.dao.InterventionEventDao
import com.digitaldiscipline.spike.data.local.entities.DailyUsageEntity
import com.digitaldiscipline.spike.data.local.entities.InterventionEventEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * High-performance, non-blocking local behavioral analytics repository.
 * Aggregates intervention attempts, habit interruption rates, and rapid reopens strictly on-device.
 *
 * PRIVACY GUARANTEE: Never collects screen contents, keystrokes, messages, browsing history, or personal data.
 */
class LocalAnalyticsRepository(
    private val dailyUsageDao: DailyUsageDao,
    private val interventionEventDao: InterventionEventDao
) {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayString: String
        get() = sdf.format(Date())

    fun getTodayUsageFlow(): Flow<List<DailyUsageEntity>> {
        return dailyUsageDao.getUsageForDateFlow(todayString)
    }

    fun getRecentInterventionEventsFlow(limit: Int = 100): Flow<List<InterventionEventEntity>> {
        return interventionEventDao.getRecentEventsFlow(limit)
    }

    suspend fun clearAllMetrics() {
        dailyUsageDao.clearAllDailyUsage()
        interventionEventDao.clearAllEvents()
    }

    suspend fun recordAppOpened(packageName: String, appDisplayName: String) {
        val now = System.currentTimeMillis()
        val date = todayString

        // Check for rapid reopen against previous completed intervention
        val lastCompleted = interventionEventDao.getLatestCompletedEventForPackage(packageName)
        var reopen1m = false
        var reopen5m = false
        var reopen15m = false

        if (lastCompleted != null && lastCompleted.unlockExpiredAt > 0) {
            val deltaMs = now - lastCompleted.unlockExpiredAt
            if (deltaMs in 0..60_000L) reopen1m = true
            if (deltaMs in 0..300_000L) reopen5m = true
            if (deltaMs in 0..900_000L) reopen15m = true

            if (reopen5m) {
                interventionEventDao.updateEvent(
                    lastCompleted.copy(
                        reopenWithin1Minute = reopen1m,
                        reopenWithin5Minutes = reopen5m,
                        reopenWithin15Minutes = reopen15m,
                        outcome = "RAPID_REOPEN"
                    )
                )
            }
        }

        val existing = dailyUsageDao.getUsageEntry(date, packageName)
        if (existing == null) {
            dailyUsageDao.insertOrUpdate(
                DailyUsageEntity(
                    dateString = date,
                    packageName = packageName,
                    appDisplayName = appDisplayName,
                    openCount = 1,
                    attempts = 1,
                    rapidReopens = if (reopen5m) 1 else 0
                )
            )
        } else {
            dailyUsageDao.incrementOpenCount(date, packageName)
        }
    }

    suspend fun recordAppBlocked(
        packageName: String,
        appDisplayName: String,
        interventionType: String = "PAUSE",
        durationSeconds: Int = 10
    ): String {
        val date = todayString
        val eventId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Log intervention attempt event
        val event = InterventionEventEntity(
            eventId = eventId,
            timestamp = now,
            packageName = packageName,
            appDisplayName = appDisplayName,
            interventionType = interventionType,
            status = "STARTED",
            outcome = "STARTED",
            durationSeconds = durationSeconds,
            hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        )
        interventionEventDao.logEvent(event)

        // 2. Increment block count on daily summary
        val existing = dailyUsageDao.getUsageEntry(date, packageName)
        if (existing == null) {
            dailyUsageDao.insertOrUpdate(
                DailyUsageEntity(
                    dateString = date,
                    packageName = packageName,
                    appDisplayName = appDisplayName,
                    blockCount = 1,
                    attempts = 1,
                    pauseCount = if (interventionType == "PAUSE") 1 else 0,
                    breathingCount = if (interventionType == "BREATHING") 1 else 0,
                    squatsCount = if (interventionType == "SQUATS") 1 else 0
                )
            )
        } else {
            dailyUsageDao.incrementBlockCount(date, packageName)
        }

        return eventId
    }

    suspend fun recordInterventionCompleted(
        packageName: String,
        appDisplayName: String,
        interventionType: String,
        durationSeconds: Int,
        earnedDurationSeconds: Int = 600,
        latencyMs: Long = 0L
    ) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Log completed intervention event
        val event = InterventionEventEntity(
            timestamp = now,
            packageName = packageName,
            appDisplayName = appDisplayName,
            interventionType = interventionType,
            status = "COMPLETED",
            outcome = "EARNED_ACCESS",
            durationSeconds = durationSeconds,
            earnedSeconds = earnedDurationSeconds,
            unlockStartedAt = now,
            unlockExpiredAt = now + (earnedDurationSeconds * 1000L),
            hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
            latencyMs = latencyMs
        )
        interventionEventDao.logEvent(event)

        // 2. Increment unlock & earned count on daily summary
        val date = todayString
        val existing = dailyUsageDao.getUsageEntry(date, packageName)
        if (existing == null) {
            dailyUsageDao.insertOrUpdate(
                DailyUsageEntity(
                    dateString = date,
                    packageName = packageName,
                    appDisplayName = appDisplayName,
                    unlockCount = 1,
                    completed = 1,
                    earnedAccess = 1,
                    earnedMinutes = earnedDurationSeconds / 60
                )
            )
        } else {
            val earnedMins = (earnedDurationSeconds / 60).coerceAtLeast(1)
            dailyUsageDao.incrementUnlockCount(date, packageName, earnedMinutes = earnedMins)
        }
    }

    suspend fun recordUsageSeconds(packageName: String, additionalSeconds: Long) {
        if (additionalSeconds <= 0) return
        val date = todayString
        val existing = dailyUsageDao.getUsageEntry(date, packageName)
        if (existing == null) {
            dailyUsageDao.insertOrUpdate(
                DailyUsageEntity(
                    dateString = date,
                    packageName = packageName,
                    appDisplayName = packageName,
                    totalForegroundSeconds = additionalSeconds
                )
            )
        } else {
            dailyUsageDao.incrementUsageSeconds(date, packageName, additionalSeconds)
        }
    }

    suspend fun recordInterventionExited(
        packageName: String,
        appDisplayName: String,
        interventionType: String
    ) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val event = InterventionEventEntity(
            timestamp = now,
            packageName = packageName,
            appDisplayName = appDisplayName,
            interventionType = interventionType,
            status = "EXITED",
            outcome = "EXITED",
            hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        )
        interventionEventDao.logEvent(event)
    }

    suspend fun recordParentOverride(
        packageName: String,
        appDisplayName: String
    ) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val event = InterventionEventEntity(
            timestamp = now,
            packageName = packageName,
            appDisplayName = appDisplayName,
            interventionType = "PARENT_OVERRIDE",
            status = "COMPLETED",
            outcome = "PARENT_OVERRIDE",
            hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        )
        interventionEventDao.logEvent(event)
    }

    /**
     * Calculates the deterministic Habit Interruption Rate (HIR) for a given date.
     * Formula: (Attempts without 5-minute reopen / Total Attempts) * 100
     */
    suspend fun calculateHabitInterruptionRate(dateString: String = todayString): Float {
        val startOfDay = getStartOfDayTimestamp(dateString)
        val events = interventionEventDao.getEventsSince(startOfDay)
        if (events.isEmpty()) return 100.0f

        val totalAttempts = events.size
        val uninterruptedAttempts = events.count { !it.reopenWithin5Minutes }

        return ((uninterruptedAttempts.toFloat() / totalAttempts.toFloat()) * 100.0f).coerceIn(0.0f, 100.0f)
    }

    /**
     * Calculates the effectiveness stats separately for each intervention type.
     */
    suspend fun getInterventionEffectiveness(sinceTimestamp: Long = System.currentTimeMillis() - 7 * 86400000L): Map<String, InterventionStats> {
        val events = interventionEventDao.getEventsSince(sinceTimestamp)
        val types = listOf("PAUSE", "BREATHING", "SQUATS", "PARENT_OVERRIDE")

        return types.associateWith { type ->
            val typeEvents = events.filter { it.interventionType.equals(type, ignoreCase = true) }
            val attempts = typeEvents.size
            val completed = typeEvents.count { it.status == "COMPLETED" }
            val exited = typeEvents.count { it.status == "EXITED" }
            val reopens5m = typeEvents.count { it.reopenWithin5Minutes }
            val hir = if (attempts > 0) ((attempts - reopens5m).toFloat() / attempts.toFloat()) * 100f else 100f

            InterventionStats(
                interventionType = type,
                attempts = attempts,
                completed = completed,
                exited = exited,
                fiveMinuteReopens = reopens5m,
                habitInterruptionRate = hir
            )
        }
    }

    /**
     * Computes the 24-hour attempt distribution.
     */
    suspend fun getHourlyDistribution(sinceTimestamp: Long = getStartOfDayTimestamp(todayString)): IntArray {
        val events = interventionEventDao.getEventsSince(sinceTimestamp)
        val distribution = IntArray(24)
        for (event in events) {
            val hour = event.hourOfDay.coerceIn(0, 23)
            distribution[hour]++
        }
        return distribution
    }

    private fun getStartOfDayTimestamp(dateString: String): Long {
        return try {
            val date = sdf.parse(dateString) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis() - 86400000L
        }
    }
}

data class InterventionStats(
    val interventionType: String,
    val attempts: Int,
    val completed: Int,
    val exited: Int,
    val fiveMinuteReopens: Int,
    val habitInterruptionRate: Float
)

package com.digitaldiscipline.spike.behaviour.intelligence

import com.digitaldiscipline.spike.behaviour.BehaviourInsightsEngine
import com.digitaldiscipline.spike.data.local.entities.*
import java.util.Calendar

data class TimePatternResult(
    val hasSufficientData: Boolean,
    val peakHour: Int? = null,
    val peakWindowStart: Int? = null,
    val peakWindowEnd: Int? = null,
    val weekdayAttempts: Int = 0,
    val weekendAttempts: Int = 0,
    val morningAttempts: Int = 0,   // 06:00 - 11:59
    val afternoonAttempts: Int = 0, // 12:00 - 17:59
    val eveningAttempts: Int = 0,   // 18:00 - 21:59
    val nightAttempts: Int = 0,     // 22:00 - 05:59
    val summaryMessage: String = ""
)

data class AppPatternItem(
    val packageName: String,
    val displayName: String,
    val attempts: Int,
    val completed: Int,
    val exits: Int,
    val reopens5m: Int,
    val habitInterruptionRate: Float,
    val earnedSeconds: Int
)

data class InterventionPatternItem(
    val type: String,
    val displayName: String,
    val attempts: Int,
    val completed: Int,
    val exits: Int,
    val reopens5m: Int,
    val habitInterruptionRate: Float,
    val earnedSeconds: Int
)

data class GoalPatternResult(
    val targetCompletionRate: Float,
    val consistencyScore: Float,
    val activeDaysCount: Int,
    val totalDaysCount: Int,
    val totalCompletedActions: Int
)

data class WalletPatternResult(
    val totalEarnedSeconds: Int,
    val totalConsumedSeconds: Int,
    val averageSessionDurationSeconds: Int,
    val rewardConsumptionRatio: Float,
    val hasRapidConsumptionPattern: Boolean
)

object BehaviourPatternEngine {

    const val MIN_SAMPLE_THRESHOLD = 10

    /**
     * Identifies deterministic temporal distraction patterns.
     */
    fun calculateTimePatterns(events: List<InterventionEventEntity>): TimePatternResult {
        if (events.size < MIN_SAMPLE_THRESHOLD) {
            return TimePatternResult(
                hasSufficientData = false,
                summaryMessage = "Gathering baseline data (${events.size}/$MIN_SAMPLE_THRESHOLD sessions)."
            )
        }

        var weekday = 0
        var weekend = 0
        var morning = 0
        var afternoon = 0
        var evening = 0
        var night = 0

        val hourCounts = IntArray(24)

        events.forEach { ev ->
            val hour = ev.hourOfDay.coerceIn(0, 23)
            hourCounts[hour]++

            // Day of week (Calendar: 1 = Sunday, 7 = Saturday)
            if (ev.dayOfWeek == Calendar.SATURDAY || ev.dayOfWeek == Calendar.SUNDAY) {
                weekend++
            } else {
                weekday++
            }

            when (hour) {
                in 6..11 -> morning++
                in 12..17 -> afternoon++
                in 18..21 -> evening++
                else -> night++
            }
        }

        // 2-hour window peak detection
        val windowBuckets = IntArray(12)
        events.forEach { ev ->
            val b = (ev.hourOfDay / 2).coerceIn(0, 11)
            windowBuckets[b]++
        }
        val peakBucket = windowBuckets.indices.maxByOrNull { windowBuckets[it] } ?: 10
        val peakStart = peakBucket * 2
        val peakEnd = (peakStart + 2) % 24

        val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 20

        val startStr = formatHour(peakStart)
        val endStr = formatHour(peakEnd)

        return TimePatternResult(
            hasSufficientData = true,
            peakHour = peakHour,
            peakWindowStart = peakStart,
            peakWindowEnd = peakEnd,
            weekdayAttempts = weekday,
            weekendAttempts = weekend,
            morningAttempts = morning,
            afternoonAttempts = afternoon,
            eveningAttempts = evening,
            nightAttempts = night,
            summaryMessage = "Your primary distraction window is between $startStr and $endStr."
        )
    }

    /**
     * Ranks monitored applications by behavioral impact.
     */
    fun calculateAppPatterns(events: List<InterventionEventEntity>): List<AppPatternItem> {
        val grouped = events.groupBy { it.packageName }
        return grouped.map { (pkg, list) ->
            val name = list.firstOrNull()?.appDisplayName?.ifBlank { pkg } ?: pkg
            val attempts = list.size
            val completed = list.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
            val exits = list.count { it.status == "EXITED" }
            val reopens = list.count { it.reopenWithin5Minutes }
            val hir = if (attempts > 0) ((attempts - reopens).toFloat() / attempts.toFloat()) * 100f else 100f
            val earned = list.filter { it.status == "COMPLETED" }.sumOf { it.earnedSeconds }

            AppPatternItem(
                packageName = pkg,
                displayName = name,
                attempts = attempts,
                completed = completed,
                exits = exits,
                reopens5m = reopens,
                habitInterruptionRate = hir,
                earnedSeconds = earned
            )
        }.sortedByDescending { it.attempts }
    }

    /**
     * Evaluates and ranks replacement interventions.
     */
    fun calculateInterventionPatterns(events: List<InterventionEventEntity>): List<InterventionPatternItem> {
        val grouped = events.filter { it.interventionType != "PARENT_OVERRIDE" }
            .groupBy { it.interventionType.uppercase() }

        return grouped.map { (type, list) ->
            val attempts = list.size
            val completed = list.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
            val exits = list.count { it.status == "EXITED" }
            val reopens = list.count { it.reopenWithin5Minutes }
            val hir = if (attempts > 0) ((attempts - reopens).toFloat() / attempts.toFloat()) * 100f else 100f
            val earned = list.filter { it.status == "COMPLETED" }.sumOf { it.earnedSeconds }
            val displayName = when (type) {
                "SQUATS" -> "Squats Challenge"
                "BREATHING", "BOX_BREATHING" -> "Box Breathing"
                "PAUSE", "MINDFUL_PAUSE" -> "Mindful Pause"
                "PUSHUPS" -> "Pushups"
                else -> type.lowercase().replaceFirstChar { it.uppercase() }
            }

            InterventionPatternItem(
                type = type,
                displayName = displayName,
                attempts = attempts,
                completed = completed,
                exits = exits,
                reopens5m = reopens,
                habitInterruptionRate = hir,
                earnedSeconds = earned
            )
        }.sortedByDescending { it.habitInterruptionRate }
    }

    /**
     * Calculates consistency and goal adherence patterns.
     */
    fun calculateGoalPatterns(
        goal: GoalEntity?,
        progressList: List<GoalProgressEntity>,
        days: Int = 7
    ): GoalPatternResult {
        val relevant = progressList.take(days)
        val activeDays = relevant.count { it.completedCount > 0 }
        val targetMetDays = relevant.count { it.completedCount >= (goal?.dailyTarget ?: 1) }
        val totalCompleted = relevant.sumOf { it.completedCount }

        val targetCompRate = if (days > 0) (targetMetDays.toFloat() / days.toFloat()) * 100f else 0f
        val consistency = if (days > 0) (activeDays.toFloat() / days.toFloat()) * 100f else 0f

        return GoalPatternResult(
            targetCompletionRate = targetCompRate,
            consistencyScore = consistency,
            activeDaysCount = activeDays,
            totalDaysCount = days,
            totalCompletedActions = totalCompleted
        )
    }

    /**
     * Analyzes wallet consumption and session patterns.
     */
    fun calculateWalletPatterns(
        transactions: List<WalletTransactionEntity>,
        sessions: List<WalletSessionEntity>
    ): WalletPatternResult {
        val earned = transactions.filter { it.type == WalletTransactionType.EARN.name }.sumOf { it.amountSeconds }
        val consumed = sessions.sumOf { it.consumedSeconds }
        val avgSec = if (sessions.isNotEmpty()) sessions.map { it.consumedSeconds }.average().toInt() else 0
        val ratio = if (earned > 0) (consumed.toFloat() / earned.toFloat()) else 0f

        val rapidConsumption = sessions.size >= 5 && ratio >= 0.85f

        return WalletPatternResult(
            totalEarnedSeconds = earned,
            totalConsumedSeconds = consumed,
            averageSessionDurationSeconds = avgSec,
            rewardConsumptionRatio = ratio,
            hasRapidConsumptionPattern = rapidConsumption
        )
    }

    private fun formatHour(hour: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val ampm = if (hour < 12) "AM" else "PM"
        return "$h $ampm"
    }
}

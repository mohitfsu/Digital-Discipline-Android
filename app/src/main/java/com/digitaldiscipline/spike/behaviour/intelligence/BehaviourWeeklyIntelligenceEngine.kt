package com.digitaldiscipline.spike.behaviour.intelligence

import com.digitaldiscipline.spike.behaviour.BehaviourInsightsEngine
import com.digitaldiscipline.spike.data.local.entities.*
import java.util.Calendar

data class WeeklyIntelligenceSummary(
    val momentumScore: Int,
    val momentumState: MomentumState,
    val goalIntegrityScore: Int,
    val goalIntegritySummary: String,
    val strongestDay: String,
    val biggestDistraction: String,
    val whatWorked: String,
    val vulnerableWindow: String,
    val biggestWin: String,
    val nextWeekFocus: String
)

object BehaviourWeeklyIntelligenceEngine {

    const val MIN_SAMPLE_THRESHOLD = 10

    /**
     * Generates a comprehensive, deterministic weekly behavioral intelligence report.
     */
    fun generateWeeklyIntelligence(
        goal: GoalEntity?,
        events: List<InterventionEventEntity>,
        progressList: List<GoalProgressEntity>,
        transactions: List<WalletTransactionEntity> = emptyList(),
        sessions: List<WalletSessionEntity> = emptyList(),
        dailyUsage: List<DailyUsageEntity> = emptyList()
    ): WeeklyIntelligenceSummary {
        val momentum = BehaviourMomentumEngine.calculateMomentumScore(events, goal, progressList, dailyUsage)
        val integrity = GoalIntegrityEngine.calculateGoalIntegrity(goal, progressList, events)
        val timePattern = BehaviourPatternEngine.calculateTimePatterns(events)
        val appPatterns = BehaviourPatternEngine.calculateAppPatterns(events)
        val bestIntervention = BehaviourInsightsEngine.calculateBestIntervention(events)

        val topAppItem = appPatterns.firstOrNull()
        val totalAttempts = events.size
        val topAppShare = if (totalAttempts > 0 && topAppItem != null) {
            ((topAppItem.attempts.toFloat() / totalAttempts.toFloat()) * 100f).toInt()
        } else {
            0
        }

        val biggestDistraction = if (topAppItem != null && totalAttempts >= MIN_SAMPLE_THRESHOLD) {
            "${topAppItem.displayName} accounted for $topAppShare% of monitored distraction attempts."
        } else {
            "Distraction app activity was evenly distributed."
        }

        // Strongest day detection
        val dayCounts = IntArray(8) // 1=Sun .. 7=Sat
        val dayCompletions = IntArray(8)
        events.forEach { ev ->
            val day = ev.dayOfWeek.coerceIn(1, 7)
            dayCounts[day]++
            if (ev.status == "COMPLETED" || ev.outcome == "EARNED_ACCESS") {
                dayCompletions[day]++
            }
        }
        val bestDayIndex = (1..7).maxByOrNull { if (dayCounts[it] > 0) (dayCompletions[it].toFloat() / dayCounts[it].toFloat()) else 0f } ?: 3
        val dayName = when (bestDayIndex) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Weekdays"
        }
        val strongestDay = "Your strongest discipline was on $dayName."

        val bestName = bestIntervention?.displayName ?: "Mindful Pause"
        val bestRate = bestIntervention?.interruptionRate?.toInt() ?: 75
        val topAppName = topAppItem?.displayName ?: "distraction"
        val whatWorked = "$bestName interrupted $bestRate% of your $topAppName reopen attempts."

        val vulnerableWindow = if (timePattern.hasSufficientData && timePattern.peakWindowStart != null) {
            "You tend to open $topAppName most often between ${formatHour(timePattern.peakWindowStart)} and ${formatHour(timePattern.peakWindowEnd ?: 22)}."
        } else {
            "Distractions were spread evenly across the day."
        }

        val interruptedCount = events.count { !it.reopenWithin5Minutes && (it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS") }
        val biggestWin = if (interruptedCount > 0) {
            "You successfully interrupted $interruptedCount habitual opens this week."
        } else {
            "You started building your personal discipline baseline."
        }

        val nextWeekFocus = if (timePattern.hasSufficientData && timePattern.peakWindowStart != null) {
            "Protect your ${formatHour(timePattern.peakWindowStart)}–${formatHour(timePattern.peakWindowEnd ?: 22)} window."
        } else {
            "Maintain consistency with your primary replacement habit."
        }

        return WeeklyIntelligenceSummary(
            momentumScore = momentum.score,
            momentumState = momentum.state,
            goalIntegrityScore = integrity.score,
            goalIntegritySummary = integrity.alignmentSummary,
            strongestDay = strongestDay,
            biggestDistraction = biggestDistraction,
            whatWorked = whatWorked,
            vulnerableWindow = vulnerableWindow,
            biggestWin = biggestWin,
            nextWeekFocus = nextWeekFocus
        )
    }

    private fun formatHour(hour: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val ampm = if (hour < 12) "AM" else "PM"
        return "$h $ampm"
    }
}

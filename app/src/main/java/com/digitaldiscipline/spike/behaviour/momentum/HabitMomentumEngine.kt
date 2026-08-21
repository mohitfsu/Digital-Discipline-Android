package com.digitaldiscipline.spike.behaviour.momentum

import com.digitaldiscipline.spike.data.local.entities.GoalEntity
import com.digitaldiscipline.spike.data.local.entities.GoalProgressEntity
import com.digitaldiscipline.spike.data.local.entities.InterventionEventEntity
import com.digitaldiscipline.spike.data.local.entities.WalletTransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Phase 4E-3 — Habit Momentum & 7-Day Formation Engine
 *
 * Deterministic, offline-first calculation engine for rolling 7-day habit momentum,
 * recovery detection, and milestone achievement without gamified streak anxiety.
 */
object HabitMomentumEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)

    /**
     * Evaluates the complete 7-day rolling window from local telemetry.
     */
    fun evaluate7DayWindow(
        events: List<InterventionEventEntity>,
        goal: GoalEntity?,
        progressList: List<GoalProgressEntity>,
        walletTransactions: List<WalletTransactionEntity> = emptyList(),
        firstWinCompleted: Boolean = false,
        referenceDate: Date = Date()
    ): HabitMomentumSnapshot {
        val calendar = Calendar.getInstance().apply { time = referenceDate }
        val todayStr = dateFormat.format(calendar.time)

        // Build 7-day list starting 6 days ago up to today
        val rawDays = mutableListOf<HabitDay>()
        var previousDayWasActive = false
        var consecutiveMissedDays = 0
        var totalRecoveries = 0

        // Progress map by date
        val progressByDate = progressList.associateBy { it.dateString }

        // Events grouped by date
        val eventsByDate = events.groupBy { event ->
            dateFormat.format(Date(event.timestamp))
        }

        // Transactions grouped by date
        val transactionsByDate = walletTransactions.groupBy { tx ->
            dateFormat.format(Date(tx.timestampWallClock))
        }

        val allActivityDates = (events.map { dateFormat.format(Date(it.timestamp)) } +
                progressList.map { it.dateString } +
                walletTransactions.map { dateFormat.format(Date(it.timestampWallClock)) }).toSet()

        val earliestActivityDate = allActivityDates.minOrNull()
        var hasReachedFirstActiveDay = false

        val targetDailyCount = goal?.dailyTarget?.coerceAtLeast(1) ?: 1

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                time = referenceDate
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = dateFormat.format(dayCal.time)
            val dayLabel = dayLabelFormat.format(dayCal.time)
            val isToday = dateStr == todayStr
            val dayIndex = 7 - i

            val dayEvents = eventsByDate[dateStr] ?: emptyList()
            val dayProgress = progressByDate[dateStr]
            val dayTx = transactionsByDate[dateStr] ?: emptyList()

            // Meaningful interventions count
            val meaningfulInterventions = dayEvents.count {
                it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" || it.interventionType == "INTERVENTION_COMPLETED"
            } + (dayProgress?.completedCount ?: 0)

            val earnedSec = dayTx.filter { it.type == "EARN" }.sumOf { it.amountSeconds }

            if (meaningfulInterventions > 0 || (earliestActivityDate != null && dateStr >= earliestActivityDate)) {
                hasReachedFirstActiveDay = true
            }

            // Determine day status
            val status: HabitDayStatus
            var isRecovery = false

            if (meaningfulInterventions >= 2 || (dayProgress?.completedCount ?: 0) >= targetDailyCount) {
                status = HabitDayStatus.STRONG
            } else if (meaningfulInterventions >= 1) {
                status = HabitDayStatus.COMPLETED
            } else if (dayEvents.isNotEmpty()) {
                status = HabitDayStatus.PARTIAL
            } else if (isToday) {
                status = HabitDayStatus.ACTIVE
            } else if (hasReachedFirstActiveDay) {
                status = HabitDayStatus.MISSED
            } else {
                status = HabitDayStatus.NOT_STARTED
            }

            val isDayMeaningful = status == HabitDayStatus.COMPLETED || status == HabitDayStatus.STRONG

            // Recovery Detection: Previous active -> 1+ missed -> today meaningful
            if (isDayMeaningful) {
                if (previousDayWasActive && consecutiveMissedDays > 0) {
                    isRecovery = true
                    totalRecoveries++
                }
                previousDayWasActive = true
                consecutiveMissedDays = 0
            } else if (status == HabitDayStatus.MISSED) {
                consecutiveMissedDays++
            }

            rawDays.add(
                HabitDay(
                    dayIndex = dayIndex,
                    dayLabel = dayLabel,
                    dateString = dateStr,
                    status = status,
                    interventionCount = meaningfulInterventions,
                    earnedSeconds = earnedSec,
                    isToday = isToday,
                    isRecovery = isRecovery
                )
            )
        }

        val meaningfulDays = rawDays.count { it.status == HabitDayStatus.COMPLETED || it.status == HabitDayStatus.STRONG }
        val strongDays = rawDays.count { it.status == HabitDayStatus.STRONG }
        val missedDays = rawDays.count { it.status == HabitDayStatus.MISSED }
        val totalInterventions = rawDays.sumOf { it.interventionCount }
        val totalEarnedMinutes = rawDays.sumOf { it.earnedSeconds } / 60
        val totalSavedMinutes = rawDays.sumOf { it.earnedSeconds } / 60 // Saved or available
        val todayCompleted = rawDays.find { it.isToday }?.let { it.status == HabitDayStatus.COMPLETED || it.status == HabitDayStatus.STRONG } ?: false
        val isWeekCompleted = meaningfulDays >= 5 || rawDays.all { it.status != HabitDayStatus.NOT_STARTED && it.status != HabitDayStatus.FUTURE }

        // Most effective intervention
        val mostEffective = events.filter { it.status == "COMPLETED" }
            .groupBy { it.interventionType }
            .maxByOrNull { it.value.size }?.key?.replace("_", " ")?.lowercase(Locale.US)?.replaceFirstChar { it.uppercase() }

        // Momentum Score Calculation (0..100)
        val momentumScore = calculateScore(
            meaningfulDays = meaningfulDays,
            strongDays = strongDays,
            missedDays = missedDays,
            recoveryCount = totalRecoveries,
            totalInterventions = totalInterventions,
            firstWinCompleted = firstWinCompleted
        )

        // Momentum Tier
        val tier = when {
            momentumScore >= 70 -> HabitMomentumTier.STRONG_MOMENTUM
            momentumScore >= 35 -> HabitMomentumTier.BUILDING_MOMENTUM
            momentumScore >= 10 -> HabitMomentumTier.GETTING_STARTED
            else -> HabitMomentumTier.NEEDS_ATTENTION
        }

        // Milestones
        val milestones = listOf(
            HabitMilestone(
                id = "m_first_win",
                title = "First Small Win",
                description = "Interrupted your first distraction and made a conscious choice.",
                isReached = firstWinCompleted || totalInterventions >= 1
            ),
            HabitMilestone(
                id = "m_3_days",
                title = "3 Meaningful Days",
                description = "Paused habit impulses across 3 days this week.",
                isReached = meaningfulDays >= 3
            ),
            HabitMilestone(
                id = "m_5_days",
                title = "5 Protected Days",
                description = "Built a steady pattern of positive friction.",
                isReached = meaningfulDays >= 5
            ),
            HabitMilestone(
                id = "m_7_days",
                title = "First Week Complete",
                description = "Finished a full 7-day habit formation cycle.",
                isReached = isWeekCompleted
            )
        )

        val milestoneText = when {
            isWeekCompleted -> "7-Day Cycle Completed 🎉"
            meaningfulDays >= 5 -> "5 Protected Days Reached ✓"
            meaningfulDays >= 3 -> "3 Meaningful Days Reached ✓"
            firstWinCompleted -> "First Win Done ✓"
            else -> "Ready for your first win"
        }

        val weekSummary = HabitWeekSummary(
            totalDays = 7,
            meaningfulDaysCount = meaningfulDays,
            strongDaysCount = strongDays,
            missedDaysCount = missedDays,
            recoveryCount = totalRecoveries,
            totalInterventionsCount = totalInterventions,
            totalEarnedMinutes = totalEarnedMinutes,
            totalSavedMinutes = totalSavedMinutes,
            mostEffectiveIntervention = mostEffective ?: "Mindful pause",
            isWeekCompleted = isWeekCompleted,
            milestoneText = milestoneText
        )

        // Contextual Insight
        val insight = when {
            totalRecoveries > 0 -> "Good recovery. Coming back after a quiet day builds lasting discipline."
            meaningfulDays >= 5 -> "Your interruptions are becoming second nature. Keep protecting your focus."
            meaningfulDays >= 3 -> "You've interrupted distractions on 3 different days. Consistency is forming."
            totalInterventions >= 1 -> "Every pause between impulse and action rewires the dopamine loop."
            else -> "Small interruptions become habits. Complete one small challenge today."
        }

        return HabitMomentumSnapshot(
            days = rawDays,
            momentumScore = momentumScore,
            momentumTier = tier,
            meaningfulDaysCount = meaningfulDays,
            recoveryCount = totalRecoveries,
            todayCompleted = todayCompleted,
            isWeekCompleted = isWeekCompleted,
            weekSummary = weekSummary,
            milestones = milestones,
            contextualInsight = insight
        )
    }

    /**
     * Pure deterministic momentum score formula bounded between 0 and 100.
     */
    fun calculateScore(
        meaningfulDays: Int,
        strongDays: Int,
        missedDays: Int,
        recoveryCount: Int,
        totalInterventions: Int,
        firstWinCompleted: Boolean
    ): Int {
        // Base consistency: up to 50 pts
        val consistencyPts = (meaningfulDays.toFloat() / 7f) * 50f

        // Strong days bonus: up to 15 pts (5 pts per strong day, max 3)
        val strongBonus = min(15, strongDays * 5)

        // Density / volume bonus: up to 15 pts
        val volumeBonus = min(15, totalInterventions * 3)

        // Recovery resilience bonus: up to 15 pts
        val recoveryBonus = min(15, recoveryCount * 8)

        // First win foundation: 5 pts
        val firstWinBonus = if (firstWinCompleted) 5 else 0

        // Gentle missed day penalty: at most 4 pts per missed day
        val missedPenalty = missedDays * 4

        val rawScore = (consistencyPts + strongBonus + volumeBonus + recoveryBonus + firstWinBonus - missedPenalty).toInt()
        return rawScore.coerceIn(0, 100)
    }
}

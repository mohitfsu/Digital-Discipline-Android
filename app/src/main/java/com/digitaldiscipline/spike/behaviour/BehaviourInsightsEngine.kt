package com.digitaldiscipline.spike.behaviour

import com.digitaldiscipline.spike.data.local.entities.DailyUsageEntity
import com.digitaldiscipline.spike.data.local.entities.GoalProgressEntity
import com.digitaldiscipline.spike.data.local.entities.InterventionEventEntity
import com.digitaldiscipline.spike.data.local.entities.WalletTransactionEntity
import com.digitaldiscipline.spike.data.local.entities.WalletTransactionType

data class BestInterventionResult(
    val interventionType: String,
    val displayName: String,
    val interruptionRate: Float,
    val trialCount: Int
)

data class DistractionPatternResult(
    val hasSufficientData: Boolean,
    val message: String,
    val peakHourStart: Int? = null,
    val peakHourEnd: Int? = null,
    val topApp: String? = null
)

data class FeedbackResult(
    val ruleId: String,
    val feedbackMessage: String,
    val isPositive: Boolean
)

data class ConsistencyResult(
    val activeDays: Int,
    val totalDays: Int,
    val summaryText: String
)

data class WeeklyTrendResult(
    val totalScreenTimeMinutes: Int,
    val interventionAttempts: Int,
    val completedInterventions: Int,
    val habitInterruptionRate: Float
)

data class RecentWin(
    val id: String,
    val title: String,
    val timestamp: Long,
    val icon: String = "✓"
)

enum class TriggerReflection(val key: String, val displayName: String) {
    INTENTIONAL_USE("INTENTIONAL_USE", "I actually want to use it"),
    BOREDOM("BOREDOM", "I'm bored"),
    AVOIDANCE("AVOIDANCE", "I'm avoiding something"),
    HABIT("HABIT", "Just habit"),
    SKIPPED("SKIPPED", "Skipped reflection")
}

object BehaviourInsightsEngine {

    /**
     * Calculates deterministic Habit Interruption Rate (HIR).
     * Formula: (Attempts without 5-min rapid reopen / Total Attempts) * 100
     */
    fun calculateHabitInterruptionRate(events: List<InterventionEventEntity>): Float {
        if (events.isEmpty()) return 100.0f
        val totalAttempts = events.size
        val uninterrupted = events.count { !it.reopenWithin5Minutes }
        return ((uninterrupted.toFloat() / totalAttempts.toFloat()) * 100.0f).coerceIn(0.0f, 100.0f)
    }

    /**
     * Identifies the best performing positive friction challenge (min 10 trials).
     */
    fun calculateBestIntervention(events: List<InterventionEventEntity>): BestInterventionResult? {
        val grouped = events.groupBy { it.interventionType.uppercase() }
        var best: BestInterventionResult? = null

        grouped.forEach { (type, typeEvents) ->
            if (typeEvents.size >= 10 && type != "PARENT_OVERRIDE") {
                val reopens = typeEvents.count { it.reopenWithin5Minutes }
                val hir = ((typeEvents.size - reopens).toFloat() / typeEvents.size.toFloat()) * 100.0f
                val displayName = when (type) {
                    "SQUATS" -> "Squats"
                    "BREATHING" -> "Box Breathing"
                    "PAUSE" -> "Mindful Pause"
                    "PUSHUPS" -> "Pushups"
                    else -> type.lowercase().replaceFirstChar { it.uppercase() }
                }
                if (best == null || hir > best!!.interruptionRate) {
                    best = BestInterventionResult(
                        interventionType = type,
                        displayName = displayName,
                        interruptionRate = hir,
                        trialCount = typeEvents.size
                    )
                }
            }
        }
        return best
    }

    /**
     * Computes the user's primary distraction pattern (peak hours & top app).
     * Requires minimum 10 attempts to prevent noise.
     */
    fun calculateDistractionPattern(
        events: List<InterventionEventEntity>,
        minThreshold: Int = 10
    ): DistractionPatternResult {
        if (events.size < minThreshold) {
            return DistractionPatternResult(
                hasSufficientData = false,
                message = "Keep using Digital Discipline and we'll show your patterns here."
            )
        }

        // Top App
        val topAppEntry = events.groupBy { it.appDisplayName.ifBlank { it.packageName } }
            .maxByOrNull { it.value.size }
        val topAppName = topAppEntry?.key ?: "Distraction apps"

        // Peak Hours (2-hour bucket)
        val hourBuckets = IntArray(12) // 0=0-1, 1=2-3, ..., 10=20-21 (8-9PM), 11=22-23 (10-11PM)
        events.forEach { event ->
            val bucket = (event.hourOfDay / 2).coerceIn(0, 11)
            hourBuckets[bucket]++
        }
        val peakBucket = hourBuckets.indices.maxByOrNull { hourBuckets[it] } ?: 10
        val startHour = peakBucket * 2
        val endHour = (startHour + 2) % 24

        val startHourStr = formatHour(startHour)
        val endHourStr = formatHour(endHour)

        return DistractionPatternResult(
            hasSufficientData = true,
            message = "You tend to open $topAppName most often between $startHourStr and $endHourStr.",
            peakHourStart = startHour,
            peakHourEnd = endHour,
            topApp = topAppName
        )
    }

    private fun formatHour(hour: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val ampm = if (hour < 12) "AM" else "PM"
        return "$h $ampm"
    }

    /**
     * Evaluates personal behaviour feedback rules (A, B, C, D, E) deterministically.
     */
    fun evaluatePersonalFeedback(
        currentWeekEvents: List<InterventionEventEntity>,
        previousWeekEvents: List<InterventionEventEntity>
    ): FeedbackResult {
        val currentAttempts = currentWeekEvents.size
        if (currentAttempts < 10) {
            // Rule E: Insufficient data (<10 trials)
            return FeedbackResult(
                ruleId = "RULE_E",
                feedbackMessage = "Keep going. We'll show more useful patterns as you build history.",
                isPositive = true
            )
        }

        val currentHIR = calculateHabitInterruptionRate(currentWeekEvents)

        // Rule B & C: Compare with previous week if previous week had enough data
        if (previousWeekEvents.size >= 5) {
            val prevHIR = calculateHabitInterruptionRate(previousWeekEvents)
            val diff = currentHIR - prevHIR
            if (diff >= 10.0f) {
                // Rule B: Improvement by >=10 points
                return FeedbackResult(
                    ruleId = "RULE_B",
                    feedbackMessage = "Your ability to interrupt the habit is improving.",
                    isPositive = true
                )
            } else if (diff <= -10.0f) {
                // Rule C: Decline by >=10 points
                return FeedbackResult(
                    ruleId = "RULE_C",
                    feedbackMessage = "Your distraction pattern has become harder to interrupt this week.",
                    isPositive = false
                )
            }
        }

        // Rule D: Comparison between interventions
        val grouped = currentWeekEvents.groupBy { it.interventionType.uppercase() }
        if (grouped.size >= 2) {
            val stats = grouped.mapNotNull { (type, list) ->
                if (list.size >= 10 && type != "PARENT_OVERRIDE") {
                    val reopens = list.count { it.reopenWithin5Minutes }
                    val reopenRate = (reopens.toFloat() / list.size.toFloat()) * 100.0f
                    val name = when (type) {
                        "SQUATS" -> "Squats challenge"
                        "BREATHING" -> "Box breathing"
                        "PAUSE" -> "Mindful pause"
                        else -> type.lowercase()
                    }
                    name to reopenRate
                } else null
            }
            if (stats.size >= 2) {
                val sorted = stats.sortedBy { it.second } // lowest reopen rate first
                val best = sorted.first()
                val worst = sorted.last()
                if ((worst.second - best.second) >= 20.0f) {
                    return FeedbackResult(
                        ruleId = "RULE_D",
                        feedbackMessage = "Your ${best.first} appears to work better for you.",
                        isPositive = true
                    )
                }
            }
        }

        // Rule A: Success rate >=70%
        if (currentHIR >= 70.0f) {
            return FeedbackResult(
                ruleId = "RULE_A",
                feedbackMessage = "You're successfully interrupting most distraction attempts.",
                isPositive = true
            )
        }

        return FeedbackResult(
            ruleId = "DEFAULT",
            feedbackMessage = "You're building consistency with every intentional pause.",
            isPositive = true
        )
    }

    /**
     * Calculates consistency score (active days with at least 1 completed challenge in last N days).
     */
    fun calculateConsistency(
        progressList: List<GoalProgressEntity>,
        days: Int = 7
    ): ConsistencyResult {
        val activeDays = progressList.take(days).count { it.completedCount > 0 }
        return ConsistencyResult(
            activeDays = activeDays,
            totalDays = days,
            summaryText = "$activeDays of $days days"
        )
    }

    /**
     * Calculates 7-day trend metrics.
     */
    fun calculateWeeklyTrend(
        events: List<InterventionEventEntity>,
        usageList: List<DailyUsageEntity>
    ): WeeklyTrendResult {
        val totalScreenTimeMin = (usageList.sumOf { it.totalForegroundSeconds } / 60L).toInt()
        val attempts = events.size
        val completed = events.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
        val hir = calculateHabitInterruptionRate(events)

        return WeeklyTrendResult(
            totalScreenTimeMinutes = totalScreenTimeMin,
            interventionAttempts = attempts,
            completedInterventions = completed,
            habitInterruptionRate = hir
        )
    }

    /**
     * Computes "What Worked" insight statement.
     */
    fun getWhatWorkedSummary(events: List<InterventionEventEntity>): String? {
        val best = calculateBestIntervention(events) ?: return null
        val topApp = events.firstOrNull { it.interventionType.equals(best.interventionType, ignoreCase = true) }?.appDisplayName
            ?: "distraction"
        val rateInt = best.interruptionRate.toInt()
        return "${best.displayName} interrupted $rateInt% of your $topApp reopen attempts."
    }

    /**
     * Gathers recent positive accomplishments.
     */
    fun getRecentWins(
        events: List<InterventionEventEntity>,
        transactions: List<WalletTransactionEntity>,
        limit: Int = 4
    ): List<RecentWin> {
        val wins = mutableListOf<RecentWin>()

        // Add completed transactions
        transactions.filter { it.type == WalletTransactionType.EARN.name }.take(limit).forEach { tx ->
            val mins = (tx.amountSeconds / 60).coerceAtLeast(1)
            val title = when (tx.source) {
                "SQUATS", "SQUATS_CONFIRMED" -> "Completed physical challenge"
                "BREATHING" -> "Completed breathing exercise"
                "PAUSE" -> "Completed mindful pause"
                else -> "Earned $mins minutes"
            }
            wins.add(RecentWin(id = tx.transactionId, title = title, timestamp = tx.timestampWallClock))
        }

        // Add interrupted attempts
        events.filter { !it.reopenWithin5Minutes && it.status == "COMPLETED" }.take(limit).forEach { ev ->
            val app = ev.appDisplayName.ifBlank { ev.packageName }
            wins.add(RecentWin(id = ev.eventId, title = "Interrupted a $app reopen", timestamp = ev.timestamp))
        }

        return wins.distinctBy { it.title }.sortedByDescending { it.timestamp }.take(limit)
    }
}

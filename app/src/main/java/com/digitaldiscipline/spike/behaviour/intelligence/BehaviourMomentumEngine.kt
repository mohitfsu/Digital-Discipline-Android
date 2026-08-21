package com.digitaldiscipline.spike.behaviour.intelligence

import com.digitaldiscipline.spike.behaviour.BehaviourInsightsEngine
import com.digitaldiscipline.spike.data.local.entities.DailyUsageEntity
import com.digitaldiscipline.spike.data.local.entities.GoalEntity
import com.digitaldiscipline.spike.data.local.entities.GoalProgressEntity
import com.digitaldiscipline.spike.data.local.entities.InterventionEventEntity

enum class MomentumState(val displayName: String, val message: String, val badge: String) {
    STRONG_MOMENTUM("Strong momentum", "Your discipline is compounding powerfully.", "🔥"),
    BUILDING_MOMENTUM("Building momentum", "You're building solid consistency day by day.", "⚡"),
    INCONSISTENT("A little inconsistent", "You're making progress. Focus on your vulnerable hours.", "🌱"),
    NEEDS_RESET("Needs a reset", "Take a breath and start small with simple challenges.", "🔄"),
    NEEDS_ATTENTION("Needs attention", "Keep going. Each interrupted distraction is a win.", "💡"),
    INSUFFICIENT_DATA("Building baseline data", "Complete more daily sessions to unlock momentum score.", "⚪")
}

data class MomentumScoreBreakdown(
    val goalConsistencyScore: Float,
    val habitInterruptionScore: Float,
    val rapidReopenScore: Float,
    val interventionCompletionScore: Float,
    val goalProgressScore: Float,
    val screenTimeScore: Float,
    val planAdherenceScore: Float
)

data class BehaviourMomentumResult(
    val score: Int,
    val state: MomentumState,
    val breakdown: MomentumScoreBreakdown,
    val summaryText: String
)

object BehaviourMomentumEngine {

    const val MIN_SAMPLE_THRESHOLD = 10

    // Component Weights (Sum = 1.0)
    const val WEIGHT_GOAL_CONSISTENCY = 0.20f
    const val WEIGHT_HABIT_INTERRUPTION = 0.20f
    const val WEIGHT_RAPID_REOPEN = 0.15f
    const val WEIGHT_INTERVENTION_COMPLETION = 0.15f
    const val WEIGHT_GOAL_PROGRESS = 0.15f
    const val WEIGHT_SCREEN_TIME = 0.10f
    const val WEIGHT_PLAN_ADHERENCE = 0.05f

    /**
     * Calculates the deterministic 0–100 Behaviour Momentum Score.
     */
    fun calculateMomentumScore(
        events: List<InterventionEventEntity>,
        goal: GoalEntity?,
        progressList: List<GoalProgressEntity>,
        dailyUsage: List<DailyUsageEntity> = emptyList()
    ): BehaviourMomentumResult {
        if (events.size < MIN_SAMPLE_THRESHOLD) {
            val emptyBreakdown = MomentumScoreBreakdown(0f, 0f, 0f, 0f, 0f, 0f, 0f)
            return BehaviourMomentumResult(
                score = 0,
                state = MomentumState.INSUFFICIENT_DATA,
                breakdown = emptyBreakdown,
                summaryText = "Building baseline data (${events.size}/$MIN_SAMPLE_THRESHOLD trials)."
            )
        }

        // 1. Goal Consistency (Active days in past 7 days)
        val activeDays = progressList.take(7).count { it.completedCount > 0 }
        val consistencyScore = (activeDays.toFloat() / 7f) * 100f

        // 2. Habit Interruption Rate (HIR)
        val hirScore = BehaviourInsightsEngine.calculateHabitInterruptionRate(events)

        // 3. Rapid Reopen Score (Inverse of 5m reopen rate)
        val reopens = events.count { it.reopenWithin5Minutes }
        val rapidReopenRate = (reopens.toFloat() / events.size.toFloat()) * 100f
        val rapidReopenScore = (100f - rapidReopenRate).coerceIn(0f, 100f)

        // 4. Intervention Completion Score
        val completed = events.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
        val completionScore = (completed.toFloat() / events.size.toFloat()) * 100f

        // 5. Goal Progress Score (Daily Target Completion)
        val targetMetDays = progressList.take(7).count { it.completedCount >= (goal?.dailyTarget ?: 1) }
        val goalProgressScore = (targetMetDays.toFloat() / 7f) * 100f

        // 6. Screen-time improvement score
        val screenTimeScore = if (dailyUsage.isNotEmpty()) {
            val avgSeconds = dailyUsage.map { it.totalForegroundSeconds }.average()
            if (avgSeconds <= 7200) 100f else if (avgSeconds <= 14400) 75f else 50f
        } else {
            75f
        }

        // 7. Plan Adherence (Policies active and monitored)
        val planAdherenceScore = 90.0f

        val rawTotal = (consistencyScore * WEIGHT_GOAL_CONSISTENCY) +
                (hirScore * WEIGHT_HABIT_INTERRUPTION) +
                (rapidReopenScore * WEIGHT_RAPID_REOPEN) +
                (completionScore * WEIGHT_INTERVENTION_COMPLETION) +
                (goalProgressScore * WEIGHT_GOAL_PROGRESS) +
                (screenTimeScore * WEIGHT_SCREEN_TIME) +
                (planAdherenceScore * WEIGHT_PLAN_ADHERENCE)

        val finalScore = rawTotal.toInt().coerceIn(0, 100)

        val state = when (finalScore) {
            in 90..100 -> MomentumState.STRONG_MOMENTUM
            in 75..89 -> MomentumState.BUILDING_MOMENTUM
            in 50..74 -> MomentumState.INCONSISTENT
            in 25..49 -> MomentumState.NEEDS_RESET
            else -> MomentumState.NEEDS_ATTENTION
        }

        val breakdown = MomentumScoreBreakdown(
            goalConsistencyScore = consistencyScore,
            habitInterruptionScore = hirScore,
            rapidReopenScore = rapidReopenScore,
            interventionCompletionScore = completionScore,
            goalProgressScore = goalProgressScore,
            screenTimeScore = screenTimeScore,
            planAdherenceScore = planAdherenceScore
        )

        return BehaviourMomentumResult(
            score = finalScore,
            state = state,
            breakdown = breakdown,
            summaryText = state.message
        )
    }
}

package com.digitaldiscipline.spike.ai

import com.digitaldiscipline.spike.analytics.InterventionStats

enum class RecommendedType {
    PAUSE,
    BREATHING,
    SQUATS,
    PARENT_OVERRIDE
}

data class ChildContext(
    val packageName: String,
    val appCategory: String = "SOCIAL_MEDIA",
    val consecutiveOpenAttempts: Int = 1,
    val timeOfDayHour: Int = 16,
    val dayOfWeek: Int = 2,
    val totalTodayUsageMinutes: Int = 45,
    val historicalSquatCompletionRate: Float = 0.8f
)

data class RecommendedIntervention(
    val type: RecommendedType,
    val durationSeconds: Int,
    val targetReps: Int = 0,
    val rationale: String,
    val confidenceScore: Float = 1.0f
)

data class InterventionAdvisoryReport(
    val recommendedType: RecommendedType,
    val confidenceScore: Float,
    val reason: String,
    val supportingMetrics: Map<String, String>,
    val requiresParentApproval: Boolean = true
)

interface InterventionRecommendationEngine {
    fun recommendIntervention(context: ChildContext): RecommendedIntervention
    fun evaluateAdvisoryRecommendation(stats: Map<String, InterventionStats>): InterventionAdvisoryReport
    fun getEngineName(): String
}

class RuleBasedRecommendationEngine : InterventionRecommendationEngine {

    override fun recommendIntervention(context: ChildContext): RecommendedIntervention {
        return when {
            // If child repeatedly attempts opening the app in short burst (>3 times)
            context.consecutiveOpenAttempts >= 3 -> {
                RecommendedIntervention(
                    type = RecommendedType.BREATHING,
                    durationSeconds = 20,
                    rationale = "High attempt frequency detected. 20s calming breathing recommended to break compulsion loop.",
                    confidenceScore = 0.95f
                )
            }
            // If high physical activity adherence
            context.historicalSquatCompletionRate >= 0.7f && context.timeOfDayHour in 14..19 -> {
                RecommendedIntervention(
                    type = RecommendedType.SQUATS,
                    durationSeconds = 60,
                    targetReps = 10,
                    rationale = "Afternoon active window. 10 squats challenge recommended.",
                    confidenceScore = 0.90f
                )
            }
            // Standard mindful pause
            else -> {
                RecommendedIntervention(
                    type = RecommendedType.PAUSE,
                    durationSeconds = 10,
                    rationale = "Standard 10-second mindful pause before screen access.",
                    confidenceScore = 1.0f
                )
            }
        }
    }

    /**
     * Evaluates deterministic habit metrics across intervention types to formulate
     * an advisory recommendation for the parent dashboard.
     *
     * IMPORTANT: This DOES NOT automatically alter policy. It requires explicit parent approval.
     */
    override fun evaluateAdvisoryRecommendation(stats: Map<String, InterventionStats>): InterventionAdvisoryReport {
        val pauseStats = stats["PAUSE"] ?: InterventionStats("PAUSE", 0, 0, 0, 0, 100f)
        val breathingStats = stats["BREATHING"] ?: InterventionStats("BREATHING", 0, 0, 0, 0, 100f)
        val squatsStats = stats["SQUATS"] ?: InterventionStats("SQUATS", 0, 0, 0, 0, 100f)

        val pauseReopenRate = if (pauseStats.attempts > 0) (pauseStats.fiveMinuteReopens.toFloat() / pauseStats.attempts.toFloat()) * 100f else 50f
        val breathingReopenRate = if (breathingStats.attempts > 0) (breathingStats.fiveMinuteReopens.toFloat() / breathingStats.attempts.toFloat()) * 100f else 50f
        val squatsReopenRate = if (squatsStats.attempts > 0) (squatsStats.fiveMinuteReopens.toFloat() / squatsStats.attempts.toFloat()) * 100f else 50f

        return when {
            // Rule 1: Squats challenge has >= 10 trials and 20% lower reopen rate than pause
            squatsStats.completed >= 10 && (pauseReopenRate - squatsReopenRate) >= 20f -> {
                InterventionAdvisoryReport(
                    recommendedType = RecommendedType.SQUATS,
                    confidenceScore = 0.92f,
                    reason = "Physical challenges resulted in ${String.format("%.1f", pauseReopenRate - squatsReopenRate)}% fewer 5-minute reopens than mindful pauses this week.",
                    supportingMetrics = mapOf(
                        "squatsCompleted" to "${squatsStats.completed}",
                        "squatsReopenRate" to "${String.format("%.1f", squatsReopenRate)}%",
                        "pauseReopenRate" to "${String.format("%.1f", pauseReopenRate)}%",
                        "habitInterruptionDiff" to "+${String.format("%.1f", squatsStats.habitInterruptionRate - pauseStats.habitInterruptionRate)}%"
                    )
                )
            }
            // Rule 2: Breathing has >= 10 trials and 15% lower reopen rate than pause
            breathingStats.completed >= 10 && (pauseReopenRate - breathingReopenRate) >= 15f -> {
                InterventionAdvisoryReport(
                    recommendedType = RecommendedType.BREATHING,
                    confidenceScore = 0.88f,
                    reason = "Box breathing resulted in ${String.format("%.1f", pauseReopenRate - breathingReopenRate)}% fewer 5-minute reopens than simple pauses.",
                    supportingMetrics = mapOf(
                        "breathingCompleted" to "${breathingStats.completed}",
                        "breathingReopenRate" to "${String.format("%.1f", breathingReopenRate)}%",
                        "pauseReopenRate" to "${String.format("%.1f", pauseReopenRate)}%"
                    )
                )
            }
            else -> {
                InterventionAdvisoryReport(
                    recommendedType = RecommendedType.PAUSE,
                    confidenceScore = 0.80f,
                    reason = "Mindful pause continues to provide balanced friction with baseline habit interruption rate of ${String.format("%.1f", pauseStats.habitInterruptionRate)}%.",
                    supportingMetrics = mapOf(
                        "pauseAttempts" to "${pauseStats.attempts}",
                        "pauseHIR" to "${String.format("%.1f", pauseStats.habitInterruptionRate)}%"
                    )
                )
            }
        }
    }

    override fun getEngineName(): String = "LOCAL_DETERMINISTIC_HABIT_ENGINE"
}

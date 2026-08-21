package com.digitaldiscipline.spike.behaviour.intelligence

import com.digitaldiscipline.spike.behaviour.BehaviourInsightsEngine
import com.digitaldiscipline.spike.data.local.entities.GoalEntity
import com.digitaldiscipline.spike.data.local.entities.GoalProgressEntity
import com.digitaldiscipline.spike.data.local.entities.InterventionEventEntity

data class GoalDistractionRelationship(
    val goalTitle: String,
    val topDistractionApp: String,
    val bestInterventionName: String,
    val interruptionRate: Float,
    val narrativeSummary: String
)

data class GoalIntegrityResult(
    val score: Int,
    val goalTitle: String,
    val alignmentSummary: String,
    val relationship: GoalDistractionRelationship?
)

object GoalIntegrityEngine {

    const val MIN_SAMPLE_THRESHOLD = 10

    const val WEIGHT_TARGET_CONSISTENCY = 0.35f
    const val WEIGHT_INTERRUPTION_RATE = 0.35f
    const val WEIGHT_CHALLENGE_COMPLETION = 0.15f
    const val WEIGHT_REOPEN_CONTROL = 0.15f

    /**
     * Calculates deterministic Goal Integrity Score (0–100).
     */
    fun calculateGoalIntegrity(
        goal: GoalEntity?,
        progressList: List<GoalProgressEntity>,
        events: List<InterventionEventEntity>
    ): GoalIntegrityResult {
        val goalName = goal?.title ?: "Focus"

        if (events.size < MIN_SAMPLE_THRESHOLD) {
            return GoalIntegrityResult(
                score = 100,
                goalTitle = goalName,
                alignmentSummary = "Building baseline alignment for your $goalName goal.",
                relationship = null
            )
        }

        // 1. Target Consistency (Active days vs 7 days)
        val activeDays = progressList.take(7).count { it.completedCount > 0 }
        val consistencyScore = (activeDays.toFloat() / 7f) * 100f

        // 2. Interruption Rate
        val hirScore = BehaviourInsightsEngine.calculateHabitInterruptionRate(events)

        // 3. Challenge Completion
        val completed = events.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
        val completionScore = (completed.toFloat() / events.size.toFloat()) * 100f

        // 4. Reopen Control (100 - rapid reopen rate)
        val reopens = events.count { it.reopenWithin5Minutes }
        val reopenRate = (reopens.toFloat() / events.size.toFloat()) * 100f
        val reopenControlScore = (100f - reopenRate).coerceIn(0f, 100f)

        val rawScore = (consistencyScore * WEIGHT_TARGET_CONSISTENCY) +
                (hirScore * WEIGHT_INTERRUPTION_RATE) +
                (completionScore * WEIGHT_CHALLENGE_COMPLETION) +
                (reopenControlScore * WEIGHT_REOPEN_CONTROL)

        val finalScore = rawScore.toInt().coerceIn(0, 100)

        val alignmentSummary = when {
            finalScore >= 80 -> "Your behaviour is strongly aligned with your $goalName goal."
            finalScore >= 60 -> "Your behaviour is moderately aligned with your $goalName goal."
            else -> "Your behaviour is building alignment with your $goalName goal."
        }

        // Goal <-> Distraction Relationship
        val topApp = events.groupBy { it.appDisplayName.ifBlank { it.packageName } }
            .maxByOrNull { it.value.size }?.key ?: "Distraction apps"
        val bestIntervention = BehaviourInsightsEngine.calculateBestIntervention(events)
        val bestName = bestIntervention?.displayName ?: "Mindful Pause"
        val bestHIR = bestIntervention?.interruptionRate ?: hirScore

        val relationship = GoalDistractionRelationship(
            goalTitle = goalName,
            topDistractionApp = topApp,
            bestInterventionName = bestName,
            interruptionRate = bestHIR,
            narrativeSummary = "$topApp appears to be your primary distraction, while $bestName works best for you so far (${bestHIR.toInt()}% interruption rate)."
        )

        return GoalIntegrityResult(
            score = finalScore,
            goalTitle = goalName,
            alignmentSummary = alignmentSummary,
            relationship = relationship
        )
    }
}

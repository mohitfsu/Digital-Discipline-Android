package com.digitaldiscipline.spike.behaviour.continuity

import com.digitaldiscipline.spike.behaviour.adaptive.BehaviourRecommendation
import com.digitaldiscipline.spike.behaviour.adaptive.PlanHealth
import com.digitaldiscipline.spike.behaviour.adaptive.RecommendationType
import com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumSnapshot
import com.digitaldiscipline.spike.data.local.entities.GoalEntity
import com.digitaldiscipline.spike.data.local.entities.ReplacementBehaviourEntity
import java.util.Locale
import kotlin.math.max

/**
 * Phase 4E-4 — Personal Habit Plan Refinement & Long-Term Continuity Engine
 *
 * Deterministic orchestrator that bridges the 7-day habit-formation loop
 * into ongoing, user-approved plan refinement without gamification or cloud dependencies.
 */
object PlanContinuityEngine {

    /**
     * Evaluates the complete plan continuity snapshot.
     */
    fun evaluateContinuitySnapshot(
        habitSnapshot: HabitMomentumSnapshot,
        activeGoal: GoalEntity?,
        activeBehaviour: ReplacementBehaviourEntity?,
        activeRewardSeconds: Int,
        recommendation: BehaviourRecommendation?,
        lastPlanReviewTimestamp: Long = 0L,
        savedContinuityState: String? = null,
        savedWeekNumber: Int? = null,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): PlanContinuitySnapshot {
        val meaningfulDays = habitSnapshot.meaningfulDaysCount
        val totalInterventions = habitSnapshot.weekSummary.totalInterventionsCount
        val isFirstWeekComplete = habitSnapshot.isWeekCompleted || meaningfulDays >= 5

        // Determine Week Number: If saved, use max(1, savedWeekNumber). Otherwise derive from first week completion
        val activeWeekNumber = when {
            savedWeekNumber != null && savedWeekNumber > 0 -> savedWeekNumber
            isFirstWeekComplete -> 2
            else -> 1
        }

        // Determine Plan Health
        val planHealth = when {
            totalInterventions == 0 && meaningfulDays == 0 -> PlanHealth.INSUFFICIENT_DATA
            recommendation != null && recommendation.type != RecommendationType.KEEP_PLAN && recommendation.type != RecommendationType.INSUFFICIENT_DATA -> PlanHealth.NEEDS_ADJUSTMENT
            meaningfulDays >= 3 || totalInterventions >= 3 -> PlanHealth.WORKING
            else -> PlanHealth.INSUFFICIENT_DATA
        }

        // Determine Continuity State
        val state = when {
            savedContinuityState != null && savedContinuityState.isNotBlank() -> {
                try {
                    PlanContinuityState.valueOf(savedContinuityState)
                } catch (e: Exception) {
                    deriveState(isFirstWeekComplete, recommendation, lastPlanReviewTimestamp, currentTimeMillis)
                }
            }
            else -> deriveState(isFirstWeekComplete, recommendation, lastPlanReviewTimestamp, currentTimeMillis)
        }

        // Build Evidence Summary
        val evidenceSummary = PlanEvidenceSummary(
            totalInterventionsCount = totalInterventions,
            meaningfulDaysCount = meaningfulDays,
            topInterventionName = habitSnapshot.weekSummary.mostEffectiveIntervention ?: (activeBehaviour?.title ?: "Positive friction"),
            interruptionRatePercentage = if (totalInterventions > 0) 75f else 0f,
            totalEarnedMinutes = habitSnapshot.weekSummary.totalEarnedMinutes,
            totalSavedMinutes = habitSnapshot.weekSummary.totalSavedMinutes,
            peakDistractionWindow = "Evenings (8 PM – 10 PM)",
            goalConsistencyNarrative = when {
                meaningfulDays >= 5 -> "Strong consistency across 5+ days this week."
                meaningfulDays >= 3 -> "Solid habit pauses across 3 days this week."
                meaningfulDays >= 1 -> "You've started building intentional friction."
                else -> "Quiet baseline week. Small interruptions build habits."
            }
        )

        // Build Change Preview if an actionable recommendation exists
        val changePreview = recommendation?.let { rec ->
            if (rec.type != RecommendationType.KEEP_PLAN && rec.type != RecommendationType.INSUFFICIENT_DATA) {
                createPlanChangePreview(rec, activeBehaviour, activeRewardSeconds)
            } else {
                null
            }
        }

        // Headlines and Narratives
        val headline = when (state) {
            PlanContinuityState.FIRST_WEEK_REVIEW -> "Your First Week is Complete"
            PlanContinuityState.PLAN_CONFIRMED -> "Week $activeWeekNumber — Ongoing Routine"
            PlanContinuityState.PLAN_REFINED -> "Plan Refined & Ready"
            PlanContinuityState.PLAN_NEEDS_REVIEW -> "Plan Adjustment Recommended"
            PlanContinuityState.LEARNING -> "Week 1 — Learning Phase"
            PlanContinuityState.INSUFFICIENT_DATA -> "Building Baseline"
        }

        val narrative = when (state) {
            PlanContinuityState.FIRST_WEEK_REVIEW -> "You've learned what helps you interrupt distracting impulses. Review what worked or keep going."
            PlanContinuityState.PLAN_CONFIRMED -> "Your plan is confirmed for Week $activeWeekNumber. Keep doing what works."
            PlanContinuityState.PLAN_REFINED -> "Your updated challenge and reward are active for the upcoming days."
            PlanContinuityState.PLAN_NEEDS_REVIEW -> "A small adjustment may help you protect your focus more effectively."
            PlanContinuityState.LEARNING -> "Practice interrupting impulses one day at a time."
            PlanContinuityState.INSUFFICIENT_DATA -> "We're still learning your natural distraction patterns."
        }

        return PlanContinuitySnapshot(
            state = state,
            activeWeekNumber = activeWeekNumber,
            isFirstWeekCompleted = isFirstWeekComplete,
            planHealth = planHealth,
            activeGoal = activeGoal,
            activeBehaviour = activeBehaviour,
            activeRewardSeconds = activeRewardSeconds,
            recommendation = recommendation,
            evidenceSummary = evidenceSummary,
            changePreview = changePreview,
            statusHeadline = headline,
            statusNarrative = narrative
        )
    }

    private fun deriveState(
        isFirstWeekComplete: Boolean,
        recommendation: BehaviourRecommendation?,
        lastReviewTimestamp: Long,
        currentTimeMillis: Long
    ): PlanContinuityState {
        val daysSinceReview = if (lastReviewTimestamp > 0) {
            (currentTimeMillis - lastReviewTimestamp) / 86400000L
        } else {
            999L
        }

        return when {
            isFirstWeekComplete && lastReviewTimestamp == 0L -> PlanContinuityState.FIRST_WEEK_REVIEW
            recommendation != null && recommendation.type != RecommendationType.KEEP_PLAN && recommendation.type != RecommendationType.INSUFFICIENT_DATA -> PlanContinuityState.PLAN_NEEDS_REVIEW
            daysSinceReview < 7 -> PlanContinuityState.PLAN_CONFIRMED
            isFirstWeekComplete -> PlanContinuityState.PLAN_CONFIRMED
            else -> PlanContinuityState.LEARNING
        }
    }

    /**
     * Builds a concrete before/after diff for user review before committing changes.
     */
    fun createPlanChangePreview(
        recommendation: BehaviourRecommendation,
        currentBehaviour: ReplacementBehaviourEntity?,
        currentRewardSeconds: Int
    ): PlanChangePreview {
        val currentTitle = currentBehaviour?.title ?: "10 Squats"
        val currentReward = currentRewardSeconds
        val currentCooldown = 0

        val suggestedTitle = recommendation.suggestedInterventionType?.let {
            it.replace("_", " ").lowercase(Locale.US).replaceFirstChar { c -> c.uppercase() }
        } ?: recommendation.suggestedConfiguration

        val suggestedReward = recommendation.suggestedRewardSeconds ?: when (recommendation.type) {
            RecommendationType.REDUCE_REWARD -> 300 // 5 mins
            else -> currentReward
        }

        val suggestedCooldown = recommendation.cooldownSeconds

        val changes = mutableListOf<String>()
        if (suggestedTitle != currentTitle && recommendation.type == RecommendationType.CHANGE_INTERVENTION) {
            changes.add("Challenge changes to $suggestedTitle")
        }
        if (recommendation.type == RecommendationType.SHORTER_INTERVENTION) {
            changes.add("Challenge duration/reps reduced for easier compliance")
        }
        if (suggestedReward < currentReward) {
            changes.add("Screen time reward reduced to ${suggestedReward / 60}m")
        }
        if (suggestedCooldown > 0) {
            changes.add("Adds a ${suggestedCooldown}s intentional cooldown")
        }
        if (recommendation.type == RecommendationType.CHANGE_DISTRACTION_WINDOW) {
            changes.add("Adjusts active distraction protection hours")
        }
        if (changes.isEmpty()) {
            changes.add("Optimizes positive friction balance")
        }

        return PlanChangePreview(
            recommendationType = recommendation.type,
            currentInterventionTitle = currentTitle,
            suggestedInterventionTitle = suggestedTitle,
            currentRewardSeconds = currentReward,
            suggestedRewardSeconds = suggestedReward,
            currentCooldownSeconds = currentCooldown,
            suggestedCooldownSeconds = suggestedCooldown,
            explanation = recommendation.explanation,
            changesSummary = changes
        )
    }
}

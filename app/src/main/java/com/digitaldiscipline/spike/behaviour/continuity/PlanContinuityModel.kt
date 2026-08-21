package com.digitaldiscipline.spike.behaviour.continuity

import com.digitaldiscipline.spike.behaviour.adaptive.BehaviourRecommendation
import com.digitaldiscipline.spike.behaviour.adaptive.PlanHealth
import com.digitaldiscipline.spike.behaviour.adaptive.RecommendationType
import com.digitaldiscipline.spike.data.local.entities.GoalEntity
import com.digitaldiscipline.spike.data.local.entities.ReplacementBehaviourEntity

/**
 * Phase 4E-4 — Plan Continuity State
 *
 * Represents the lifecycle of personal habit plan refinement and long-term continuity.
 */
enum class PlanContinuityState {
    LEARNING,             // First week in progress (< 7 days or < 5 active days)
    FIRST_WEEK_REVIEW,    // First 7-day cycle completed, awaiting initial review / keep decision
    PLAN_CONFIRMED,       // User explicitly confirmed current plan for ongoing cycle
    PLAN_NEEDS_REVIEW,    // Adaptive engine suggests adjustment or periodic weekly review is ready
    PLAN_REFINED,         // User recently applied an approved plan adjustment
    INSUFFICIENT_DATA     // Not enough baseline telemetry to evaluate
}

data class PlanEvidenceSummary(
    val totalInterventionsCount: Int,
    val meaningfulDaysCount: Int,
    val topInterventionName: String,
    val interruptionRatePercentage: Float,
    val totalEarnedMinutes: Int,
    val totalSavedMinutes: Int,
    val peakDistractionWindow: String? = null,
    val goalConsistencyNarrative: String
)

data class PlanChangePreview(
    val recommendationType: RecommendationType,
    val currentInterventionTitle: String,
    val suggestedInterventionTitle: String,
    val currentRewardSeconds: Int,
    val suggestedRewardSeconds: Int,
    val currentCooldownSeconds: Int,
    val suggestedCooldownSeconds: Int,
    val explanation: String,
    val changesSummary: List<String>
)

data class PlanContinuitySnapshot(
    val state: PlanContinuityState,
    val activeWeekNumber: Int,
    val isFirstWeekCompleted: Boolean,
    val planHealth: PlanHealth,
    val activeGoal: GoalEntity?,
    val activeBehaviour: ReplacementBehaviourEntity?,
    val activeRewardSeconds: Int,
    val recommendation: BehaviourRecommendation?,
    val evidenceSummary: PlanEvidenceSummary,
    val changePreview: PlanChangePreview?,
    val statusHeadline: String,
    val statusNarrative: String
)

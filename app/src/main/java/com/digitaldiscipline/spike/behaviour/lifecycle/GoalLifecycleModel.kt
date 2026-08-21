package com.digitaldiscipline.spike.behaviour.lifecycle

import com.digitaldiscipline.spike.data.local.entities.GoalEntity

/**
 * Phase 4E-5 — Goal Lifecycle State
 *
 * Represents the lifecycle stages of personal behavior goals.
 */
enum class GoalLifecycleState {
    ACTIVE,      // Goal is currently active and enforcing positive friction
    PAUSED,      // Goal is intentionally paused (friction and notifications suppressed)
    COMPLETED,   // User intentionally achieved/concluded this goal chapter
    REPLACED,    // Goal was replaced by a new primary goal
    ARCHIVED     // Historical goal stored permanently in read-only history
}

enum class GoalTransitionType {
    PAUSE,
    RESUME,
    COMPLETE,
    REPLACE,
    START_FRESH,
    ARCHIVE
}

enum class TransitionValidationResult {
    VALID,
    INVALID,
    REQUIRES_CONFIRMATION,
    NO_OP
}

data class GoalTransitionPreview(
    val transitionType: GoalTransitionType,
    val currentGoalTitle: String,
    val targetGoalTitle: String? = null,
    val currentState: GoalLifecycleState,
    val targetState: GoalLifecycleState,
    val whatChanges: List<String>,
    val whatStays: List<String>,
    val confirmationHeadline: String,
    val confirmationNarrative: String
)

data class HistoricalGoalSummary(
    val goalId: String,
    val title: String,
    val category: String,
    val state: GoalLifecycleState,
    val startedDateFormatted: String,
    val endedDateFormatted: String? = null,
    val meaningfulDaysCount: Int = 0,
    val totalInterventionsCount: Int = 0,
    val totalEarnedMinutes: Int = 0,
    val totalSavedMinutes: Int = 0,
    val topInterventionName: String? = null
)

data class GoalLifecycleSnapshot(
    val activeGoal: GoalEntity?,
    val lifecycleState: GoalLifecycleState,
    val daysActiveCount: Int,
    val meaningfulDaysCount: Int,
    val totalInterventionsCount: Int,
    val earnedMinutesCount: Int,
    val availableTransitions: List<GoalTransitionType>,
    val statusHeadline: String,
    val statusNarrative: String
)

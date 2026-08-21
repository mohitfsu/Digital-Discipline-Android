package com.digitaldiscipline.spike.behaviour.lifecycle

import com.digitaldiscipline.spike.data.local.entities.GoalEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 4E-5 — Goal Lifecycle Engine
 *
 * Deterministic, off-path evaluation engine for validating goal transitions,
 * generating before/after consequence previews, and aggregating historical goal records.
 */
object GoalLifecycleEngine {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    /**
     * Validates whether a requested transition is allowable from the current lifecycle state.
     */
    fun validateTransition(
        currentState: GoalLifecycleState,
        transitionType: GoalTransitionType
    ): TransitionValidationResult {
        return when (currentState) {
            GoalLifecycleState.ACTIVE -> {
                when (transitionType) {
                    GoalTransitionType.PAUSE -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    GoalTransitionType.COMPLETE -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    GoalTransitionType.REPLACE -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    GoalTransitionType.START_FRESH -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    GoalTransitionType.ARCHIVE -> TransitionValidationResult.VALID
                    GoalTransitionType.RESUME -> TransitionValidationResult.NO_OP
                }
            }
            GoalLifecycleState.PAUSED -> {
                when (transitionType) {
                    GoalTransitionType.RESUME -> TransitionValidationResult.VALID
                    GoalTransitionType.COMPLETE -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    GoalTransitionType.REPLACE -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    GoalTransitionType.START_FRESH -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    GoalTransitionType.ARCHIVE -> TransitionValidationResult.VALID
                    GoalTransitionType.PAUSE -> TransitionValidationResult.NO_OP
                }
            }
            GoalLifecycleState.COMPLETED -> {
                when (transitionType) {
                    GoalTransitionType.ARCHIVE -> TransitionValidationResult.VALID
                    GoalTransitionType.REPLACE -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    GoalTransitionType.START_FRESH -> TransitionValidationResult.REQUIRES_CONFIRMATION
                    else -> TransitionValidationResult.INVALID
                }
            }
            GoalLifecycleState.REPLACED -> {
                when (transitionType) {
                    GoalTransitionType.ARCHIVE -> TransitionValidationResult.VALID
                    else -> TransitionValidationResult.INVALID
                }
            }
            GoalLifecycleState.ARCHIVED -> {
                TransitionValidationResult.INVALID
            }
        }
    }

    /**
     * Computes the target state resulting from a valid transition.
     */
    fun evaluateTargetState(
        currentState: GoalLifecycleState,
        transitionType: GoalTransitionType
    ): GoalLifecycleState? {
        val validation = validateTransition(currentState, transitionType)
        if (validation == TransitionValidationResult.INVALID) return null

        return when (transitionType) {
            GoalTransitionType.PAUSE -> GoalLifecycleState.PAUSED
            GoalTransitionType.RESUME -> GoalLifecycleState.ACTIVE
            GoalTransitionType.COMPLETE -> GoalLifecycleState.COMPLETED
            GoalTransitionType.REPLACE -> GoalLifecycleState.REPLACED
            GoalTransitionType.START_FRESH -> GoalLifecycleState.ACTIVE
            GoalTransitionType.ARCHIVE -> GoalLifecycleState.ARCHIVED
        }
    }

    /**
     * Generates a concrete transition preview detailing what changes, what stays, and the consequence narrative.
     */
    fun createTransitionPreview(
        transitionType: GoalTransitionType,
        currentGoal: GoalEntity?,
        targetGoalTitle: String? = null
    ): GoalTransitionPreview {
        val title = currentGoal?.title ?: "Personal Goal"

        return when (transitionType) {
            GoalTransitionType.PAUSE -> {
                GoalTransitionPreview(
                    transitionType = GoalTransitionType.PAUSE,
                    currentGoalTitle = title,
                    currentState = GoalLifecycleState.ACTIVE,
                    targetState = GoalLifecycleState.PAUSED,
                    whatChanges = listOf(
                        "Self Mode pause prompts are silenced",
                        "Goal-specific action reminders stop"
                    ),
                    whatStays = listOf(
                        "All previous progress and habit evidence are preserved",
                        "Earned wallet screen time remains fully intact",
                        "Parent Mode protection rules remain active"
                    ),
                    confirmationHeadline = "Pause This Goal?",
                    confirmationNarrative = "Your progress won't be deleted. Your history stays here. Your plan simply stops asking you to complete challenges."
                )
            }
            GoalTransitionType.RESUME -> {
                GoalTransitionPreview(
                    transitionType = GoalTransitionType.RESUME,
                    currentGoalTitle = title,
                    currentState = GoalLifecycleState.PAUSED,
                    targetState = GoalLifecycleState.ACTIVE,
                    whatChanges = listOf(
                        "Positive friction interventions resume on distraction apps",
                        "Daily focus reminders re-activate"
                    ),
                    whatStays = listOf(
                        "All past progress and momentum are restored seamlessly",
                        "Active challenge and reward settings remain unchanged"
                    ),
                    confirmationHeadline = "Resume Goal?",
                    confirmationNarrative = "Welcome back. Pick up right where you left off."
                )
            }
            GoalTransitionType.COMPLETE -> {
                GoalTransitionPreview(
                    transitionType = GoalTransitionType.COMPLETE,
                    currentGoalTitle = title,
                    currentState = GoalLifecycleState.ACTIVE,
                    targetState = GoalLifecycleState.COMPLETED,
                    whatChanges = listOf(
                        "Marks this life goal chapter as completed",
                        "Clears the active daily dashboard to choose your next focus"
                    ),
                    whatStays = listOf(
                        "Complete history permanently recorded in Goal History",
                        "All earned wallet minutes and analytics preserved"
                    ),
                    confirmationHeadline = "Ready to Complete This Goal?",
                    confirmationNarrative = "Your progress and history will remain available in Goal History. Completing the goal simply means you're choosing to move on."
                )
            }
            GoalTransitionType.REPLACE -> {
                val newTitle = targetGoalTitle ?: "New Goal"
                GoalTransitionPreview(
                    transitionType = GoalTransitionType.REPLACE,
                    currentGoalTitle = title,
                    targetGoalTitle = newTitle,
                    currentState = GoalLifecycleState.ACTIVE,
                    targetState = GoalLifecycleState.REPLACED,
                    whatChanges = listOf(
                        "Primary goal switches to $newTitle",
                        "New positive friction challenge and reward active"
                    ),
                    whatStays = listOf(
                        "Previous goal archived in Goal History with all evidence",
                        "Wallet balance and transactions remain unchanged",
                        "Past weekly reviews and reflections preserved"
                    ),
                    confirmationHeadline = "Switch to $newTitle?",
                    confirmationNarrative = "You are evolving your focus. Your previous work on $title will be saved in your Goal History."
                )
            }
            GoalTransitionType.START_FRESH -> {
                GoalTransitionPreview(
                    transitionType = GoalTransitionType.START_FRESH,
                    currentGoalTitle = title,
                    currentState = GoalLifecycleState.ACTIVE,
                    targetState = GoalLifecycleState.ACTIVE,
                    whatChanges = listOf(
                        "Resets active challenge and reward configuration to draft baseline",
                        "Starts a fresh 7-day habit momentum cycle"
                    ),
                    whatStays = listOf(
                        "All historical wallet ledger transactions preserved",
                        "All past analytics events and weekly reviews retained",
                        "No data is erased"
                    ),
                    confirmationHeadline = "Start Fresh With $title?",
                    confirmationNarrative = "This creates a clean active starting plan. Your previous progress and telemetry will remain safe in your history."
                )
            }
            GoalTransitionType.ARCHIVE -> {
                GoalTransitionPreview(
                    transitionType = GoalTransitionType.ARCHIVE,
                    currentGoalTitle = title,
                    currentState = GoalLifecycleState.COMPLETED,
                    targetState = GoalLifecycleState.ARCHIVED,
                    whatChanges = listOf("Moves goal to permanent archive"),
                    whatStays = listOf("Complete read-only summary in Goal History"),
                    confirmationHeadline = "Archive Goal?",
                    confirmationNarrative = "Stores this goal in permanent historical archives."
                )
            }
        }
    }

    /**
     * Builds a comprehensive lifecycle snapshot.
     */
    fun evaluateLifecycleSnapshot(
        activeGoal: GoalEntity?,
        currentState: GoalLifecycleState,
        meaningfulDays: Int,
        totalInterventions: Int,
        earnedMinutes: Int
    ): GoalLifecycleSnapshot {
        val daysActive = if (activeGoal != null) {
            val diffMs = System.currentTimeMillis() - activeGoal.startDate
            (diffMs / 86400000L).toInt().coerceAtLeast(1)
        } else {
            0
        }

        val available = when (currentState) {
            GoalLifecycleState.ACTIVE -> listOf(
                GoalTransitionType.PAUSE,
                GoalTransitionType.COMPLETE,
                GoalTransitionType.REPLACE,
                GoalTransitionType.START_FRESH
            )
            GoalLifecycleState.PAUSED -> listOf(
                GoalTransitionType.RESUME,
                GoalTransitionType.COMPLETE,
                GoalTransitionType.REPLACE,
                GoalTransitionType.START_FRESH
            )
            GoalLifecycleState.COMPLETED -> listOf(
                GoalTransitionType.REPLACE,
                GoalTransitionType.START_FRESH
            )
            GoalLifecycleState.REPLACED -> emptyList()
            GoalLifecycleState.ARCHIVED -> emptyList()
        }

        val headline = when (currentState) {
            GoalLifecycleState.ACTIVE -> "Goal Active"
            GoalLifecycleState.PAUSED -> "Goal Paused"
            GoalLifecycleState.COMPLETED -> "Goal Completed"
            GoalLifecycleState.REPLACED -> "Goal Replaced"
            GoalLifecycleState.ARCHIVED -> "Goal Archived"
        }

        val narrative = when (currentState) {
            GoalLifecycleState.ACTIVE -> "Your active behavior plan is protecting your focus."
            GoalLifecycleState.PAUSED -> "Your plan is on pause. You can resume anytime without losing progress."
            GoalLifecycleState.COMPLETED -> "You finished this chapter. Choose your next goal when ready."
            GoalLifecycleState.REPLACED -> "This goal was archived when you chose a new focus."
            GoalLifecycleState.ARCHIVED -> "Stored permanently in your goal history."
        }

        return GoalLifecycleSnapshot(
            activeGoal = activeGoal,
            lifecycleState = currentState,
            daysActiveCount = daysActive,
            meaningfulDaysCount = meaningfulDays,
            totalInterventionsCount = totalInterventions,
            earnedMinutesCount = earnedMinutes,
            availableTransitions = available,
            statusHeadline = headline,
            statusNarrative = narrative
        )
    }

    /**
     * Converts a goal entity and telemetry into a clean historical summary.
     */
    fun buildHistoricalGoalSummary(
        goal: GoalEntity,
        state: GoalLifecycleState,
        meaningfulDays: Int = 0,
        totalInterventions: Int = 0,
        earnedMinutes: Int = 0,
        savedMinutes: Int = 0,
        topIntervention: String? = null
    ): HistoricalGoalSummary {
        val startedFormatted = dateFormat.format(Date(goal.startDate))
        val endedFormatted = if (goal.updatedAt > goal.startDate && (state == GoalLifecycleState.COMPLETED || state == GoalLifecycleState.REPLACED || state == GoalLifecycleState.ARCHIVED)) {
            dateFormat.format(Date(goal.updatedAt))
        } else {
            null
        }

        return HistoricalGoalSummary(
            goalId = goal.goalId,
            title = goal.title,
            category = goal.category,
            state = state,
            startedDateFormatted = startedFormatted,
            endedDateFormatted = endedFormatted,
            meaningfulDaysCount = meaningfulDays,
            totalInterventionsCount = totalInterventions,
            totalEarnedMinutes = earnedMinutes,
            totalSavedMinutes = savedMinutes,
            topInterventionName = topIntervention
        )
    }
}

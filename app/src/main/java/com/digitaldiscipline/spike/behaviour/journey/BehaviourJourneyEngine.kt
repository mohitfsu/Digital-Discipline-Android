package com.digitaldiscipline.spike.behaviour.journey

import com.digitaldiscipline.spike.behaviour.adaptive.PlanHealth
import com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState
import com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumSnapshot
import com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumTier
import com.digitaldiscipline.spike.data.local.entities.AdjustmentStatus
import com.digitaldiscipline.spike.data.local.entities.GoalEntity
import com.digitaldiscipline.spike.data.local.entities.InterventionEventEntity
import com.digitaldiscipline.spike.data.local.entities.PlanAdjustmentEntity
import com.digitaldiscipline.spike.data.local.entities.WeeklyReviewEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 4E-6 — Behaviour Journey Engine
 *
 * Deterministic, off-path synthesizer for deriving a private chronological timeline
 * of meaningful behavioral milestones, evidence-backed learnings, and long-term summaries.
 */
object BehaviourJourneyEngine {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    fun evaluateJourneySnapshot(
        goals: List<GoalEntity>,
        currentLifecycleState: GoalLifecycleState,
        currentWeekNumber: Int,
        events: List<InterventionEventEntity>,
        weeklyReviews: List<WeeklyReviewEntity> = emptyList(),
        planAdjustments: List<PlanAdjustmentEntity> = emptyList(),
        firstWinState: String? = null,
        firstWinTimestamp: Long = 0L,
        firstWinActionTitle: String? = null,
        habitSnapshot: HabitMomentumSnapshot? = null,
        pausedAtTimestamp: Long = 0L,
        completedAtTimestamp: Long = 0L
    ): BehaviourJourneySnapshot {
        val primaryGoal = goals.firstOrNull { it.active } ?: goals.firstOrNull()
        val timeline = mutableListOf<JourneyEvent>()

        // 1. Synthesize First Win Event
        if (firstWinTimestamp > 0L && (firstWinState == "FIRST_WIN_COMPLETED" || firstWinState == "TIME_USED" || firstWinState == "TIME_SAVED")) {
            val action = firstWinActionTitle ?: "Mindful Pause"
            timeline.add(
                JourneyEvent(
                    eventId = "first_win_${firstWinTimestamp}",
                    eventType = JourneyEventType.FIRST_WIN,
                    timestamp = firstWinTimestamp,
                    dateFormatted = dateFormat.format(Date(firstWinTimestamp)),
                    title = "First Win Achieved",
                    shortDescription = "Interrupted your first distraction attempt with $action.",
                    importance = EventImportance.MILESTONE,
                    supportingMetric = "+10m",
                    supportingMetricLabel = "Banked"
                )
            )
        }

        // 2. Synthesize Goal Lifecycle Events
        goals.forEach { goal ->
            // Goal Started
            if (goal.startDate > 0L) {
                timeline.add(
                    JourneyEvent(
                        eventId = "goal_started_${goal.goalId}",
                        eventType = JourneyEventType.GOAL_STARTED,
                        timestamp = goal.startDate,
                        dateFormatted = dateFormat.format(Date(goal.startDate)),
                        title = "Goal Started",
                        shortDescription = "Committed to '${goal.title}' in ${goal.category.lowercase()}.",
                        goalId = goal.goalId,
                        goalName = goal.title,
                        importance = EventImportance.HIGH
                    )
                )
            }

            // Goal Completed
            if (!goal.active && goal.updatedAt > goal.startDate) {
                timeline.add(
                    JourneyEvent(
                        eventId = "goal_completed_${goal.goalId}",
                        eventType = JourneyEventType.GOAL_COMPLETED,
                        timestamp = goal.updatedAt,
                        dateFormatted = dateFormat.format(Date(goal.updatedAt)),
                        title = "Goal Chapter Completed",
                        shortDescription = "Concluded positive friction for '${goal.title}'.",
                        goalId = goal.goalId,
                        goalName = goal.title,
                        importance = EventImportance.MILESTONE
                    )
                )
            }
        }

        // 3. Synthesize Current Paused Event
        if (currentLifecycleState == GoalLifecycleState.PAUSED && pausedAtTimestamp > 0L) {
            timeline.add(
                JourneyEvent(
                    eventId = "goal_paused_${pausedAtTimestamp}",
                    eventType = JourneyEventType.GOAL_PAUSED,
                    timestamp = pausedAtTimestamp,
                    dateFormatted = dateFormat.format(Date(pausedAtTimestamp)),
                    title = "Goal Paused",
                    shortDescription = "Temporarily silenced positive friction. History preserved.",
                    goalId = primaryGoal?.goalId,
                    goalName = primaryGoal?.title,
                    importance = EventImportance.MEDIUM
                )
            )
        }

        // 4. Synthesize Plan Refinement Events
        planAdjustments.forEach { adj ->
            if (adj.status == AdjustmentStatus.ACCEPTED.name || adj.appliedAt > 0L) {
                timeline.add(
                    JourneyEvent(
                        eventId = "plan_refined_${adj.adjustmentId}",
                        eventType = JourneyEventType.PLAN_REFINED,
                        timestamp = if (adj.appliedAt > 0L) adj.appliedAt else adj.createdAt,
                        dateFormatted = dateFormat.format(Date(if (adj.appliedAt > 0L) adj.appliedAt else adj.createdAt)),
                        title = "Plan Refined",
                        shortDescription = adj.reason.ifBlank { "Refined intervention configuration." },
                        goalId = primaryGoal?.goalId,
                        goalName = primaryGoal?.title,
                        importance = EventImportance.HIGH
                    )
                )
            }
        }

        // 5. Synthesize Weekly Reviews
        weeklyReviews.forEach { review ->
            timeline.add(
                JourneyEvent(
                    eventId = "weekly_review_${review.reviewId}",
                    eventType = JourneyEventType.WEEKLY_REVIEW,
                    timestamp = review.generatedAt,
                    dateFormatted = dateFormat.format(Date(review.generatedAt)),
                    title = "Weekly Review Completed",
                    shortDescription = "${review.completed} challenges completed • ${review.earnedSeconds / 60}m banked.",
                    importance = EventImportance.MEDIUM,
                    supportingMetric = "${review.completed}",
                    supportingMetricLabel = "Pauses"
                )
            )
        }

        // 6. Synthesize Habit Momentum & Recovery Events
        if (habitSnapshot != null) {
            if (habitSnapshot.recoveryCount > 0) {
                timeline.add(
                    JourneyEvent(
                        eventId = "recovery_detected_${System.currentTimeMillis() / 86400000L}",
                        eventType = JourneyEventType.RECOVERY_DETECTED,
                        timestamp = System.currentTimeMillis() - 3600000L,
                        dateFormatted = dateFormat.format(Date()),
                        title = "Recovery Detected",
                        shortDescription = "Returned to positive friction after a quiet period.",
                        importance = EventImportance.HIGH
                    )
                )
            }

            if (habitSnapshot.meaningfulDaysCount >= 5) {
                timeline.add(
                    JourneyEvent(
                        eventId = "momentum_milestone_${System.currentTimeMillis() / 86400000L}",
                        eventType = JourneyEventType.HABIT_MOMENTUM,
                        timestamp = System.currentTimeMillis() - 7200000L,
                        dateFormatted = dateFormat.format(Date()),
                        title = "Momentum Milestone",
                        shortDescription = "Completed meaningful positive friction on ${habitSnapshot.meaningfulDaysCount} days this week.",
                        importance = EventImportance.MILESTONE,
                        supportingMetric = "${habitSnapshot.momentumScore}/100",
                        supportingMetricLabel = "Score"
                    )
                )
            }
        }

        // Deduplicate and sort chronologically (newest first)
        val deduplicatedEvents = timeline
            .distinctBy { it.eventId }
            .sortedByDescending { it.timestamp }

        // 7. Calculate Aggregated Summary
        val completedGoalsCount = goals.count { !it.active }
        val totalMeaningfulActions = events.size
        val totalSavedMinutes = events.size * 10
        val totalInterventions = events.size

        // 8. Derive 1–3 Evidence-Backed Personal Learnings
        val learnings = deriveLearnings(events, goals, planAdjustments, habitSnapshot)

        // 9. Plan Health & Direction
        val planHealth = when {
            habitSnapshot != null && habitSnapshot.momentumScore >= 60 -> PlanHealth.WORKING
            habitSnapshot != null && habitSnapshot.momentumScore >= 30 -> PlanHealth.WORKING
            events.isEmpty() -> PlanHealth.INSUFFICIENT_DATA
            else -> PlanHealth.NEEDS_ADJUSTMENT
        }

        val directionHeadline = when (currentLifecycleState) {
            GoalLifecycleState.PAUSED -> "Goal is currently paused."
            GoalLifecycleState.COMPLETED -> "Ready for your next chapter."
            else -> when (planHealth) {
                PlanHealth.WORKING -> "Keep building on what works."
                PlanHealth.NEEDS_ADJUSTMENT -> "Your plan may be ready for a review."
                PlanHealth.NOT_WORKING -> "Consider trying a different positive friction."
                PlanHealth.INSUFFICIENT_DATA -> "Continue building your initial baseline."
            }
        }

        val directionNarrative = when (currentLifecycleState) {
            GoalLifecycleState.PAUSED -> "Resume whenever you are ready to reactivate positive friction."
            GoalLifecycleState.COMPLETED -> "You've successfully finished this goal. Start a new focus when ready."
            else -> "Your positive friction routine is protecting your daily focus."
        }

        val directionAction = when (currentLifecycleState) {
            GoalLifecycleState.PAUSED -> "RESUME GOAL"
            GoalLifecycleState.COMPLETED -> "CHOOSE NEXT GOAL"
            else -> "REVIEW PLAN"
        }

        return BehaviourJourneySnapshot(
            currentGoal = primaryGoal,
            currentGoalLifecycleState = currentLifecycleState,
            currentWeekNumber = currentWeekNumber.coerceAtLeast(1),
            planHealth = planHealth,
            habitMomentumScore = habitSnapshot?.momentumScore ?: 0,
            habitMomentumTier = habitSnapshot?.momentumTier ?: HabitMomentumTier.GETTING_STARTED,
            summary = JourneyLongTermSummary(
                totalGoalChaptersCompleted = completedGoalsCount,
                totalMeaningfulActionsCount = totalMeaningfulActions,
                totalSavedMinutesCount = totalSavedMinutes,
                totalInterventionsInterrupted = totalInterventions,
                topLearnings = learnings
            ),
            timelineEvents = deduplicatedEvents,
            currentDirectionHeadline = directionHeadline,
            currentDirectionNarrative = directionNarrative,
            currentDirectionActionLabel = directionAction
        )
    }

    private fun deriveLearnings(
        events: List<InterventionEventEntity>,
        goals: List<GoalEntity>,
        adjustments: List<PlanAdjustmentEntity>,
        habitSnapshot: HabitMomentumSnapshot?
    ): List<PersonalPatternInsight> {
        val list = mutableListOf<PersonalPatternInsight>()

        if (events.size >= 3) {
            list.add(
                PersonalPatternInsight(
                    title = "Positive Friction Interrupts Impulses",
                    narrative = "Completing brief challenges reliably diverts distraction attempts.",
                    evidence = "${events.size} distractions successfully interrupted"
                )
            )
        }

        if (habitSnapshot != null && habitSnapshot.recoveryCount > 0) {
            list.add(
                PersonalPatternInsight(
                    title = "Quick Habit Recovery",
                    narrative = "You consistently resume your plan after missed days.",
                    evidence = "Recovered after low-activity window"
                )
            )
        }

        if (adjustments.any { it.status == AdjustmentStatus.ACCEPTED.name || it.appliedAt > 0L }) {
            list.add(
                PersonalPatternInsight(
                    title = "Adaptive Plan Evolution",
                    narrative = "Refining your challenge parameters improved consistency.",
                    evidence = "${adjustments.count { it.status == AdjustmentStatus.ACCEPTED.name || it.appliedAt > 0L }} plan refinements applied"
                )
            )
        }

        if (list.isEmpty()) {
            list.add(
                PersonalPatternInsight(
                    title = "Building Initial Baseline",
                    narrative = "You are currently discovering what intervention styles work best for you.",
                    evidence = "Early journey learning"
                )
            )
        }

        return list.take(3)
    }
}

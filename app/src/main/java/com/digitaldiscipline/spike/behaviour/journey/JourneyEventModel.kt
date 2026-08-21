package com.digitaldiscipline.spike.behaviour.journey

import com.digitaldiscipline.spike.behaviour.adaptive.PlanHealth
import com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState
import com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumTier
import com.digitaldiscipline.spike.data.local.entities.GoalEntity

/**
 * Phase 4E-6 — Journey Event Types
 *
 * Deterministic types of meaningful personal behavioral milestones.
 */
enum class JourneyEventType {
    FIRST_WIN,
    MILESTONE_REACHED,
    HABIT_MOMENTUM,
    PLAN_STARTED,
    PLAN_REFINED,
    PLAN_CONFIRMED,
    INTERVENTION_CHANGED,
    GOAL_STARTED,
    GOAL_PAUSED,
    GOAL_RESUMED,
    GOAL_COMPLETED,
    GOAL_REPLACED,
    GOAL_ARCHIVED,
    WEEKLY_REVIEW,
    RECOVERY_DETECTED,
    MEANINGFUL_IMPROVEMENT,
    PERSONAL_PATTERN_DISCOVERED
}

enum class EventImportance {
    LOW,
    MEDIUM,
    HIGH,
    MILESTONE
}

data class JourneyEvent(
    val eventId: String,
    val eventType: JourneyEventType,
    val timestamp: Long,
    val dateFormatted: String,
    val title: String,
    val shortDescription: String,
    val goalId: String? = null,
    val goalName: String? = null,
    val supportingMetric: String? = null,
    val supportingMetricLabel: String? = null,
    val importance: EventImportance = EventImportance.MEDIUM
)

data class PersonalPatternInsight(
    val title: String,
    val narrative: String,
    val evidence: String
)

data class JourneyLongTermSummary(
    val totalGoalChaptersCompleted: Int,
    val totalMeaningfulActionsCount: Int,
    val totalSavedMinutesCount: Int,
    val totalInterventionsInterrupted: Int,
    val topLearnings: List<PersonalPatternInsight>
)

data class BehaviourJourneySnapshot(
    val currentGoal: GoalEntity?,
    val currentGoalLifecycleState: GoalLifecycleState,
    val currentWeekNumber: Int,
    val planHealth: PlanHealth,
    val habitMomentumScore: Int,
    val habitMomentumTier: HabitMomentumTier,
    val summary: JourneyLongTermSummary,
    val timelineEvents: List<JourneyEvent>,
    val currentDirectionHeadline: String,
    val currentDirectionNarrative: String,
    val currentDirectionActionLabel: String
)

package com.digitaldiscipline.spike.behaviour.momentum

/**
 * Phase 4E-3 — Habit Day Status
 *
 * Represents the status of a single day in the 7-day formation cycle.
 */
enum class HabitDayStatus {
    NOT_STARTED,
    ACTIVE,
    PARTIAL,
    COMPLETED,
    STRONG,
    REST_DAY,
    MISSED,
    FUTURE
}

data class HabitDay(
    val dayIndex: Int,          // 1..7
    val dayLabel: String,        // "Mon", "Tue", "Day 1", etc.
    val dateString: String,      // "2026-08-17"
    val status: HabitDayStatus,
    val interventionCount: Int,
    val earnedSeconds: Int,
    val isToday: Boolean,
    val isRecovery: Boolean = false
)

enum class HabitMomentumTier(val title: String, val narrative: String) {
    GETTING_STARTED("Getting started", "Small interruptions become habits."),
    BUILDING_MOMENTUM("Building momentum", "You're building solid consistency day by day."),
    STRONG_MOMENTUM("Strong momentum", "You are getting better at interrupting the habit."),
    NEEDS_ATTENTION("Needs a little attention", "One small action today is enough to get back.")
}

data class HabitMilestone(
    val id: String,
    val title: String,
    val description: String,
    val isReached: Boolean,
    val reachedAtText: String = ""
)

data class HabitWeekSummary(
    val totalDays: Int,
    val meaningfulDaysCount: Int,
    val strongDaysCount: Int,
    val missedDaysCount: Int,
    val recoveryCount: Int,
    val totalInterventionsCount: Int,
    val totalEarnedMinutes: Int,
    val totalSavedMinutes: Int,
    val mostEffectiveIntervention: String?,
    val isWeekCompleted: Boolean,
    val milestoneText: String
)

data class HabitMomentumSnapshot(
    val days: List<HabitDay>,
    val momentumScore: Int, // 0..100
    val momentumTier: HabitMomentumTier,
    val meaningfulDaysCount: Int,
    val recoveryCount: Int,
    val todayCompleted: Boolean,
    val isWeekCompleted: Boolean,
    val weekSummary: HabitWeekSummary,
    val milestones: List<HabitMilestone>,
    val contextualInsight: String
)

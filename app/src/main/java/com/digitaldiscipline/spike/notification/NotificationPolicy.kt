package com.digitaldiscipline.spike.notification

/**
 * Phase 4D-3 — Smart Notification Domain Model
 *
 * Defines all notification types, decisions, preferences, and context objects.
 * No LLM, no cloud, no surveillance data. Fully deterministic and local.
 *
 * PRIVACY GUARANTEE: Uses only existing local behavioural data already collected.
 * No new surveillance capabilities are introduced.
 */

// ---------------------------------------------------------------------------
// Notification Type
// ---------------------------------------------------------------------------

enum class NotificationType(val channelId: String, val label: String) {
    /** Morning: set today's intention. Max 1/day. */
    MORNING_INTENTION("digital_discipline_daily", "Morning Intention"),
    /** Reminder to complete today's next action. Max 1/day. */
    NEXT_ACTION("digital_discipline_actions", "Next Action Reminder"),
    /** Pre-empt a known high-risk distraction period. Max 1/day. */
    DISTRACTION_PREEMPTION("digital_discipline_actions", "Distraction Window"),
    /** Nudge if action still incomplete late in day. Max 1/day. */
    MISSED_ACTION("digital_discipline_actions", "Missed Action"),
    /** Positive reinforcement on completion. Max 1/day. */
    SUCCESS("digital_discipline_daily", "Goal Achieved"),
    /** Evening: prompt daily reflection. Max 1/day. */
    EVENING_REFLECTION("digital_discipline_daily", "Evening Reflection"),
    /** Prompt for weekly review. Max 1/week. */
    WEEKLY_REVIEW("digital_discipline_weekly", "Weekly Review")
}

// ---------------------------------------------------------------------------
// Notification Decision
// ---------------------------------------------------------------------------

sealed class NotificationDecision {
    /** Post the notification immediately. */
    data class Show(val candidate: NotificationCandidate) : NotificationDecision()
    /** Do not post. Reason recorded for telemetry. */
    data class Suppress(val reason: String) : NotificationDecision()
    /** Re-evaluate later (defer to next scheduled window). */
    data class Defer(val reason: String, val deferMinutes: Int = 60) : NotificationDecision()
    /** Reschedule to a better time today. */
    data class Reschedule(val reason: String, val targetHour: Int) : NotificationDecision()
}

// ---------------------------------------------------------------------------
// Notification Candidate — scored context object used by SmartNotificationEngine
// ---------------------------------------------------------------------------

data class NotificationCandidate(
    val type: NotificationType,
    val title: String,
    val body: String,
    val deepLink: String,
    val actionLabel: String? = null,
    val actionDeepLink: String? = null,
    val dismissLabel: String = "Later",
    val goalId: String = "",
    val actionId: String = "",
    val reason: String = ""
)

// ---------------------------------------------------------------------------
// Notification Context — all input data consumed by the engine (immutable snapshot)
// ---------------------------------------------------------------------------

data class NotificationContext(
    val isSelfMode: Boolean,
    val isParentMode: Boolean,
    val hasActivePlan: Boolean,
    val goalTitle: String,
    val goalCategory: String,
    val goalUnit: String,
    val dailyTarget: Int,
    val completedToday: Int,
    val isGoalComplete: Boolean,
    val nextActionTitle: String,
    val nextActionId: String,
    val walletBalanceSeconds: Int,
    val distractionPeakHour: Int?,           // null = insufficient data
    val distractionPeakDayOfWeek: Int?,      // null = insufficient data
    val distractionDataPoints: Int,          // must reach threshold for preemption
    val recentInterventionCount: Int,        // last 24 h
    val rapidReopenCount: Int,               // last 24 h
    val behaviourMomentumScore: Int,         // 0-100
    val currentHour: Int,
    val currentDayOfWeek: Int,
    val reflectionCompletedToday: Boolean,
    val weeklyReviewDue: Boolean,
    val notificationHistory: List<NotificationRecord>
)

// ---------------------------------------------------------------------------
// Notification Record — persisted as JSON in DataStore, no Room migration needed
// ---------------------------------------------------------------------------

data class NotificationRecord(
    val type: String,
    val timestampMs: Long,
    val reason: String,
    val goalId: String = "",
    val actionId: String = "",
    val userInteracted: Boolean = false,
    val userDismissed: Boolean = false,
    val resultedInCompletion: Boolean = false
)

// ---------------------------------------------------------------------------
// Notification Preferences — stored in DataStore via PreferencesManager
// ---------------------------------------------------------------------------

enum class NotificationFrequencyMode { MINIMAL, BALANCED, HELPFUL }

data class NotificationPreferences(
    val enableDailyFocus: Boolean = true,
    val enableActionReminders: Boolean = true,
    val enableDistractionWindow: Boolean = true,
    val enableSuccess: Boolean = true,
    val enableEveningReflection: Boolean = true,
    val enableWeeklyReview: Boolean = true,
    val frequencyMode: NotificationFrequencyMode = NotificationFrequencyMode.BALANCED
) {
    /** Maps frequency mode to total daily notification cap. */
    val maxTotalPerDay: Int get() = when (frequencyMode) {
        NotificationFrequencyMode.MINIMAL  -> 1
        NotificationFrequencyMode.BALANCED -> 3
        NotificationFrequencyMode.HELPFUL  -> 5
    }

    /** Maps frequency mode to behaviour-reminder daily cap. */
    val maxBehaviourRemindersPerDay: Int get() = when (frequencyMode) {
        NotificationFrequencyMode.MINIMAL  -> 1
        NotificationFrequencyMode.BALANCED -> 2
        NotificationFrequencyMode.HELPFUL  -> 3
    }

    /** Minimum gap between any two notifications in minutes. */
    val minGapMinutes: Int get() = when (frequencyMode) {
        NotificationFrequencyMode.MINIMAL  -> 180
        NotificationFrequencyMode.BALANCED -> 120
        NotificationFrequencyMode.HELPFUL  -> 60
    }
}

// ---------------------------------------------------------------------------
// Governor State — in-memory daily counters, reset at midnight
// ---------------------------------------------------------------------------

data class GovernorState(
    val dateString: String,
    val totalSentToday: Int = 0,
    val behaviourRemindersSentToday: Int = 0,
    val successSentToday: Int = 0,
    val preemptiveSentToday: Int = 0,
    val missedActionSentToday: Int = 0,
    val perTypeSentToday: Map<String, Int> = emptyMap(),
    val lastSentTimestampMs: Long = 0L
)

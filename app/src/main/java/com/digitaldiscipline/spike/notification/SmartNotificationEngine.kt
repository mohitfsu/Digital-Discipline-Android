package com.digitaldiscipline.spike.notification

import java.util.concurrent.TimeUnit

/**
 * Phase 4D-3 — Smart Notification Engine
 *
 * A pure deterministic scoring engine that decides whether a given notification type
 * should be SHOWN, SUPPRESSED, DEFERRED, or RESCHEDULED.
 *
 * CONTRACT:
 * - Pure object: no coroutines, no Room, no DataStore, no network, no side-effects.
 * - Accepts an immutable NotificationContext snapshot; returns a NotificationDecision.
 * - Evaluation latency target: <10ms.
 * - NEVER called from AccessibilityService, PolicyEngine, or OverlayManager.
 * - Parent Mode is the primary suppression gate: if isParentMode==true, every type returns Suppress.
 *
 * SCORING MODEL:
 *   NotificationScore =
 *       actionUrgency          [0–30] incomplete action, progressed fraction
 *     + distractionRisk        [0–20] approaching known peak hour
 *     + goalRelevance          [0–10] plan active + goal healthy
 *     + historicalRelevance    [0–10] pattern data sufficient
 *     - recentNotificationPenalty  [0–30] another notification was sent within gap
 *     - dismissalPenalty           [0–20] user dismissed same type recently
 *     - completionPenalty          [0–50] goal already complete today
 *
 * Threshold to SHOW: score >= 30.
 * Score is only used as a gate; the type and copy are pre-determined externally.
 */
object SmartNotificationEngine {

    // Minimum number of distraction data points required before showing a preemption notification.
    const val MIN_DISTRACTION_DATA_POINTS = 3

    // Score threshold above which a notification may be shown.
    private const val SHOW_THRESHOLD = 30

    // Dismissal recency window: if user dismissed within this period, apply penalty.
    private const val DISMISSAL_PENALTY_WINDOW_HOURS = 4

    // How far (in hours) before the distraction peak window is "approaching".
    private const val PEAK_APPROACH_WINDOW_HOURS = 1

    /**
     * Evaluate whether a given notification type should be shown for the supplied context.
     * Returns [NotificationDecision.Show], [NotificationDecision.Suppress],
     * [NotificationDecision.Defer], or [NotificationDecision.Reschedule].
     */
    fun evaluate(type: NotificationType, ctx: NotificationContext, prefs: NotificationPreferences): NotificationDecision {

        // --- Primary suppression gates ---

        if (ctx.isParentMode || !ctx.isSelfMode) {
            return NotificationDecision.Suppress("Parent Mode active — Self Mode notifications suppressed")
        }

        if (!ctx.hasActivePlan) {
            return NotificationDecision.Suppress("No active Self Mode plan")
        }

        // --- Type-level preference gate ---
        if (!isTypeEnabled(type, prefs)) {
            return NotificationDecision.Suppress("User preference disabled for $type")
        }

        // --- Type-specific evaluation ---
        return when (type) {
            NotificationType.MORNING_INTENTION    -> evaluateMorningIntention(ctx, prefs)
            NotificationType.NEXT_ACTION          -> evaluateNextAction(ctx, prefs)
            NotificationType.DISTRACTION_PREEMPTION -> evaluateDistractionPreemption(ctx, prefs)
            NotificationType.MISSED_ACTION        -> evaluateMissedAction(ctx, prefs)
            NotificationType.SUCCESS              -> evaluateSuccess(ctx, prefs)
            NotificationType.EVENING_REFLECTION   -> evaluateEveningReflection(ctx, prefs)
            NotificationType.WEEKLY_REVIEW        -> evaluateWeeklyReview(ctx, prefs)
        }
    }

    // ---------------------------------------------------------------------------
    // MORNING_INTENTION
    // ---------------------------------------------------------------------------
    private fun evaluateMorningIntention(ctx: NotificationContext, prefs: NotificationPreferences): NotificationDecision {
        // Only show in the morning window (6–10)
        if (ctx.currentHour < 6 || ctx.currentHour >= 10) {
            return NotificationDecision.Suppress("Outside morning window (hour=${ctx.currentHour})")
        }
        if (ctx.isGoalComplete) {
            return NotificationDecision.Suppress("Goal already complete today")
        }
        if (alreadySentTodayOfType(NotificationType.MORNING_INTENTION, ctx.notificationHistory)) {
            return NotificationDecision.Suppress("Already sent morning intention today")
        }

        val score = computeBaseScore(ctx)
        return decideFromScore(
            score = score,
            candidate = NotificationCandidate(
                type = NotificationType.MORNING_INTENTION,
                title = "Today's focus",
                body = buildMorningBody(ctx),
                deepLink = "digitaldiscipline://today",
                actionLabel = "VIEW TODAY",
                actionDeepLink = "digitaldiscipline://today",
                goalId = "",
                actionId = ctx.nextActionId,
                reason = "morning_intention"
            )
        )
    }

    // ---------------------------------------------------------------------------
    // NEXT_ACTION
    // ---------------------------------------------------------------------------
    private fun evaluateNextAction(ctx: NotificationContext, prefs: NotificationPreferences): NotificationDecision {
        if (ctx.isGoalComplete) {
            return NotificationDecision.Suppress("Goal already complete today")
        }
        if (ctx.completedToday > 0 && ctx.completedToday >= ctx.dailyTarget) {
            return NotificationDecision.Suppress("Goal met")
        }
        if (ctx.nextActionId.isBlank()) {
            return NotificationDecision.Suppress("No next action available")
        }
        if (alreadySentTodayOfType(NotificationType.NEXT_ACTION, ctx.notificationHistory)) {
            return NotificationDecision.Suppress("Already sent action reminder today")
        }
        // Only show during sensible hours (8–20)
        if (ctx.currentHour < 8 || ctx.currentHour >= 20) {
            return NotificationDecision.Suppress("Outside action reminder window (hour=${ctx.currentHour})")
        }

        val score = computeBaseScore(ctx) + actionUrgency(ctx)
        return decideFromScore(
            score = score,
            candidate = NotificationCandidate(
                type = NotificationType.NEXT_ACTION,
                title = "Your action is still waiting",
                body = "Your next action: ${ctx.nextActionTitle}. It takes just a few minutes.",
                deepLink = "digitaldiscipline://action/${ctx.nextActionId}",
                actionLabel = "DO IT NOW",
                actionDeepLink = "digitaldiscipline://action/${ctx.nextActionId}",
                goalId = "",
                actionId = ctx.nextActionId,
                reason = "next_action_reminder"
            )
        )
    }

    // ---------------------------------------------------------------------------
    // DISTRACTION_PREEMPTION
    // ---------------------------------------------------------------------------
    private fun evaluateDistractionPreemption(ctx: NotificationContext, prefs: NotificationPreferences): NotificationDecision {
        if (ctx.distractionDataPoints < MIN_DISTRACTION_DATA_POINTS) {
            return NotificationDecision.Suppress("Insufficient data (${ctx.distractionDataPoints} < $MIN_DISTRACTION_DATA_POINTS)")
        }
        val peakHour = ctx.distractionPeakHour ?: return NotificationDecision.Suppress("No distraction peak hour identified")
        if (ctx.isGoalComplete) {
            return NotificationDecision.Suppress("Goal already complete today")
        }
        // Only show within PEAK_APPROACH_WINDOW_HOURS before peak
        val hoursUntilPeak = ((peakHour - ctx.currentHour) + 24) % 24
        if (hoursUntilPeak > PEAK_APPROACH_WINDOW_HOURS || hoursUntilPeak < 0) {
            return NotificationDecision.Defer("Distraction peak not approaching yet (${hoursUntilPeak}h away)", 60)
        }
        if (alreadySentTodayOfType(NotificationType.DISTRACTION_PREEMPTION, ctx.notificationHistory)) {
            return NotificationDecision.Suppress("Already sent preemption today")
        }

        val score = computeBaseScore(ctx) + distractionRisk(ctx) + historicalRelevance(ctx)
        return decideFromScore(
            score = score,
            candidate = NotificationCandidate(
                type = NotificationType.DISTRACTION_PREEMPTION,
                title = "A heads-up",
                body = buildDistractionBody(ctx),
                deepLink = "digitaldiscipline://action/${ctx.nextActionId}",
                actionLabel = "DO IT NOW",
                actionDeepLink = "digitaldiscipline://action/${ctx.nextActionId}",
                goalId = "",
                actionId = ctx.nextActionId,
                reason = "distraction_preemption"
            )
        )
    }

    // ---------------------------------------------------------------------------
    // MISSED_ACTION
    // ---------------------------------------------------------------------------
    private fun evaluateMissedAction(ctx: NotificationContext, prefs: NotificationPreferences): NotificationDecision {
        // Only show late in day (17–21)
        if (ctx.currentHour < 17 || ctx.currentHour >= 21) {
            return NotificationDecision.Suppress("Outside missed-action window (hour=${ctx.currentHour})")
        }
        if (ctx.isGoalComplete) {
            return NotificationDecision.Suppress("Goal already complete today")
        }
        if (alreadySentTodayOfType(NotificationType.MISSED_ACTION, ctx.notificationHistory)) {
            return NotificationDecision.Suppress("Already sent missed-action reminder today")
        }

        val score = computeBaseScore(ctx) + actionUrgency(ctx)
        return decideFromScore(
            score = score,
            candidate = NotificationCandidate(
                type = NotificationType.MISSED_ACTION,
                title = "Still time today",
                body = "Your ${ctx.goalTitle} action isn't done yet. There's still time.",
                deepLink = "digitaldiscipline://action/${ctx.nextActionId}",
                actionLabel = "DO IT NOW",
                actionDeepLink = "digitaldiscipline://action/${ctx.nextActionId}",
                goalId = "",
                actionId = ctx.nextActionId,
                reason = "missed_action"
            )
        )
    }

    // ---------------------------------------------------------------------------
    // SUCCESS
    // ---------------------------------------------------------------------------
    private fun evaluateSuccess(ctx: NotificationContext, prefs: NotificationPreferences): NotificationDecision {
        if (!ctx.isGoalComplete) {
            return NotificationDecision.Suppress("Goal not yet complete")
        }
        if (alreadySentTodayOfType(NotificationType.SUCCESS, ctx.notificationHistory)) {
            return NotificationDecision.Suppress("Already sent success notification today")
        }

        return NotificationDecision.Show(
            NotificationCandidate(
                type = NotificationType.SUCCESS,
                title = "Today's action: done",
                body = buildSuccessBody(ctx),
                deepLink = "digitaldiscipline://today",
                actionLabel = "VIEW TODAY",
                actionDeepLink = "digitaldiscipline://today",
                goalId = "",
                actionId = "",
                reason = "goal_complete"
            )
        )
    }

    // ---------------------------------------------------------------------------
    // EVENING_REFLECTION
    // ---------------------------------------------------------------------------
    private fun evaluateEveningReflection(ctx: NotificationContext, prefs: NotificationPreferences): NotificationDecision {
        if (ctx.currentHour < 19 || ctx.currentHour >= 23) {
            return NotificationDecision.Suppress("Outside evening window (hour=${ctx.currentHour})")
        }
        if (ctx.reflectionCompletedToday) {
            return NotificationDecision.Suppress("Daily reflection already completed")
        }
        if (alreadySentTodayOfType(NotificationType.EVENING_REFLECTION, ctx.notificationHistory)) {
            return NotificationDecision.Suppress("Already sent evening reflection today")
        }

        return NotificationDecision.Show(
            NotificationCandidate(
                type = NotificationType.EVENING_REFLECTION,
                title = "Quick check-in",
                body = "How did today's plan work for you?",
                deepLink = "digitaldiscipline://today",
                actionLabel = "VIEW TODAY",
                actionDeepLink = "digitaldiscipline://today",
                goalId = "",
                actionId = "",
                reason = "evening_reflection"
            )
        )
    }

    // ---------------------------------------------------------------------------
    // WEEKLY_REVIEW
    // ---------------------------------------------------------------------------
    private fun evaluateWeeklyReview(ctx: NotificationContext, prefs: NotificationPreferences): NotificationDecision {
        if (!ctx.weeklyReviewDue) {
            return NotificationDecision.Suppress("Weekly review not yet due")
        }
        if (alreadySentThisWeekOfType(NotificationType.WEEKLY_REVIEW, ctx.notificationHistory)) {
            return NotificationDecision.Suppress("Weekly review notification already sent this week")
        }

        return NotificationDecision.Show(
            NotificationCandidate(
                type = NotificationType.WEEKLY_REVIEW,
                title = "Your weekly review is ready",
                body = "See how your plan performed this week.",
                deepLink = "digitaldiscipline://weekly-review",
                actionLabel = "VIEW REVIEW",
                actionDeepLink = "digitaldiscipline://weekly-review",
                goalId = "",
                actionId = "",
                reason = "weekly_review"
            )
        )
    }

    // ---------------------------------------------------------------------------
    // Score components
    // ---------------------------------------------------------------------------

    /** Urgency from incomplete action. Range 0–30. */
    private fun actionUrgency(ctx: NotificationContext): Int {
        if (ctx.isGoalComplete) return -50
        if (ctx.dailyTarget <= 0) return 0
        val fractionRemaining = (ctx.dailyTarget - ctx.completedToday).toFloat() / ctx.dailyTarget
        return (fractionRemaining * 30f).toInt().coerceIn(0, 30)
    }

    /** Risk from approaching distraction window. Range 0–20. */
    private fun distractionRisk(ctx: NotificationContext): Int {
        val peak = ctx.distractionPeakHour ?: return 0
        if (ctx.distractionDataPoints < MIN_DISTRACTION_DATA_POINTS) return 0
        val hoursUntilPeak = ((peak - ctx.currentHour) + 24) % 24
        return when {
            hoursUntilPeak == 0 -> 20
            hoursUntilPeak <= PEAK_APPROACH_WINDOW_HOURS -> 15
            else -> 0
        }
    }

    /** Goal and plan relevance. Range 0–10. */
    private fun goalRelevance(ctx: NotificationContext): Int {
        var score = 0
        if (ctx.hasActivePlan) score += 5
        if (ctx.behaviourMomentumScore < 50) score += 5  // low momentum = more relevant to nudge
        return score
    }

    /** Historical data quality. Range 0–10. */
    private fun historicalRelevance(ctx: NotificationContext): Int {
        return when {
            ctx.distractionDataPoints >= 10 -> 10
            ctx.distractionDataPoints >= MIN_DISTRACTION_DATA_POINTS -> 5
            else -> 0
        }
    }

    /** Penalty for recent notification. Range 0–30. */
    private fun recentNotificationPenalty(ctx: NotificationContext, prefs: NotificationPreferences): Int {
        val gapMs = TimeUnit.MINUTES.toMillis(prefs.minGapMinutes.toLong())
        val nowMs = System.currentTimeMillis()
        val recent = ctx.notificationHistory.any { (nowMs - it.timestampMs) < gapMs }
        return if (recent) 30 else 0
    }

    /** Penalty for recent dismissal of same type. Range 0–20. */
    private fun dismissalPenalty(type: NotificationType, ctx: NotificationContext): Int {
        val windowMs = TimeUnit.HOURS.toMillis(DISMISSAL_PENALTY_WINDOW_HOURS.toLong())
        val nowMs = System.currentTimeMillis()
        val recentlyDismissed = ctx.notificationHistory.any { record ->
            record.type == type.name && record.userDismissed && (nowMs - record.timestampMs) < windowMs
        }
        return if (recentlyDismissed) 20 else 0
    }

    /** Total base score for general gates. */
    private fun computeBaseScore(ctx: NotificationContext): Int {
        return goalRelevance(ctx) + actionUrgency(ctx) - recentNotificationPenalty(ctx, NotificationPreferences())
    }

    // ---------------------------------------------------------------------------
    // Decision gate
    // ---------------------------------------------------------------------------

    private fun decideFromScore(score: Int, candidate: NotificationCandidate): NotificationDecision {
        return if (score >= SHOW_THRESHOLD) {
            NotificationDecision.Show(candidate)
        } else {
            NotificationDecision.Suppress("Score $score below threshold $SHOW_THRESHOLD")
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun isTypeEnabled(type: NotificationType, prefs: NotificationPreferences): Boolean = when (type) {
        NotificationType.MORNING_INTENTION     -> prefs.enableDailyFocus
        NotificationType.NEXT_ACTION           -> prefs.enableActionReminders
        NotificationType.DISTRACTION_PREEMPTION -> prefs.enableDistractionWindow
        NotificationType.MISSED_ACTION         -> prefs.enableActionReminders
        NotificationType.SUCCESS               -> prefs.enableSuccess
        NotificationType.EVENING_REFLECTION    -> prefs.enableEveningReflection
        NotificationType.WEEKLY_REVIEW         -> prefs.enableWeeklyReview
    }

    private fun alreadySentTodayOfType(type: NotificationType, history: List<NotificationRecord>): Boolean {
        val todayStart = todayStartMs()
        return history.any { it.type == type.name && it.timestampMs >= todayStart }
    }

    private fun alreadySentThisWeekOfType(type: NotificationType, history: List<NotificationRecord>): Boolean {
        val weekStart = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return history.any { it.type == type.name && it.timestampMs >= weekStart }
    }

    private fun todayStartMs(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun buildMorningBody(ctx: NotificationContext): String {
        return if (ctx.nextActionTitle.isNotBlank()) {
            "Today's focus: ${ctx.nextActionTitle} before your first distraction."
        } else {
            "Today's focus: ${ctx.goalTitle}. Start small, stay consistent."
        }
    }

    private fun buildDistractionBody(ctx: NotificationContext): String {
        return if (ctx.nextActionTitle.isNotBlank()) {
            "You usually get pulled into distractions around now. Want to do your ${ctx.nextActionTitle} first?"
        } else {
            "Your peak distraction window is approaching. Do your goal action first."
        }
    }

    private fun buildSuccessBody(ctx: NotificationContext): String {
        val walletMin = ctx.walletBalanceSeconds / 60
        return if (walletMin > 0) {
            "You completed today's ${ctx.goalTitle} action. +${walletMin} min earned."
        } else {
            "You completed today's ${ctx.goalTitle} action. Nice work."
        }
    }
}

package com.digitaldiscipline.spike.notification

import android.content.Context
import androidx.work.*
import com.digitaldiscipline.spike.logging.EventLogger
import java.util.concurrent.TimeUnit

/**
 * Phase 4D-3 — Notification Scheduler
 *
 * Schedules battery-conscious WorkManager jobs for Smart Notifications.
 * Uses existing WorkManager infrastructure (same instance as SyncManager).
 *
 * Jobs scheduled:
 *   DAILY_NOTIFICATION_WORKER    — repeats every 24 hours
 *                                   Evaluates: MORNING_INTENTION, EVENING_REFLECTION, WEEKLY_REVIEW
 *   ACTION_REMINDER_WORKER       — repeats every 12 hours
 *                                   Evaluates: NEXT_ACTION, DISTRACTION_PREEMPTION, MISSED_ACTION
 *
 * No continuous polling. No foreground service. No wakelock.
 * No network constraints (all evaluation is local/offline).
 * Uses ExistingPeriodicWorkPolicy.KEEP to avoid redundant re-enqueue.
 *
 * Survives process death: WorkManager persists schedule to its own internal DB.
 * Survives reboot: BootCompletedReceiver calls initializeSchedules().
 */
object NotificationScheduler {

    const val DAILY_NOTIFICATION_TAG   = "smart_notification_daily"
    const val ACTION_REMINDER_TAG      = "smart_notification_action"

    /**
     * Initialise both notification schedules.
     * Safe to call multiple times — uses KEEP policy.
     */
    fun initializeSchedules(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Daily notification worker — every 24 hours, no network constraint
        val dailyRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .addTag(DAILY_NOTIFICATION_TAG)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DAILY_NOTIFICATION_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )

        // Action reminder worker — every 12 hours (mid-day evaluation)
        val actionRequest = PeriodicWorkRequestBuilder<ActionReminderWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 1,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .addTag(ACTION_REMINDER_TAG)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ACTION_REMINDER_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            actionRequest
        )

        EventLogger.log("NOTIFICATION", "system", "SCHEDULER_INITIALIZED",
            details = "DailyWorker(24h) + ActionReminderWorker(12h) enqueued")
    }

    /**
     * Cancel all scheduled notification workers.
     * Called if user disables all notifications in settings.
     */
    fun cancelAllSchedules(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(DAILY_NOTIFICATION_TAG)
        workManager.cancelAllWorkByTag(ACTION_REMINDER_TAG)
        EventLogger.log("NOTIFICATION", "system", "SCHEDULER_CANCELLED", details = "All notification workers cancelled")
    }
}

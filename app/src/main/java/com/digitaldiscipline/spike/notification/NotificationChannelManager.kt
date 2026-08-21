package com.digitaldiscipline.spike.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.ui.MainActivity

/**
 * Phase 4D-3 — Notification Channel Manager
 *
 * Creates Android notification channels and posts notifications safely.
 *
 * Channels:
 *   DIGITAL_DISCIPLINE_DAILY   — DEFAULT importance. Morning intention, success, evening reflection.
 *   DIGITAL_DISCIPLINE_ACTIONS — DEFAULT importance. Next action, distraction pre-emption, missed action.
 *   DIGITAL_DISCIPLINE_WEEKLY  — LOW importance.    Weekly review.
 *
 * POST_NOTIFICATIONS handling:
 *   - If permission is denied, notifications are silently skipped.
 *   - Self Mode enforcement continues normally.
 *   - TodayScreen continues normally.
 *   - Wallet continues normally.
 *   - Zero crashes.
 *   - No repeated permission nagging from this class.
 *
 * No HIGH importance channels. No aggressive sound/vibration.
 */
object NotificationChannelManager {

    const val CHANNEL_DAILY   = "digital_discipline_daily"
    const val CHANNEL_ACTIONS = "digital_discipline_actions"
    const val CHANNEL_WEEKLY  = "digital_discipline_weekly"

    private const val NOTIF_ID_MORNING    = 1001
    private const val NOTIF_ID_ACTION     = 1002
    private const val NOTIF_ID_PREEMPT    = 1003
    private const val NOTIF_ID_MISSED     = 1004
    private const val NOTIF_ID_SUCCESS    = 1005
    private const val NOTIF_ID_EVENING    = 1006
    private const val NOTIF_ID_WEEKLY     = 1007

    /**
     * Creates all notification channels. Safe to call multiple times (idempotent).
     * Must be called before any notifications are posted (recommended: Application.onCreate).
     */
    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val daily = NotificationChannel(
            CHANNEL_DAILY,
            "Daily Focus",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily intention, success, and evening reflection"
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val actions = NotificationChannel(
            CHANNEL_ACTIONS,
            "Action Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for pending daily actions"
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val weekly = NotificationChannel(
            CHANNEL_WEEKLY,
            "Weekly Review",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Weekly plan review prompt"
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        nm.createNotificationChannel(daily)
        nm.createNotificationChannel(actions)
        nm.createNotificationChannel(weekly)

        EventLogger.log("NOTIFICATION", "system", "CHANNELS_CREATED", details = "3 channels registered")
    }

    /**
     * Post a notification for the given candidate, if POST_NOTIFICATIONS permission is granted.
     * Returns true if posted, false if suppressed due to permission denial or other reason.
     */
    fun postNotification(context: Context, candidate: NotificationCandidate): Boolean {
        if (!hasPostPermission(context)) {
            EventLogger.log("NOTIFICATION", "system", "POST_PERMISSION_DENIED",
                details = "Notification skipped: POST_NOTIFICATIONS not granted")
            return false
        }

        val notifId = notifIdForType(candidate.type)
        val channelId = candidate.type.channelId

        // Deep-link tap intent
        val tapIntent = buildDeepLinkIntent(context, candidate.deepLink)
        val tapPendingIntent = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(candidate.title)
            .setContentText(candidate.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(candidate.body))
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Action button (e.g., "DO IT NOW")
        if (!candidate.actionLabel.isNullOrBlank() && !candidate.actionDeepLink.isNullOrBlank()) {
            val actionIntent = buildDeepLinkIntent(context, candidate.actionDeepLink)
            val actionPendingIntent = PendingIntent.getActivity(
                context, notifId + 100, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, candidate.actionLabel, actionPendingIntent)
        }

        // "Later" dismiss action — suppresses this type for governor cooldown
        val laterPendingIntent = PendingIntent.getBroadcast(
            context, notifId + 200,
            Intent("com.digitaldiscipline.spike.NOTIFICATION_DISMISS")
                .putExtra("notif_type", candidate.type.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, candidate.dismissLabel, laterPendingIntent)

        return try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            EventLogger.log("NOTIFICATION", "system", "NOTIFICATION_POSTED",
                details = "${candidate.type.name}: ${candidate.title}")
            true
        } catch (e: SecurityException) {
            EventLogger.log("NOTIFICATION", "system", "POST_SECURITY_EXCEPTION", details = e.message ?: "Unknown")
            false
        } catch (e: Exception) {
            EventLogger.log("NOTIFICATION", "system", "POST_EXCEPTION", details = e.message ?: "Unknown")
            false
        }
    }

    /** Returns true if POST_NOTIFICATIONS permission is granted (or API < 33 where it is automatic). */
    fun hasPostPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun notifIdForType(type: NotificationType): Int = when (type) {
        NotificationType.MORNING_INTENTION      -> NOTIF_ID_MORNING
        NotificationType.NEXT_ACTION            -> NOTIF_ID_ACTION
        NotificationType.DISTRACTION_PREEMPTION -> NOTIF_ID_PREEMPT
        NotificationType.MISSED_ACTION          -> NOTIF_ID_MISSED
        NotificationType.SUCCESS                -> NOTIF_ID_SUCCESS
        NotificationType.EVENING_REFLECTION     -> NOTIF_ID_EVENING
        NotificationType.WEEKLY_REVIEW          -> NOTIF_ID_WEEKLY
    }

    private fun buildDeepLinkIntent(context: Context, deepLink: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("deep_link", deepLink)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}

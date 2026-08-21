package com.digitaldiscipline.spike.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val Context.governorDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_governor")

/**
 * Phase 4D-3 — Notification Frequency Governor
 *
 * Enforces hard daily notification caps so that Self Mode notifications are never
 * noisy or manipulative. Runs in <1ms (in-memory after initial DataStore load).
 *
 * All counters are keyed by calendar date string (yyyy-MM-dd) and reset automatically
 * when the date changes. State is persisted in DataStore; survives process death and reboot.
 *
 * LIMITS (defaults, mapped from NotificationPreferences.frequencyMode):
 *   MAX_TOTAL_NOTIFICATIONS_PER_DAY          = 3  (BALANCED)
 *   MAX_BEHAVIOUR_REMINDERS_PER_DAY          = 2  (BALANCED)
 *   MAX_SAME_TYPE_PER_DAY                    = 1
 *   MAX_PREEMPTIVE_NOTIFICATIONS_PER_DAY     = 1
 *   MAX_SUCCESS_NOTIFICATIONS_PER_DAY        = 1
 *   MAX_MISSED_ACTION_NOTIFICATIONS_PER_DAY  = 1
 *   MIN_NOTIFICATION_GAP_MINUTES             = 120 (BALANCED)
 *
 * None of this logic runs on or near the enforcement path.
 */
class NotificationFrequencyGovernor(private val context: Context) {

    companion object {
        private val KEY_DATE           = stringPreferencesKey("gov_date")
        private val KEY_TOTAL          = intPreferencesKey("gov_total")
        private val KEY_BEHAVIOUR      = intPreferencesKey("gov_behaviour")
        private val KEY_SUCCESS        = intPreferencesKey("gov_success")
        private val KEY_PREEMPTIVE     = intPreferencesKey("gov_preemptive")
        private val KEY_MISSED         = intPreferencesKey("gov_missed")
        private val KEY_LAST_SENT_MS   = longPreferencesKey("gov_last_sent_ms")
        // per-type keys stored as "gov_type_<typeName>"
        private fun typeKey(type: NotificationType) = intPreferencesKey("gov_type_${type.name}")

        private const val MAX_SAME_TYPE_PER_DAY = 1
    }

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun todayString(): String = fmt.format(Date())

    /** Returns true if this notification type is permitted by the frequency governor. */
    suspend fun canSend(type: NotificationType, prefs: NotificationPreferences): Boolean {
        val state = loadState()
        val today = todayString()

        // Roll-over: if stored date differs from today, all counters reset
        if (state.dateString != today) return true

        // Minimum gap between any two notifications
        if (state.lastSentTimestampMs > 0L) {
            val elapsedMs = System.currentTimeMillis() - state.lastSentTimestampMs
            val gapMs = TimeUnit.MINUTES.toMillis(prefs.minGapMinutes.toLong())
            if (elapsedMs < gapMs) return false
        }

        // Total daily cap
        if (state.totalSentToday >= prefs.maxTotalPerDay) return false

        // Per-type cap
        val typeCount = state.perTypeSentToday[type.name] ?: 0
        if (typeCount >= MAX_SAME_TYPE_PER_DAY) return false

        // Behaviour-reminder cap (NEXT_ACTION + DISTRACTION_PREEMPTION + MISSED_ACTION)
        val isBehaviourReminder = type in listOf(
            NotificationType.NEXT_ACTION,
            NotificationType.DISTRACTION_PREEMPTION,
            NotificationType.MISSED_ACTION
        )
        if (isBehaviourReminder && state.behaviourRemindersSentToday >= prefs.maxBehaviourRemindersPerDay) return false

        // Dedicated caps
        if (type == NotificationType.SUCCESS && state.successSentToday >= 1) return false
        if (type == NotificationType.DISTRACTION_PREEMPTION && state.preemptiveSentToday >= 1) return false
        if (type == NotificationType.MISSED_ACTION && state.missedActionSentToday >= 1) return false

        return true
    }

    /** Record that a notification of the given type was just sent. */
    suspend fun recordSent(type: NotificationType) {
        val today = todayString()
        context.governorDataStore.edit { prefs ->
            val storedDate = prefs[KEY_DATE] ?: ""
            if (storedDate != today) {
                // New day — reset all counters
                prefs[KEY_DATE]      = today
                prefs[KEY_TOTAL]     = 1
                prefs[KEY_BEHAVIOUR] = 0
                prefs[KEY_SUCCESS]   = 0
                prefs[KEY_PREEMPTIVE] = 0
                prefs[KEY_MISSED]    = 0
                prefs[KEY_LAST_SENT_MS] = System.currentTimeMillis()
                NotificationType.values().forEach { t -> prefs[typeKey(t)] = 0 }
                prefs[typeKey(type)] = 1
            } else {
                prefs[KEY_TOTAL]     = (prefs[KEY_TOTAL] ?: 0) + 1
                prefs[KEY_LAST_SENT_MS] = System.currentTimeMillis()
                prefs[typeKey(type)] = (prefs[typeKey(type)] ?: 0) + 1

                val isBehaviourReminder = type in listOf(
                    NotificationType.NEXT_ACTION,
                    NotificationType.DISTRACTION_PREEMPTION,
                    NotificationType.MISSED_ACTION
                )
                if (isBehaviourReminder) prefs[KEY_BEHAVIOUR] = (prefs[KEY_BEHAVIOUR] ?: 0) + 1
                if (type == NotificationType.SUCCESS) prefs[KEY_SUCCESS] = (prefs[KEY_SUCCESS] ?: 0) + 1
                if (type == NotificationType.DISTRACTION_PREEMPTION) prefs[KEY_PREEMPTIVE] = (prefs[KEY_PREEMPTIVE] ?: 0) + 1
                if (type == NotificationType.MISSED_ACTION) prefs[KEY_MISSED] = (prefs[KEY_MISSED] ?: 0) + 1
            }
        }
    }

    /** Load current governor state from DataStore. */
    suspend fun loadState(): GovernorState {
        val prefs = context.governorDataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()

        val today = todayString()
        val storedDate = prefs[KEY_DATE] ?: ""
        if (storedDate != today) {
            return GovernorState(dateString = today) // All zeros — fresh day
        }
        return GovernorState(
            dateString = storedDate,
            totalSentToday = prefs[KEY_TOTAL] ?: 0,
            behaviourRemindersSentToday = prefs[KEY_BEHAVIOUR] ?: 0,
            successSentToday = prefs[KEY_SUCCESS] ?: 0,
            preemptiveSentToday = prefs[KEY_PREEMPTIVE] ?: 0,
            missedActionSentToday = prefs[KEY_MISSED] ?: 0,
            perTypeSentToday = NotificationType.values().associate { t ->
                t.name to (prefs[typeKey(t)] ?: 0)
            },
            lastSentTimestampMs = prefs[KEY_LAST_SENT_MS] ?: 0L
        )
    }

    /** Expose a Flow of the daily sent count for UI display. */
    val totalSentTodayFlow: Flow<Int> = context.governorDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val storedDate = prefs[KEY_DATE] ?: ""
            if (storedDate != todayString()) 0 else prefs[KEY_TOTAL] ?: 0
        }
}

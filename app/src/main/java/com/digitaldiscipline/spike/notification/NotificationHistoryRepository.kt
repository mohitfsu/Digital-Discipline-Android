package com.digitaldiscipline.spike.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Context.notificationHistoryDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "notification_history")

/**
 * Phase 4D-3 — Notification History Repository
 *
 * Persists a rolling window of notification records to DataStore as a JSON array.
 * No Room schema migration required. Retains up to [MAX_HISTORY_RECORDS] records
 * (oldest pruned first). Records older than [HISTORY_RETENTION_DAYS] are also pruned.
 *
 * Thread-safe via DataStore serialisation.
 *
 * PRIVACY: Only stores notification metadata (type, timestamp, interaction outcome).
 *          No screen content, URL, message, or personal data is stored.
 */
class NotificationHistoryRepository(private val context: Context) {

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("notification_history_json")
        private const val MAX_HISTORY_RECORDS = 100
        private const val HISTORY_RETENTION_DAYS = 30L
    }

    /** Load all notification records from DataStore. */
    suspend fun loadHistory(): List<NotificationRecord> {
        val prefs = context.notificationHistoryDataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()
        val json = prefs[KEY_HISTORY] ?: return emptyList()
        return parseRecords(json)
    }

    /** Append a new notification record and persist. Old records are pruned. */
    suspend fun appendRecord(record: NotificationRecord) {
        context.notificationHistoryDataStore.edit { prefs ->
            val existing = parseRecords(prefs[KEY_HISTORY] ?: "[]").toMutableList()
            existing.add(record)
            // Prune by age first
            val cutoffMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(HISTORY_RETENTION_DAYS)
            val pruned = existing.filter { it.timestampMs >= cutoffMs }
            // Prune by size
            val trimmed = if (pruned.size > MAX_HISTORY_RECORDS) {
                pruned.drop(pruned.size - MAX_HISTORY_RECORDS)
            } else pruned
            prefs[KEY_HISTORY] = serialiseRecords(trimmed)
        }
    }

    /** Update the most recent record of the given type with interaction/dismissal outcome. */
    suspend fun markInteracted(type: NotificationType, interacted: Boolean, dismissed: Boolean) {
        context.notificationHistoryDataStore.edit { prefs ->
            val records = parseRecords(prefs[KEY_HISTORY] ?: "[]").toMutableList()
            val idx = records.indexOfLast { it.type == type.name }
            if (idx >= 0) {
                records[idx] = records[idx].copy(userInteracted = interacted, userDismissed = dismissed)
                prefs[KEY_HISTORY] = serialiseRecords(records)
            }
        }
    }

    /** Mark the most recent record of the given type as having resulted in completion. */
    suspend fun markResultedInCompletion(type: NotificationType) {
        context.notificationHistoryDataStore.edit { prefs ->
            val records = parseRecords(prefs[KEY_HISTORY] ?: "[]").toMutableList()
            val idx = records.indexOfLast { it.type == type.name }
            if (idx >= 0) {
                records[idx] = records[idx].copy(resultedInCompletion = true)
                prefs[KEY_HISTORY] = serialiseRecords(records)
            }
        }
    }

    // ---------------------------------------------------------------------------
    // JSON serialisation (no Gson/Moshi dependency — plain JSON only)
    // ---------------------------------------------------------------------------

    private fun parseRecords(json: String): List<NotificationRecord> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                NotificationRecord(
                    type                 = obj.optString("type", ""),
                    timestampMs          = obj.optLong("timestampMs", 0L),
                    reason               = obj.optString("reason", ""),
                    goalId               = obj.optString("goalId", ""),
                    actionId             = obj.optString("actionId", ""),
                    userInteracted       = obj.optBoolean("userInteracted", false),
                    userDismissed        = obj.optBoolean("userDismissed", false),
                    resultedInCompletion = obj.optBoolean("resultedInCompletion", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serialiseRecords(records: List<NotificationRecord>): String {
        val arr = JSONArray()
        records.forEach { r ->
            val obj = JSONObject()
            obj.put("type",                 r.type)
            obj.put("timestampMs",          r.timestampMs)
            obj.put("reason",               r.reason)
            obj.put("goalId",               r.goalId)
            obj.put("actionId",             r.actionId)
            obj.put("userInteracted",       r.userInteracted)
            obj.put("userDismissed",        r.userDismissed)
            obj.put("resultedInCompletion", r.resultedInCompletion)
            arr.put(obj)
        }
        return arr.toString()
    }
}

package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.UUID

/**
 * Deterministic local behavioral intervention event model.
 * Records intervention lifecycle metadata strictly on-device for habit interruption analysis.
 *
 * PRIVACY GUARANTEE: Never collects screen contents, keystrokes, messages, URLs, or personal data.
 */
@Entity(tableName = "intervention_events")
data class InterventionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val deviceId: String = "",
    val childId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appDisplayName: String,
    val interventionType: String, // PAUSE, BREATHING, SQUATS, PARENT_OVERRIDE
    val status: String = "STARTED", // STARTED, COMPLETED, ABANDONED, EXITED
    val outcome: String = "STARTED", // COMPLETED, ABANDONED, EXITED, PARENT_OVERRIDE, EARNED_ACCESS, AUTO_EXPIRED, RAPID_REOPEN
    val durationSeconds: Int = 10,
    val earnedSeconds: Int = 0,
    val unlockStartedAt: Long = 0L,
    val unlockExpiredAt: Long = 0L,
    val reopenWithin1Minute: Boolean = false,
    val reopenWithin5Minutes: Boolean = false,
    val reopenWithin15Minutes: Boolean = false,
    val hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val dayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
    val latencyMs: Long = 0L
)

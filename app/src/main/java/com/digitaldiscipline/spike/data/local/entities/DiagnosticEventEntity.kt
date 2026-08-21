package com.digitaldiscipline.spike.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Structured local diagnostic event recording system transitions, permission checks,
 * sync operations, and enforcement lifecycles strictly on-device.
 *
 * PRIVACY GUARANTEE: Never transmitted to external analytics or cloud.
 */
@Entity(tableName = "diagnostic_events")
data class DiagnosticEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "package_name")
    val packageName: String? = null,

    @ColumnInfo(name = "policy_version")
    val policyVersion: Int = 1,

    @ColumnInfo(name = "details")
    val details: String? = null
)

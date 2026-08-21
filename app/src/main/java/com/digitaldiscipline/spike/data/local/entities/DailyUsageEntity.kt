package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily aggregated usage and behavioral habit summary entity.
 * Stores local rollups for daily analytics and idempotent single-write cloud sync.
 */
@Entity(
    tableName = "daily_usage",
    indices = [Index(value = ["dateString", "packageName"], unique = true)]
)
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // e.g. "2026-08-16"
    val packageName: String,
    val appDisplayName: String,
    val totalForegroundSeconds: Long = 0L,
    val openCount: Int = 0,
    val blockCount: Int = 0,
    val unlockCount: Int = 0,
    val attempts: Int = 0,
    val completed: Int = 0,
    val abandoned: Int = 0,
    val earnedAccess: Int = 0,
    val parentOverrides: Int = 0,
    val rapidReopens: Int = 0,
    val habitInterruptionRate: Float = 100.0f,
    val earnedMinutes: Int = 0,
    val pauseCount: Int = 0,
    val breathingCount: Int = 0,
    val squatsCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protection_state")
data class ProtectionStateEntity(
    @PrimaryKey val id: Int = 1, // Single-row singleton state
    val isAccessibilityActive: Boolean = false,
    val isOverlayActive: Boolean = false,
    val isUsageStatsActive: Boolean = false,
    val isProtectionEnabledByParent: Boolean = true,
    val lastHeartbeatElapsedRealtime: Long = 0L,
    val lastTamperCheckTimestamp: Long = System.currentTimeMillis()
)

package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RuleMode {
    BLOCK,     // Immediate block with intervention challenge
    DELAY,     // Mindful pause before access
    EARN,      // Physical challenge required
    ALLOW      // Unrestricted
}

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appDisplayName: String,
    val mode: RuleMode = RuleMode.BLOCK,
    val isEnabled: Boolean = true,
    val dailyLimitMinutes: Int = 0, // 0 = unlimited or strict block
    val unlockDurationSeconds: Int = 600, // 10 minutes default
    val interventionType: String = "PAUSE", // PAUSE, BREATHING, SQUATS, PARENT_OVERRIDE
    val pauseDurationSeconds: Int = 10,
    val breathingDurationSeconds: Int = 30,
    val squatsTargetCount: Int = 10,
    val updatedAt: Long = System.currentTimeMillis()
)

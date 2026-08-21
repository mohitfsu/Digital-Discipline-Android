package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intervention_rules")
data class InterventionRuleEntity(
    @PrimaryKey val interventionType: String, // PAUSE, BREATHING, SQUATS, PARENT_OVERRIDE
    val displayName: String,
    val description: String,
    val durationSeconds: Int = 10,
    val targetReps: Int = 10,
    val isEnabled: Boolean = true
)

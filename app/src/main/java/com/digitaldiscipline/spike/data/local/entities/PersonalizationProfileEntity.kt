package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personalization_profiles")
data class PersonalizationProfileEntity(
    @PrimaryKey
    val profileId: String = "profile_self",
    val preferredIntervention: String = "SQUATS",
    val peakStartHour: Int = 20, // 8 PM
    val peakEndHour: Int = 22,   // 10 PM
    val challengeCompletionRate: Float = 0.0f,
    val rapidReopenRate: Float = 0.0f,
    val averageSessionDurationSeconds: Int = 0,
    val rewardEffectiveness: String = "BALANCED",
    val consistencyScore: Float = 0.0f,
    val currentPlanHealth: String = "INSUFFICIENT_DATA",
    val lastCalculatedAt: Long = System.currentTimeMillis()
)

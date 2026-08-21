package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lightweight persistent goal progress entity.
 * Stores daily goal metrics locally without expensive continuous event streams.
 */
@Entity(
    tableName = "goal_progress",
    indices = [
        Index(value = ["goalId", "dateString"], unique = true)
    ]
)
data class GoalProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: String,
    val dateString: String, // e.g. "2026-08-16"
    val completedCount: Int = 0,
    val targetCount: Int = 1,
    val completedDurationSeconds: Int = 0,
    val targetDurationSeconds: Int = 0,
    val completionPercentage: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
)

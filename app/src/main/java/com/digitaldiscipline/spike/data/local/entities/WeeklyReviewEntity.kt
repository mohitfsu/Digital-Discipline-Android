package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "weekly_reviews",
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["weekStart"])
    ]
)
data class WeeklyReviewEntity(
    @PrimaryKey
    val reviewId: String = UUID.randomUUID().toString(),
    val goalId: String = "",
    val weekStart: Long = 0L,
    val weekEnd: Long = 0L,
    val attempts: Int = 0,
    val completed: Int = 0,
    val earnedSeconds: Int = 0,
    val consumedSeconds: Int = 0,
    val habitInterruptionRate: Float = 0.0f,
    val rapidReopenRate: Float = 0.0f,
    val bestIntervention: String = "Squats",
    val planHealth: String = "WORKING",
    val biggestWin: String = "",
    val suggestedNextStep: String = "",
    val generatedAt: Long = System.currentTimeMillis()
)

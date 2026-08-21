package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class GoalCategory {
    FITNESS,
    STUDY,
    PRODUCTIVITY,
    SLEEP,
    MINDFULNESS,
    READING,
    HEALTH,
    FINANCE,
    CUSTOM
}

enum class UserMode {
    PARENT,
    SELF
}

/**
 * Persistent Goal entity.
 * Represents an individual's or parent's high-level positive behavioral objective.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val goalId: String = UUID.randomUUID().toString(),
    val ownerId: String = "self",
    val mode: String = UserMode.SELF.name, // PARENT, SELF
    val title: String,
    val description: String = "",
    val category: String = GoalCategory.FITNESS.name,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val startDate: Long = System.currentTimeMillis(),
    val targetDate: Long? = null,
    val dailyTarget: Int = 1,
    val weeklyTarget: Int = 7,
    val progress: Int = 0,
    val unit: String = "reps", // reps, minutes, pages, sessions
    val priority: Int = 1
)

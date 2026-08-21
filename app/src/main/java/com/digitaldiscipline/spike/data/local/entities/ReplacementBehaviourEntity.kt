package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class BehaviourCategory {
    PHYSICAL,
    MINDFUL,
    STUDY,
    HEALTH,
    PRODUCTIVITY,
    CUSTOM
}

enum class BehaviourType {
    // PHYSICAL
    PUSHUPS,
    SQUATS,
    JUMPING_JACKS,
    PLANK,
    WALK,
    STRETCH,

    // MINDFUL
    BOX_BREATHING,
    MINDFUL_PAUSE,
    MEDITATION,
    REFLECTION,

    // STUDY
    STUDY_TIMER,
    READ_PAGES,

    // HEALTH
    DRINK_WATER,
    WALK_OUTSIDE,
    HEALTHY_SNACK,

    // PRODUCTIVITY
    COMPLETE_TASK,
    CLEAN_DESK,
    FOCUS_TIMER,

    // CUSTOM
    CUSTOM
}

/**
 * Reusable positive friction replacement behaviour entity.
 */
@Entity(tableName = "replacement_behaviours")
data class ReplacementBehaviourEntity(
    @PrimaryKey
    val behaviourId: String = UUID.randomUUID().toString(),
    val category: String = BehaviourCategory.PHYSICAL.name,
    val type: String = BehaviourType.SQUATS.name,
    val title: String,
    val description: String = "",
    val targetCount: Int = 10,       // e.g. 10 squats, 2 pages
    val durationSeconds: Int = 30,   // e.g. 30s breathing, 60s squats
    val unit: String = "reps",       // reps, seconds, pages, minutes
    val configJson: String = "{}"
)

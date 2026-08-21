package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TriggerCategory {
    SOCIAL_MEDIA,
    VIDEO_STREAMING,
    GAMING,
    SHOPPING,
    FOOD_DELIVERY,
    CUSTOM
}

/**
 * Trigger entity.
 * Represents a distracting application or habit trigger associated with a Goal.
 */
@Entity(
    tableName = "triggers",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["goalId"])
    ]
)
data class TriggerEntity(
    @PrimaryKey
    val triggerId: String = UUID.randomUUID().toString(),
    val ownerId: String = "self",
    val goalId: String = "",
    val packageName: String,
    val appDisplayName: String = "",
    val category: String = TriggerCategory.SOCIAL_MEDIA.name,
    val active: Boolean = true,
    val startHour: Int = 0,       // 0..23
    val startMinute: Int = 0,     // 0..59
    val endHour: Int = 23,        // 0..23
    val endMinute: Int = 59,      // 0..59
    val daysOfWeek: String = "1,2,3,4,5,6,7", // Comma-separated (1=Sun..7=Sat)
    val priority: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

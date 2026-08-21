package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "Schedule", // e.g. "Office Hours", "Study Time", "Custom Focus"
    val packageName: String = "ALL_RESTRICTED", // Or specific package e.g. "com.instagram.android"
    val dayOfWeek: Int = 2, // 1 = Sunday, 7 = Saturday (Calendar.DAY_OF_WEEK)
    val startHour: Int = 9, // 0-23
    val startMinute: Int = 0, // 0-59
    val endHour: Int = 17, // 0-23
    val endMinute: Int = 0, // 0-59
    val isBlocked: Boolean = true,
    val restrictionMode: String = "BLOCK", // "BLOCK" (strict admin pin) or "INTERVENE" (30s physical/mindful reset)
    val isEnabled: Boolean = true,
    val daysOfWeekCsv: String = "" // e.g. "2,3,4,5,6" (Mon-Fri)
)

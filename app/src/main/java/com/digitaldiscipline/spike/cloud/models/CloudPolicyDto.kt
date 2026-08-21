package com.digitaldiscipline.spike.cloud.models

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class CloudPolicyDto(
    @DocumentId
    val policyId: String = "", // childId
    val version: Int = 1,
    val updatedBy: String = "",
    @ServerTimestamp
    val updatedAt: Date? = null,
    val pauseDurationSeconds: Int = 10,
    val breathingDurationSeconds: Int = 30,
    val squatsTargetCount: Int = 10,
    val rules: List<CloudAppRuleDto> = emptyList(),
    val schedules: List<CloudScheduleDto> = emptyList()
)

@Keep
data class CloudAppRuleDto(
    val packageName: String = "",
    val appDisplayName: String = "",
    val mode: String = "EARN", // "BLOCK", "DELAY", "EARN", "ALLOW"
    val isEnabled: Boolean = true,
    val dailyLimitMinutes: Int = 0,
    val unlockDurationSeconds: Int = 600,
    val interventionType: String = "PAUSE",
    val pauseDurationSeconds: Int = 10,
    val breathingDurationSeconds: Int = 30,
    val squatsTargetCount: Int = 10
)

@Keep
data class CloudScheduleDto(
    val id: String = "",
    val label: String = "Schedule",
    val packageName: String = "ALL_RESTRICTED",
    val dayOfWeek: Int = 1, // 1=Sun .. 7=Sat
    val daysOfWeekCsv: String = "", // e.g. "2,3,4,5,6"
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0,
    val isBlocked: Boolean = true,
    val restrictionMode: String = "BLOCK",
    val isEnabled: Boolean = true
)

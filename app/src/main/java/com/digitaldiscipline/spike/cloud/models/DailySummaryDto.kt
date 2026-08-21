package com.digitaldiscipline.spike.cloud.models

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class DailySummaryDto(
    @DocumentId
    val summaryId: String = "", // e.g. "childId_2026-08-15"
    val familyId: String = "",
    val childId: String = "",
    val deviceId: String = "",
    val dateString: String = "", // "2026-08-15"
    val totalScreenTimeMinutes: Int = 0,
    val totalInterventionsCompleted: Int = 0,
    val totalBlocksTriggered: Int = 0,
    val totalAttempts: Int = 0,
    val totalEarnedMinutes: Int = 0,
    val habitInterruptionRate: Float = 100.0f,
    val pauseCount: Int = 0,
    val breathingCount: Int = 0,
    val squatsCount: Int = 0,
    val topApps: List<DailyAppUsageDto> = emptyList(),
    @ServerTimestamp
    val uploadedAt: Date? = null
)

@Keep
data class DailyAppUsageDto(
    val packageName: String = "",
    val appDisplayName: String = "",
    val usageMinutes: Int = 0,
    val openCount: Int = 0,
    val blockCount: Int = 0,
    val unlockCount: Int = 0,
    val attempts: Int = 0,
    val earnedMinutes: Int = 0,
    val habitInterruptionRate: Float = 100.0f
)

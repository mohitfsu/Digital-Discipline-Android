package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AdjustmentStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}

@Entity(
    tableName = "plan_adjustments",
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["status"])
    ]
)
data class PlanAdjustmentEntity(
    @PrimaryKey
    val adjustmentId: String = UUID.randomUUID().toString(),
    val goalId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val reason: String = "",
    val recommendationType: String = "KEEP_PLAN",
    val currentConfiguration: String = "{}",
    val suggestedConfiguration: String = "{}",
    val status: String = AdjustmentStatus.PENDING.name,
    val appliedAt: Long = 0L,
    val rejectedAt: Long = 0L,
    val cooldownSeconds: Int = 0 // 0, 60, 120, 300
)

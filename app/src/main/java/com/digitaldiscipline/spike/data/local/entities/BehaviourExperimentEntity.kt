package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ExperimentStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

@Entity(
    tableName = "behaviour_experiments",
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["status"]),
        Index(value = ["experimentStartDate"])
    ]
)
data class BehaviourExperimentEntity(
    @PrimaryKey
    val experimentId: String = UUID.randomUUID().toString(),
    val goalId: String = "",
    val title: String = "",
    val hypothesis: String = "",
    val baselineStartDate: Long = 0L,
    val baselineEndDate: Long = 0L,
    val experimentStartDate: Long = 0L,
    val experimentEndDate: Long = 0L,
    val interventionConfiguration: String = "{}",
    val status: String = ExperimentStatus.DRAFT.name,
    val baselineMetrics: String = "{}",
    val experimentMetrics: String = "{}",
    val conclusion: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L
)

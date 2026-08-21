package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class RewardType {
    EARNED_SCREEN_TIME,
    NO_REWARD,
    COMPLETION_ONLY
}

/**
 * Behaviour Policy Entity.
 * Connects Goal + Trigger + Replacement Behaviour + Reward.
 * Sits above the existing enforcement engine.
 */
@Entity(
    tableName = "behaviour_policies",
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["triggerId"]),
        Index(value = ["replacementBehaviourId"])
    ]
)
data class BehaviourPolicyEntity(
    @PrimaryKey
    val policyId: String = UUID.randomUUID().toString(),
    val ownerId: String = "self",
    val goalId: String,
    val triggerId: String,
    val replacementBehaviourId: String,
    val interventionMode: String = RuleMode.EARN.name, // EARN, DELAY, BLOCK, ALLOW
    val rewardType: String = RewardType.EARNED_SCREEN_TIME.name,
    val earnedSeconds: Int = 600, // Default 10 minutes reward
    val maximumDailySeconds: Int = 3600,
    val maximumSessionSeconds: Int = 900,
    val enabled: Boolean = true,
    val priority: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

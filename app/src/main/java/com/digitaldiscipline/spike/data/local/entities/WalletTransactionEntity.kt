package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class WalletTransactionType {
    EARN,
    SPEND,
    EXPIRE,
    ADJUSTMENT,
    RESET
}

@Entity(
    tableName = "wallet_transactions",
    indices = [
        Index(value = ["walletId"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class WalletTransactionEntity(
    @PrimaryKey val transactionId: String,
    val walletId: String = "wallet_self",
    val type: String, // EARN, SPEND, EXPIRE, ADJUSTMENT, RESET
    val amountSeconds: Int,
    val balanceAfterSeconds: Int,
    val source: String, // SQUATS, MINDFUL_PAUSE, BOX_BREATHING, STUDY_TIMER, APP_SESSION, MANUAL_RESET
    val triggerPackage: String? = null,
    val idempotencyKey: String? = null,
    val sessionId: String? = null,
    val goalId: String? = null,
    val timestampWallClock: Long = System.currentTimeMillis(),
    val elapsedRealtime: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

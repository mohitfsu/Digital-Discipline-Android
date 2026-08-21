package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class WalletSessionStatus {
    ACTIVE,
    EXPIRED,
    ENDED,
    INTERRUPTED,
    INVALIDATED
}

@Entity(
    tableName = "wallet_sessions",
    indices = [
        Index(value = ["walletId"]),
        Index(value = ["status"])
    ]
)
data class WalletSessionEntity(
    @PrimaryKey val sessionId: String,
    val walletId: String = "wallet_self",
    val triggerPackage: String,
    val startedElapsedRealtime: Long,
    val lastHeartbeatElapsedRealtime: Long,
    val startedWallClock: Long = System.currentTimeMillis(),
    val initialWalletSeconds: Int,
    val consumedSeconds: Int = 0,
    val maxAllowedSeconds: Int = 1800,
    val status: String = WalletSessionStatus.ACTIVE.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

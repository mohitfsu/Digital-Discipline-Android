package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "earned_time_wallets")
data class EarnedTimeWalletEntity(
    @PrimaryKey val walletId: String = "wallet_self",
    val ownerId: String = "self",
    val mode: String = "SELF",
    val availableSeconds: Int = 0,
    val lifetimeEarnedSeconds: Int = 0,
    val lifetimeConsumedSeconds: Int = 0,
    val dailyEarnedSeconds: Int = 0,
    val dailyConsumedSeconds: Int = 0,
    val dailyEarnCapSeconds: Int = 3600, // 60 mins default daily earn cap
    val maxBalanceCapSeconds: Int = 3600, // 60 mins default max balance
    val maxSessionSeconds: Int = 1800, // 30 mins max single session duration
    val lastDateString: String = "",
    val lastUpdatedElapsedRealtime: Long = 0L,
    val lastUpdatedWallClock: Long = System.currentTimeMillis(),
    val walletVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

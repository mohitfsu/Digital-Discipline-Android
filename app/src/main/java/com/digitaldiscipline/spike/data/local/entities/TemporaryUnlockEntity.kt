package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temporary_unlocks")
data class TemporaryUnlockEntity(
    @PrimaryKey val packageName: String,
    val unlockGrantedElapsedRealtime: Long,
    val unlockExpiryElapsedRealtime: Long,
    val unlockDurationMs: Long,
    val reason: String = "INTERVENTION_COMPLETED",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isStillValid(
        currentElapsedRealtime: Long,
        currentWallTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        // Reboot check: If current elapsed is less than when granted, device was rebooted -> invalid
        if (currentElapsedRealtime < unlockGrantedElapsedRealtime) return false

        // Monotonic duration check
        if (currentElapsedRealtime >= unlockExpiryElapsedRealtime) return false

        // Wall-clock bounds check (with 10s grace for sleep)
        if (currentWallTimeMs > createdAt + unlockDurationMs + 10_000L) return false

        return true
    }

    fun remainingSeconds(
        currentElapsedRealtime: Long,
        currentWallTimeMs: Long = System.currentTimeMillis()
    ): Long {
        if (!isStillValid(currentElapsedRealtime, currentWallTimeMs)) return 0L
        return ((unlockExpiryElapsedRealtime - currentElapsedRealtime) / 1000L).coerceAtLeast(0L)
    }
}

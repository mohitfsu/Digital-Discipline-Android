package com.digitaldiscipline.spike.policy

enum class PolicyState {
    BLOCKED,
    INTERVENTION_ACTIVE,
    UNLOCKED_TEMPORARY,
    ALLOWED
}

data class RestrictionRule(
    val packageName: String,
    val appDisplayName: String,
    val isRestricted: Boolean = true,
    val unlockDurationSeconds: Int = 60,
    var currentState: PolicyState = PolicyState.BLOCKED,
    var unlockExpiryElapsedRealtime: Long = 0L
) {
    fun isTemporarilyUnlocked(currentElapsedRealtime: Long): Boolean {
        return currentState == PolicyState.UNLOCKED_TEMPORARY && currentElapsedRealtime < unlockExpiryElapsedRealtime
    }

    fun remainingUnlockSeconds(currentElapsedRealtime: Long): Long {
        if (!isTemporarilyUnlocked(currentElapsedRealtime)) return 0L
        return ((unlockExpiryElapsedRealtime - currentElapsedRealtime) / 1000L).coerceAtLeast(0L)
    }
}

package com.digitaldiscipline.spike.behaviour.firstwin

/**
 * Phase 4E-2 — First-Win State Machine Lifecycle
 *
 * Deterministic states guiding the newly activated Self Mode user
 * through their first successful behaviour intervention.
 */
enum class FirstWinState {
    NOT_STARTED,
    PLAN_ACTIVE,
    FIRST_TRIGGER_SEEN,
    REFLECTION_COMPLETED,
    INTERVENTION_STARTED,
    INTERVENTION_COMPLETED,
    TIME_EARNED,
    TIME_USED,
    TIME_SAVED,
    FIRST_WIN_COMPLETED
}

data class FirstWinSnapshot(
    val state: FirstWinState = FirstWinState.NOT_STARTED,
    val planId: String? = null,
    val completedAt: Long = 0L,
    val earnedSeconds: Int = 0,
    val usedSeconds: Int = 0,
    val savedSeconds: Int = 0,
    val actionTitle: String? = null
) {
    val isCompleted: Boolean
        get() = state == FirstWinState.FIRST_WIN_COMPLETED || state == FirstWinState.TIME_USED || state == FirstWinState.TIME_SAVED
}

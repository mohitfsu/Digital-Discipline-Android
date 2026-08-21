package com.digitaldiscipline.spike.behaviour.firstwin

import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Phase 4E-2 — First-Win State Manager
 *
 * Coordinates the deterministic lifecycle of the Self Mode First-Win.
 * Idempotent, mutex-protected, and strictly off the real-time enforcement critical path.
 */
object FirstWinStateManager {

    private val mutex = Mutex()

    /**
     * Transition to PLAN_ACTIVE when a new Self Mode plan is created and confirmed.
     */
    suspend fun onPlanActivated(
        planId: String,
        preferencesManager: PreferencesManager
    ) = mutex.withLock {
        preferencesManager.setFirstWinState(
            state = FirstWinState.PLAN_ACTIVE.name,
            planId = planId
        )
        EventLogger.log(
            source = "FIRST_WIN",
            packageName = "system",
            eventType = "FIRST_WIN_PLAN_ACTIVATED",
            details = "Plan: $planId"
        )
    }

    /**
     * Record encounter with the first configured distraction trigger.
     */
    suspend fun onTriggerSeen(
        planId: String,
        packageName: String,
        currentState: FirstWinState,
        preferencesManager: PreferencesManager
    ) = mutex.withLock {
        if (currentState == FirstWinState.PLAN_ACTIVE) {
            preferencesManager.setFirstWinState(
                state = FirstWinState.FIRST_TRIGGER_SEEN.name,
                planId = planId
            )
            EventLogger.log(
                source = "FIRST_WIN",
                packageName = packageName,
                eventType = "FIRST_WIN_TRIGGER_SEEN",
                details = "App: $packageName"
            )
        }
    }

    /**
     * Record reflection completion during the intervention pause.
     */
    suspend fun onReflectionCompleted(
        planId: String,
        currentState: FirstWinState,
        preferencesManager: PreferencesManager
    ) = mutex.withLock {
        if (currentState == FirstWinState.FIRST_TRIGGER_SEEN || currentState == FirstWinState.PLAN_ACTIVE) {
            preferencesManager.setFirstWinState(
                state = FirstWinState.REFLECTION_COMPLETED.name,
                planId = planId
            )
        }
    }

    /**
     * Record start of the replacement behaviour challenge.
     */
    suspend fun onInterventionStarted(
        planId: String,
        behaviourId: String,
        preferencesManager: PreferencesManager
    ) = mutex.withLock {
        preferencesManager.setFirstWinState(
            state = FirstWinState.INTERVENTION_STARTED.name,
            planId = planId
        )
    }

    /**
     * Record successful completion of the replacement behaviour and deposited earnings.
     */
    suspend fun onInterventionCompleted(
        planId: String,
        earnedSeconds: Int,
        actionTitle: String,
        preferencesManager: PreferencesManager
    ) = mutex.withLock {
        preferencesManager.setFirstWinState(
            state = FirstWinState.TIME_EARNED.name,
            planId = planId,
            earnedSeconds = earnedSeconds,
            actionTitle = actionTitle
        )
    }

    /**
     * Record user choice: USE MY TIME.
     */
    suspend fun onTimeUsed(
        planId: String,
        usedSeconds: Int,
        preferencesManager: PreferencesManager
    ) = mutex.withLock {
        val now = System.currentTimeMillis()
        preferencesManager.setFirstWinState(
            state = FirstWinState.FIRST_WIN_COMPLETED.name,
            planId = planId,
            completedAt = now,
            usedSeconds = usedSeconds
        )
        EventLogger.log(
            source = "FIRST_WIN",
            packageName = "system",
            eventType = "FIRST_WIN_COMPLETED_USED",
            details = "Used: ${usedSeconds}s | Plan: $planId"
        )
    }

    /**
     * Record user choice: SAVE FOR LATER.
     */
    suspend fun onTimeSaved(
        planId: String,
        savedSeconds: Int,
        preferencesManager: PreferencesManager
    ) = mutex.withLock {
        val now = System.currentTimeMillis()
        preferencesManager.setFirstWinState(
            state = FirstWinState.FIRST_WIN_COMPLETED.name,
            planId = planId,
            completedAt = now,
            savedSeconds = savedSeconds
        )
        EventLogger.log(
            source = "FIRST_WIN",
            packageName = "system",
            eventType = "FIRST_WIN_COMPLETED_SAVED",
            details = "Saved: ${savedSeconds}s | Plan: $planId"
        )
    }

    /**
     * Determine whether First Win reminder notification is eligible.
     */
    fun isNotificationEligible(
        snapshot: FirstWinSnapshot,
        isSelfMode: Boolean,
        isParentMode: Boolean
    ): Boolean {
        if (isParentMode || !isSelfMode) return false
        if (snapshot.isCompleted) return false
        return snapshot.state == FirstWinState.PLAN_ACTIVE || snapshot.state == FirstWinState.FIRST_TRIGGER_SEEN
    }
}

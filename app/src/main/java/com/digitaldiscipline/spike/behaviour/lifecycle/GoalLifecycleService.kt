package com.digitaldiscipline.spike.behaviour.lifecycle

import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.templates.BehaviourPlanCreator
import com.digitaldiscipline.spike.behaviour.templates.BehaviourPlanDraft
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService

/**
 * Phase 4E-5 — Goal Lifecycle Service
 *
 * Single authoritative coordinator for atomically applying user-approved
 * lifecycle transitions across Room entities and PreferencesManager DataStore.
 */
class GoalLifecycleService(
    private val behaviourRepository: BehaviourRepository,
    private val preferencesManager: PreferencesManager,
    private val walletService: EarnedTimeWalletService
) {

    /**
     * Pauses the primary active goal, silencing positive friction triggers
     * and goal-specific notifications while preserving all progress and wallet ledgers.
     */
    suspend fun pauseActiveGoal(goalId: String) {
        val policies = behaviourRepository.getPoliciesForGoal(goalId)
        policies.forEach { policy ->
            val pausedPolicy = policy.copy(enabled = false)
            behaviourRepository.savePolicy(pausedPolicy)
        }

        val goal = behaviourRepository.getGoalById(goalId)
        if (goal != null) {
            val pausedGoal = goal.copy(updatedAt = System.currentTimeMillis())
            behaviourRepository.saveGoal(pausedGoal)
        }

        preferencesManager.setPrimaryGoalLifecycleState(GoalLifecycleState.PAUSED.name)
        preferencesManager.setPrimaryGoalPausedAt(System.currentTimeMillis())
    }

    /**
     * Resumes a paused goal, restoring active friction policies and focus notifications.
     */
    suspend fun resumeGoal(goalId: String) {
        val policies = behaviourRepository.getPoliciesForGoal(goalId)
        policies.forEach { policy ->
            val resumedPolicy = policy.copy(enabled = true)
            behaviourRepository.savePolicy(resumedPolicy)
        }

        val goal = behaviourRepository.getGoalById(goalId)
        if (goal != null) {
            val activeGoal = goal.copy(active = true, updatedAt = System.currentTimeMillis())
            behaviourRepository.saveGoal(activeGoal)
        }

        preferencesManager.setPrimaryGoalLifecycleState(GoalLifecycleState.ACTIVE.name)
    }

    /**
     * Marks the primary goal as completed, archiving its active friction
     * while permanently keeping all historical records in Goal History.
     */
    suspend fun completeActiveGoal(goalId: String) {
        val policies = behaviourRepository.getPoliciesForGoal(goalId)
        policies.forEach { policy ->
            val disabledPolicy = policy.copy(enabled = false)
            behaviourRepository.savePolicy(disabledPolicy)
        }

        val goal = behaviourRepository.getGoalById(goalId)
        if (goal != null) {
            val completedGoal = goal.copy(active = false, updatedAt = System.currentTimeMillis())
            behaviourRepository.saveGoal(completedGoal)
        }

        preferencesManager.setPrimaryGoalLifecycleState(GoalLifecycleState.COMPLETED.name)
        preferencesManager.setPrimaryGoalCompletedAt(System.currentTimeMillis())
    }

    /**
     * Replaces the old primary goal with a new confirmed goal draft.
     * Archives old policies/goal and commits the new plan atomically.
     */
    suspend fun replaceGoal(oldGoalId: String, newPlanDraft: BehaviourPlanDraft) {
        // 1. Archive previous goal
        val oldPolicies = behaviourRepository.getPoliciesForGoal(oldGoalId)
        oldPolicies.forEach { policy ->
            val disabled = policy.copy(enabled = false)
            behaviourRepository.savePolicy(disabled)
        }

        val oldGoal = behaviourRepository.getGoalById(oldGoalId)
        if (oldGoal != null) {
            val archivedOld = oldGoal.copy(active = false, updatedAt = System.currentTimeMillis())
            behaviourRepository.saveGoal(archivedOld)
        }

        // 2. Commit new plan draft
        BehaviourPlanCreator.confirmAndPersistPlan(
            draft = newPlanDraft,
            behaviourRepository = behaviourRepository,
            walletService = walletService,
            preferencesManager = preferencesManager
        )

        // 3. Reset continuity and week counters for new goal
        preferencesManager.setPrimaryGoalLifecycleState(GoalLifecycleState.ACTIVE.name)
        preferencesManager.setPlanActiveWeekNumber(1)
        preferencesManager.setPlanContinuityState("LEARNING")
    }

    /**
     * Starts fresh with the current goal, resetting the active week counter
     * without deleting any historical telemetry or wallet ledger balances.
     */
    suspend fun startFreshGoal(goalId: String) {
        val goal = behaviourRepository.getGoalById(goalId)
        if (goal != null) {
            val refreshed = goal.copy(
                active = true,
                startDate = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            behaviourRepository.saveGoal(refreshed)
        }

        preferencesManager.setPrimaryGoalLifecycleState(GoalLifecycleState.ACTIVE.name)
        preferencesManager.setPlanActiveWeekNumber(1)
        preferencesManager.setPlanContinuityState("LEARNING")
    }
}

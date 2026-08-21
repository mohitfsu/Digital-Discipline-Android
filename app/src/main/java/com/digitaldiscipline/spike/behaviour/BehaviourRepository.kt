package com.digitaldiscipline.spike.behaviour

import com.digitaldiscipline.spike.data.local.dao.*
import com.digitaldiscipline.spike.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for managing behavioral change goals, triggers, replacement behaviours,
 * and behavioural policies.
 *
 * All operations are 100% local, fast (<3ms), and offline-first.
 */
class BehaviourRepository(
    private val goalDao: GoalDao,
    private val triggerDao: TriggerDao,
    private val replacementBehaviourDao: ReplacementBehaviourDao,
    private val behaviourPolicyDao: BehaviourPolicyDao,
    private val goalProgressDao: GoalProgressDao
) {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayString: String
        get() = sdf.format(Date())

    // 1. Goals
    fun getAllGoalsFlow(): Flow<List<GoalEntity>> = goalDao.getAllGoalsFlow()
    suspend fun getActiveGoals(): List<GoalEntity> = goalDao.getActiveGoals()
    suspend fun getGoalById(goalId: String): GoalEntity? = goalDao.getGoalById(goalId)
    suspend fun saveGoal(goal: GoalEntity) = goalDao.insertGoal(goal)
    suspend fun deleteGoal(goalId: String) = goalDao.deleteGoalById(goalId)

    // 2. Triggers
    fun getAllTriggersFlow(): Flow<List<TriggerEntity>> = triggerDao.getAllTriggersFlow()
    suspend fun getActiveTriggersForPackage(packageName: String): List<TriggerEntity> = triggerDao.getActiveTriggersForPackage(packageName)
    suspend fun getTriggersForGoal(goalId: String): List<TriggerEntity> = triggerDao.getTriggersForGoal(goalId)
    suspend fun saveTrigger(trigger: TriggerEntity) = triggerDao.insertTrigger(trigger)
    suspend fun deleteTrigger(triggerId: String) = triggerDao.deleteTriggerById(triggerId)

    // 3. Replacement Behaviours
    fun getAllBehavioursFlow(): Flow<List<ReplacementBehaviourEntity>> = replacementBehaviourDao.getAllBehavioursFlow()
    suspend fun getAllBehaviours(): List<ReplacementBehaviourEntity> = replacementBehaviourDao.getAllBehaviours()
    suspend fun getBehaviourById(behaviourId: String): ReplacementBehaviourEntity? = replacementBehaviourDao.getBehaviourById(behaviourId)
    suspend fun saveBehaviour(behaviour: ReplacementBehaviourEntity) = replacementBehaviourDao.insertBehaviour(behaviour)

    // 4. Behaviour Policies
    fun getAllPoliciesFlow(): Flow<List<BehaviourPolicyEntity>> = behaviourPolicyDao.getAllPoliciesFlow()
    suspend fun getActivePolicies(): List<BehaviourPolicyEntity> = behaviourPolicyDao.getActivePolicies()
    suspend fun getActivePoliciesForTrigger(triggerId: String): List<BehaviourPolicyEntity> = behaviourPolicyDao.getActivePoliciesForTrigger(triggerId)
    suspend fun getPoliciesForGoal(goalId: String): List<BehaviourPolicyEntity> = behaviourPolicyDao.getPoliciesForGoal(goalId)
    suspend fun savePolicy(policy: BehaviourPolicyEntity) = behaviourPolicyDao.insertPolicy(policy)
    suspend fun deletePolicy(policyId: String) = behaviourPolicyDao.deletePolicyById(policyId)

    // 5. Goal Progress
    fun getProgressForGoalFlow(goalId: String): Flow<List<GoalProgressEntity>> = goalProgressDao.getProgressForGoalFlow(goalId)

    suspend fun recordGoalCompletion(goalId: String, durationSec: Int, dateString: String = todayString) {
        val existing = goalProgressDao.getProgressForDate(goalId, dateString)
        if (existing == null) {
            val goal = goalDao.getGoalById(goalId)
            val target = goal?.dailyTarget ?: 1
            goalProgressDao.insertOrUpdateProgress(
                GoalProgressEntity(
                    goalId = goalId,
                    dateString = dateString,
                    completedCount = 1,
                    targetCount = target,
                    completedDurationSeconds = durationSec,
                    completionPercentage = (1f / target.toFloat()) * 100f
                )
            )
        } else {
            goalProgressDao.incrementGoalCompletion(goalId, dateString, durationSec)
        }
    }
}

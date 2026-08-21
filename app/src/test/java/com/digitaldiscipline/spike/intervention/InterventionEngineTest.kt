package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.data.local.dao.*
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.intervention.engine.InterventionDecision
import com.digitaldiscipline.spike.intervention.engine.InterventionEngine
import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.policy.PolicyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InterventionEngineTest {

    private val appRules = mutableMapOf<String, AppRuleEntity>()
    private val goals = mutableMapOf<String, GoalEntity>()
    private val triggers = mutableMapOf<String, TriggerEntity>()
    private val replacementBehaviours = mutableMapOf<String, ReplacementBehaviourEntity>()
    private val behaviourPolicies = mutableMapOf<String, BehaviourPolicyEntity>()
    private val goalProgress = mutableMapOf<String, GoalProgressEntity>()

    private lateinit var policyRepository: PolicyRepository
    private lateinit var behaviourRepository: BehaviourRepository
    private lateinit var engine: InterventionEngine

    @Before
    fun setup() {
        appRules.clear()
        goals.clear()
        triggers.clear()
        replacementBehaviours.clear()
        behaviourPolicies.clear()
        goalProgress.clear()

        val mockAppRuleDao = object : AppRuleDao {
            override fun getAllRulesFlow(): Flow<List<AppRuleEntity>> = flowOf(appRules.values.toList())
            override suspend fun getActiveRules(): List<AppRuleEntity> = appRules.values.filter { it.isEnabled }
            override suspend fun getRuleByPackage(packageName: String): AppRuleEntity? = appRules[packageName]
            override fun getRuleByPackageFlow(packageName: String): Flow<AppRuleEntity?> = flowOf(appRules[packageName])
            override suspend fun insertOrUpdate(rule: AppRuleEntity) { appRules[rule.packageName] = rule }
            override suspend fun insertAll(rules: List<AppRuleEntity>) { rules.forEach { appRules[it.packageName] = it } }
            override suspend fun deleteRule(rule: AppRuleEntity) { appRules.remove(rule.packageName) }
            override suspend fun deleteByPackage(packageName: String) { appRules.remove(packageName) }
            override suspend fun deleteAllRules() { appRules.clear() }
            override suspend fun setRuleEnabled(packageName: String, isEnabled: Boolean) {
                appRules[packageName]?.let { appRules[packageName] = it.copy(isEnabled = isEnabled) }
            }
        }

        val mockScheduleDao = object : ScheduleDao {
            override fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>> = flowOf(emptyList())
            override suspend fun getSchedulesForPackage(packageName: String): List<ScheduleEntity> = emptyList()
            override suspend fun insertSchedule(schedule: ScheduleEntity): Long = 1L
            override suspend fun insertAll(schedules: List<ScheduleEntity>) {}
            override suspend fun deleteSchedule(schedule: ScheduleEntity) {}
            override suspend fun deleteByPackage(packageName: String) {}
            override suspend fun deleteAllSchedules() {}
        }

        val mockTemporaryUnlockDao = object : TemporaryUnlockDao {
            override suspend fun getUnlock(packageName: String): TemporaryUnlockEntity? = null
            override fun getUnlockFlow(packageName: String): Flow<TemporaryUnlockEntity?> = flowOf(null)
            override suspend fun getAllUnlocks(): List<TemporaryUnlockEntity> = emptyList()
            override suspend fun insertOrUpdate(unlock: TemporaryUnlockEntity) {}
            override suspend fun deleteUnlock(packageName: String) {}
            override suspend fun purgeExpiredUnlocks(currentElapsedRealtime: Long) {}
            override suspend fun clearAllUnlocks() {}
        }

        policyRepository = PolicyRepository(mockAppRuleDao, mockScheduleDao, mockTemporaryUnlockDao)

        val mockGoalDao = object : GoalDao {
            override fun getAllGoalsFlow(): Flow<List<GoalEntity>> = flowOf(goals.values.toList())
            override suspend fun getActiveGoals(): List<GoalEntity> = goals.values.filter { it.active }
            override suspend fun getGoalById(goalId: String): GoalEntity? = goals[goalId]
            override suspend fun insertGoal(goal: GoalEntity) { goals[goal.goalId] = goal }
            override suspend fun insertAll(goalList: List<GoalEntity>) { goalList.forEach { goals[it.goalId] = it } }
            override suspend fun updateGoal(goal: GoalEntity) { goals[goal.goalId] = goal }
            override suspend fun deleteGoal(goal: GoalEntity) { goals.remove(goal.goalId) }
            override suspend fun deleteGoalById(goalId: String) { goals.remove(goalId) }
            override suspend fun clearAllGoals() { goals.clear() }
        }

        val mockTriggerDao = object : TriggerDao {
            override fun getAllTriggersFlow(): Flow<List<TriggerEntity>> = flowOf(triggers.values.toList())
            override suspend fun getActiveTriggersForPackage(packageName: String): List<TriggerEntity> =
                triggers.values.filter { it.packageName == packageName && it.active }
            override suspend fun getTriggersForGoal(goalId: String): List<TriggerEntity> =
                triggers.values.filter { it.goalId == goalId }
            override suspend fun getTriggerById(triggerId: String): TriggerEntity? = triggers[triggerId]
            override suspend fun insertTrigger(trigger: TriggerEntity) { triggers[trigger.triggerId] = trigger }
            override suspend fun insertAll(triggerList: List<TriggerEntity>) { triggerList.forEach { triggers[it.triggerId] = it } }
            override suspend fun updateTrigger(trigger: TriggerEntity) { triggers[trigger.triggerId] = trigger }
            override suspend fun deleteTrigger(trigger: TriggerEntity) { triggers.remove(trigger.triggerId) }
            override suspend fun deleteTriggerById(triggerId: String) { triggers.remove(triggerId) }
            override suspend fun clearAllTriggers() { triggers.clear() }
        }

        val mockReplacementBehaviourDao = object : ReplacementBehaviourDao {
            override fun getAllBehavioursFlow(): Flow<List<ReplacementBehaviourEntity>> = flowOf(replacementBehaviours.values.toList())
            override suspend fun getAllBehaviours(): List<ReplacementBehaviourEntity> = replacementBehaviours.values.toList()
            override suspend fun getBehavioursByCategory(category: String): List<ReplacementBehaviourEntity> =
                replacementBehaviours.values.filter { it.category == category }
            override suspend fun getBehaviourById(behaviourId: String): ReplacementBehaviourEntity? = replacementBehaviours[behaviourId]
            override suspend fun getBehaviourByType(type: String): ReplacementBehaviourEntity? =
                replacementBehaviours.values.firstOrNull { it.type == type }
            override suspend fun insertBehaviour(behaviour: ReplacementBehaviourEntity) { replacementBehaviours[behaviour.behaviourId] = behaviour }
            override suspend fun insertAll(behaviours: List<ReplacementBehaviourEntity>) { behaviours.forEach { replacementBehaviours[it.behaviourId] = it } }
            override suspend fun updateBehaviour(behaviour: ReplacementBehaviourEntity) { replacementBehaviours[behaviour.behaviourId] = behaviour }
            override suspend fun deleteBehaviour(behaviour: ReplacementBehaviourEntity) { replacementBehaviours.remove(behaviour.behaviourId) }
            override suspend fun clearAllBehaviours() { replacementBehaviours.clear() }
        }

        val mockBehaviourPolicyDao = object : BehaviourPolicyDao {
            override fun getAllPoliciesFlow(): Flow<List<BehaviourPolicyEntity>> = flowOf(behaviourPolicies.values.toList())
            override suspend fun getActivePolicies(): List<BehaviourPolicyEntity> = behaviourPolicies.values.filter { it.enabled }
            override suspend fun getActivePoliciesForTrigger(triggerId: String): List<BehaviourPolicyEntity> =
                behaviourPolicies.values.filter { it.triggerId == triggerId && it.enabled }
            override suspend fun getPoliciesForGoal(goalId: String): List<BehaviourPolicyEntity> =
                behaviourPolicies.values.filter { it.goalId == goalId }
            override suspend fun getPolicyById(policyId: String): BehaviourPolicyEntity? = behaviourPolicies[policyId]
            override suspend fun insertPolicy(policy: BehaviourPolicyEntity) { behaviourPolicies[policy.policyId] = policy }
            override suspend fun insertAll(policies: List<BehaviourPolicyEntity>) { policies.forEach { behaviourPolicies[it.policyId] = it } }
            override suspend fun updatePolicy(policy: BehaviourPolicyEntity) { behaviourPolicies[policy.policyId] = policy }
            override suspend fun deletePolicy(policy: BehaviourPolicyEntity) { behaviourPolicies.remove(policy.policyId) }
            override suspend fun deletePolicyById(policyId: String) { behaviourPolicies.remove(policyId) }
            override suspend fun clearAllPolicies() { behaviourPolicies.clear() }
        }

        val mockGoalProgressDao = object : GoalProgressDao {
            override fun getProgressForGoalFlow(goalId: String): Flow<List<GoalProgressEntity>> =
                flowOf(goalProgress.values.filter { it.goalId == goalId })
            override suspend fun getProgressForDate(goalId: String, dateString: String): GoalProgressEntity? =
                goalProgress["${goalId}_${dateString}"]
            override suspend fun getAllProgressForDate(dateString: String): List<GoalProgressEntity> =
                goalProgress.values.filter { it.dateString == dateString }
            override suspend fun insertOrUpdateProgress(progress: GoalProgressEntity) {
                goalProgress["${progress.goalId}_${progress.dateString}"] = progress
            }
            override suspend fun insertAll(progressList: List<GoalProgressEntity>) {
                progressList.forEach { goalProgress["${it.goalId}_${it.dateString}"] = it }
            }
            override suspend fun incrementGoalCompletion(goalId: String, dateString: String, durationSec: Int, now: Long) {
                val key = "${goalId}_${dateString}"
                val curr = goalProgress[key]
                if (curr != null) {
                    val count = curr.completedCount + 1
                    val pct = (count.toFloat() / curr.targetCount.toFloat()) * 100f
                    goalProgress[key] = curr.copy(
                        completedCount = count,
                        completedDurationSeconds = curr.completedDurationSeconds + durationSec,
                        completionPercentage = pct.coerceAtMost(100f),
                        lastUpdated = now
                    )
                }
            }
            override suspend fun clearAllProgress() { goalProgress.clear() }
        }

        behaviourRepository = BehaviourRepository(
            goalDao = mockGoalDao,
            triggerDao = mockTriggerDao,
            replacementBehaviourDao = mockReplacementBehaviourDao,
            behaviourPolicyDao = mockBehaviourPolicyDao,
            goalProgressDao = mockGoalProgressDao
        )

        engine = InterventionEngine(
            context = null,
            policyRepository = policyRepository,
            behaviourRepository = behaviourRepository
        )
    }

    @Test
    fun parentBlockOverridesSelfMode() = runBlocking {
        val pkg = "com.instagram.android"
        appRules[pkg] = AppRuleEntity(
            packageName = pkg,
            appDisplayName = "Instagram",
            mode = RuleMode.BLOCK,
            isEnabled = true
        )

        val decision = engine.evaluateTargetPackage(pkg, UserMode.SELF)
        assertTrue(decision is InterventionDecision.Block)
        assertEquals(pkg, (decision as InterventionDecision.Block).targetPackage)
    }

    @Test
    fun parentDelayEnforcedInParentMode() = runBlocking {
        val pkg = "com.instagram.android"
        appRules[pkg] = AppRuleEntity(
            packageName = pkg,
            appDisplayName = "Instagram",
            mode = RuleMode.DELAY,
            pauseDurationSeconds = 15,
            isEnabled = true
        )

        val decision = engine.evaluateTargetPackage(pkg, UserMode.PARENT)
        assertTrue(decision is InterventionDecision.ParentDelay)
        assertEquals(15, (decision as InterventionDecision.ParentDelay).delaySeconds)
    }

    @Test
    fun selfModeResolvesConfiguredIntervention() = runBlocking {
        val pkg = "com.instagram.android"
        triggers["trig_insta"] = TriggerEntity(
            triggerId = "trig_insta",
            goalId = "goal_1",
            packageName = pkg,
            active = true,
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            daysOfWeek = "1,2,3,4,5,6,7"
        )
        behaviourPolicies["pol_1"] = BehaviourPolicyEntity(
            policyId = "pol_1",
            goalId = "goal_1",
            triggerId = "trig_insta",
            replacementBehaviourId = "beh_squats_10",
            interventionMode = "EARN",
            earnedSeconds = 600,
            enabled = true
        )
        goals["goal_1"] = GoalEntity(
            goalId = "goal_1",
            title = "Fitness Habit",
            active = true
        )

        val decision = engine.evaluateTargetPackage(pkg, UserMode.SELF)
        assertTrue(decision is InterventionDecision.Challenge)
        val challenge = decision as InterventionDecision.Challenge
        assertEquals(PolicySource.SELF, challenge.session.policySource)
        assertEquals("SQUATS", challenge.session.intervention.id)
    }

    @Test
    fun unmonitoredAppAllowsDirectAccess() = runBlocking {
        val pkg = "com.google.android.calculator"
        val decision = engine.evaluateTargetPackage(pkg, UserMode.SELF)
        assertTrue(decision is InterventionDecision.Allow)
    }
}

package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.*
import com.digitaldiscipline.spike.data.local.dao.*
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.policy.PolicyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ParentChildModeEngineTest {

    private lateinit var policyRepository: PolicyRepository
    private lateinit var behaviourRepository: BehaviourRepository
    private lateinit var behaviourPolicyResolver: BehaviourPolicyResolver

    private val appRules = mutableMapOf<String, AppRuleEntity>()
    private val schedules = mutableListOf<ScheduleEntity>()
    private val temporaryUnlocks = mutableMapOf<String, TemporaryUnlockEntity>()

    private val goals = mutableMapOf<String, GoalEntity>()
    private val triggers = mutableMapOf<String, TriggerEntity>()
    private val replacementBehaviours = mutableMapOf<String, ReplacementBehaviourEntity>()
    private val behaviourPolicies = mutableMapOf<String, BehaviourPolicyEntity>()
    private val goalProgress = mutableMapOf<String, GoalProgressEntity>()

    @Before
    fun setUp() {
        appRules.clear()
        schedules.clear()
        temporaryUnlocks.clear()
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
            override fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>> = flowOf(schedules)
            override suspend fun getAllSchedules(): List<ScheduleEntity> = schedules
            override suspend fun getSchedulesForPackage(packageName: String): List<ScheduleEntity> {
                return schedules.filter { (it.packageName == packageName || it.packageName == "ALL_RESTRICTED") && it.isEnabled }
            }
            override suspend fun insertSchedule(schedule: ScheduleEntity): Long {
                schedules.add(schedule)
                return schedule.id
            }
            override suspend fun insertAll(schedulesList: List<ScheduleEntity>) { schedules.addAll(schedulesList) }
            override suspend fun updateSchedule(schedule: ScheduleEntity) {
                schedules.removeIf { it.id == schedule.id }
                schedules.add(schedule)
            }
            override suspend fun deleteSchedule(schedule: ScheduleEntity) { schedules.removeIf { it.id == schedule.id } }
            override suspend fun deleteById(id: Long) { schedules.removeIf { it.id == id } }
            override suspend fun deleteByPackage(packageName: String) { schedules.removeIf { it.packageName == packageName } }
            override suspend fun deleteAllSchedules() { schedules.clear() }
        }

        val mockTemporaryUnlockDao = object : TemporaryUnlockDao {
            override suspend fun getUnlock(packageName: String): TemporaryUnlockEntity? = temporaryUnlocks[packageName]
            override fun getUnlockFlow(packageName: String): Flow<TemporaryUnlockEntity?> = flowOf(temporaryUnlocks[packageName])
            override suspend fun getAllUnlocks(): List<TemporaryUnlockEntity> = temporaryUnlocks.values.toList()
            override suspend fun insertOrUpdate(unlock: TemporaryUnlockEntity) { temporaryUnlocks[unlock.packageName] = unlock }
            override suspend fun deleteUnlock(packageName: String) { temporaryUnlocks.remove(packageName) }
            override suspend fun purgeExpiredUnlocks(currentElapsedRealtime: Long) {
                temporaryUnlocks.entries.removeIf { it.value.unlockExpiryElapsedRealtime <= currentElapsedRealtime }
            }
            override suspend fun clearAllUnlocks() { temporaryUnlocks.clear() }
        }

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

        policyRepository = PolicyRepository(mockAppRuleDao, mockScheduleDao, mockTemporaryUnlockDao)
        behaviourRepository = BehaviourRepository(mockGoalDao, mockTriggerDao, mockReplacementBehaviourDao, mockBehaviourPolicyDao, mockGoalProgressDao)
        behaviourPolicyResolver = BehaviourPolicyResolver(policyRepository, behaviourRepository)
    }

    @Test
    fun parentBlockMode_enforcesStrictBlock() = runBlocking {
        val parentRule = AppRuleEntity(
            packageName = "com.instagram.android",
            appDisplayName = "Instagram",
            mode = RuleMode.BLOCK,
            isEnabled = true,
            unlockDurationSeconds = 0
        )
        policyRepository.saveRule(parentRule)

        val result = behaviourPolicyResolver.resolvePolicy("com.instagram.android", userMode = UserMode.PARENT)
        assertTrue(result is PolicyResolutionResult.ParentPolicyMatch)
        val match = result as PolicyResolutionResult.ParentPolicyMatch
        assertEquals(RuleMode.BLOCK, match.appRule.mode)
        assertEquals("Instagram", match.appRule.appDisplayName)
        assertTrue(match.appRule.isEnabled)
    }

    @Test
    fun parentEarnMode_enforcesParentConfiguredUnlockWindowAndReps() = runBlocking {
        val parentRule = AppRuleEntity(
            packageName = "com.google.android.youtube",
            appDisplayName = "YouTube",
            mode = RuleMode.EARN,
            isEnabled = true,
            unlockDurationSeconds = 300,
            squatsTargetCount = 15,
            pauseDurationSeconds = 15,
            breathingDurationSeconds = 30
        )
        policyRepository.saveRule(parentRule)

        val result = behaviourPolicyResolver.resolvePolicy("com.google.android.youtube", userMode = UserMode.PARENT)
        assertTrue(result is PolicyResolutionResult.ParentPolicyMatch)
        val match = result as PolicyResolutionResult.ParentPolicyMatch
        assertEquals(RuleMode.EARN, match.appRule.mode)
        assertEquals(300, match.appRule.unlockDurationSeconds)
        assertEquals(15, match.appRule.squatsTargetCount)
    }

    @Test
    fun parentMode_takesPrecedenceOverChildSelfModeRules() = runBlocking {
        val parentRule = AppRuleEntity(
            packageName = "com.dts.freefireth",
            appDisplayName = "Free Fire",
            mode = RuleMode.BLOCK,
            isEnabled = true
        )
        policyRepository.saveRule(parentRule)

        val childGoal = GoalEntity(goalId = "goal_child", title = "Play Games", category = "GAMING")
        val childTrigger = TriggerEntity(triggerId = "trig_ff", goalId = "goal_child", packageName = "com.dts.freefireth", appDisplayName = "Free Fire", category = "GAMING")
        val childBehaviour = ReplacementBehaviourEntity(behaviourId = "beh_play", category = "PAUSE", type = "PAUSE", title = "Short Pause")
        val childPolicy = BehaviourPolicyEntity(policyId = "pol_ff", goalId = "goal_child", triggerId = "trig_ff", replacementBehaviourId = "beh_play", interventionMode = "EARN", earnedSeconds = 900)

        behaviourRepository.saveGoal(childGoal)
        behaviourRepository.saveTrigger(childTrigger)
        behaviourRepository.saveBehaviour(childBehaviour)
        behaviourRepository.savePolicy(childPolicy)

        val result = behaviourPolicyResolver.resolvePolicy("com.dts.freefireth", userMode = UserMode.PARENT)
        assertTrue(result is PolicyResolutionResult.ParentPolicyMatch)
        val match = result as PolicyResolutionResult.ParentPolicyMatch
        assertEquals(RuleMode.BLOCK, match.appRule.mode)
    }

    @Test
    fun parentScheduledStudyWindow_savesAndRetrievesSchedule() = runBlocking {
        val schedule = ScheduleEntity(
            id = 101L,
            packageName = "com.instagram.android",
            label = "School Study Window",
            startHour = 9,
            startMinute = 0,
            endHour = 15,
            endMinute = 30,
            daysOfWeekCsv = "1,2,3,4,5,6,7",
            isEnabled = true
        )
        policyRepository.saveSchedule(schedule)

        val allSchedules = policyRepository.getAllSchedules()
        assertEquals(1, allSchedules.size)
        assertEquals("School Study Window", allSchedules.first().label)
        assertTrue(allSchedules.first().isEnabled)
    }

    @Test
    fun parentUnselectedApp_returnsNoMatchWhenNotRestricted() = runBlocking {
        val result = behaviourPolicyResolver.resolvePolicy("com.google.android.calculator", userMode = UserMode.PARENT)
        assertTrue(result is PolicyResolutionResult.NoMatch)
    }

    @Test
    fun parentDisabledRule_isNotEnforced() = runBlocking {
        val parentRule = AppRuleEntity(
            packageName = "com.snapchat.android",
            appDisplayName = "Snapchat",
            mode = RuleMode.BLOCK,
            isEnabled = false
        )
        policyRepository.saveRule(parentRule)

        val result = behaviourPolicyResolver.resolvePolicy("com.snapchat.android", userMode = UserMode.PARENT)
        assertTrue(result is PolicyResolutionResult.NoMatch)
    }
}

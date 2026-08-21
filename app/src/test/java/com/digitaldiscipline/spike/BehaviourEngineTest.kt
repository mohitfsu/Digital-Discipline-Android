package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.BehaviourPolicyResolver
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.PolicyResolutionResult
import com.digitaldiscipline.spike.data.local.dao.*
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.policy.PolicyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class BehaviourEngineTest {

    // In-memory test doubles
    private val appRules = mutableMapOf<String, AppRuleEntity>()
    private val goals = mutableMapOf<String, GoalEntity>()
    private val triggers = mutableMapOf<String, TriggerEntity>()
    private val replacementBehaviours = mutableMapOf<String, ReplacementBehaviourEntity>()
    private val behaviourPolicies = mutableMapOf<String, BehaviourPolicyEntity>()
    private val goalProgress = mutableMapOf<String, GoalProgressEntity>()

    private lateinit var policyRepository: PolicyRepository
    private lateinit var behaviourRepository: BehaviourRepository
    private lateinit var resolver: BehaviourPolicyResolver

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
        resolver = BehaviourPolicyResolver(policyRepository, behaviourRepository)
    }

    @Test
    fun testGoalCreationAndValidation() = runBlocking {
        val goal = GoalEntity(
            goalId = "goal_fitness_1",
            ownerId = "user_1",
            mode = UserMode.SELF.name,
            title = "Daily Exercise Routine",
            category = GoalCategory.FITNESS.name,
            dailyTarget = 2,
            unit = "sessions"
        )
        behaviourRepository.saveGoal(goal)

        val retrieved = behaviourRepository.getGoalById("goal_fitness_1")
        assertNotNull(retrieved)
        assertEquals("Daily Exercise Routine", retrieved?.title)
        assertEquals(GoalCategory.FITNESS.name, retrieved?.category)
        assertEquals(2, retrieved?.dailyTarget)
    }

    @Test
    fun testTriggerTimeAndDayMatching() = runBlocking {
        val trigger = TriggerEntity(
            triggerId = "trig_insta_1",
            goalId = "goal_fitness_1",
            packageName = "com.instagram.android",
            appDisplayName = "Instagram",
            category = TriggerCategory.SOCIAL_MEDIA.name,
            startHour = 18,
            startMinute = 0,
            endHour = 22,
            endMinute = 0,
            daysOfWeek = "1,2,3,4,5,6,7"
        )
        behaviourRepository.saveTrigger(trigger)

        val active = behaviourRepository.getActiveTriggersForPackage("com.instagram.android")
        assertEquals(1, active.size)
        assertEquals("trig_insta_1", active[0].triggerId)
    }

    @Test
    fun testReplacementBehaviourConfiguration() = runBlocking {
        val squats = ReplacementBehaviourEntity(
            behaviourId = "beh_squats_15",
            category = BehaviourCategory.PHYSICAL.name,
            type = BehaviourType.SQUATS.name,
            title = "15 Squats Challenge",
            targetCount = 15,
            durationSeconds = 60,
            unit = "reps"
        )
        behaviourRepository.saveBehaviour(squats)

        val retrieved = behaviourRepository.getBehaviourById("beh_squats_15")
        assertNotNull(retrieved)
        assertEquals(15, retrieved?.targetCount)
        assertEquals(BehaviourCategory.PHYSICAL.name, retrieved?.category)
    }

    @Test
    fun testBehaviourPolicyResolutionInSelfMode() = runBlocking {
        // Setup: Goal + Trigger + Replacement Behaviour + Policy
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", category = "FITNESS")
        val trigger = TriggerEntity(
            triggerId = "t1",
            goalId = "g1",
            packageName = "com.instagram.android",
            appDisplayName = "Instagram",
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59
        )
        val squats = ReplacementBehaviourEntity(
            behaviourId = "b1",
            category = "PHYSICAL",
            type = "SQUATS",
            title = "10 Squats",
            targetCount = 10
        )
        val policy = BehaviourPolicyEntity(
            policyId = "p1",
            goalId = "g1",
            triggerId = "t1",
            replacementBehaviourId = "b1",
            interventionMode = "EARN",
            earnedSeconds = 600
        )

        behaviourRepository.saveGoal(goal)
        behaviourRepository.saveTrigger(trigger)
        behaviourRepository.saveBehaviour(squats)
        behaviourRepository.savePolicy(policy)

        // Resolve policy in SELF mode
        val result = resolver.resolvePolicy(
            packageName = "com.instagram.android",
            userMode = UserMode.SELF
        )

        assertTrue("Expected BehaviourPolicyMatch", result is PolicyResolutionResult.BehaviourPolicyMatch)
        val match = result as PolicyResolutionResult.BehaviourPolicyMatch
        assertEquals("Instagram", match.resolvedAppRule.appDisplayName)
        assertEquals(RuleMode.EARN, match.resolvedAppRule.mode)
        assertEquals(10, match.resolvedAppRule.squatsTargetCount)
        assertEquals(600, match.resolvedAppRule.unlockDurationSeconds)
    }

    @Test
    fun testParentPolicyPrecedence() = runBlocking {
        // Parent restriction for YouTube (BLOCK mode)
        val parentRule = AppRuleEntity(
            packageName = "com.google.android.youtube",
            appDisplayName = "YouTube",
            mode = RuleMode.BLOCK,
            isEnabled = true
        )
        policyRepository.saveRule(parentRule)

        // Resolve in PARENT mode
        val result = resolver.resolvePolicy(
            packageName = "com.google.android.youtube",
            userMode = UserMode.PARENT
        )

        assertTrue("Expected ParentPolicyMatch", result is PolicyResolutionResult.ParentPolicyMatch)
        val match = result as PolicyResolutionResult.ParentPolicyMatch
        assertEquals(RuleMode.BLOCK, match.appRule.mode)
    }

    @Test
    fun testParentPolicyPrecedenceOverSelfModeWhenOverriding() = runBlocking {
        // Parent rule: YouTube is strictly BLOCKED
        val parentRule = AppRuleEntity(
            packageName = "com.google.android.youtube",
            appDisplayName = "YouTube",
            mode = RuleMode.BLOCK,
            isEnabled = true
        )
        policyRepository.saveRule(parentRule)

        // Self mode trigger also configured for YouTube
        val goal = GoalEntity(goalId = "g2", title = "Study Goal")
        val trigger = TriggerEntity(triggerId = "t2", goalId = "g2", packageName = "com.google.android.youtube")
        val pause = ReplacementBehaviourEntity(behaviourId = "b2", category = "MINDFUL", type = "MINDFUL_PAUSE", title = "Pause")
        val policy = BehaviourPolicyEntity(policyId = "p2", goalId = "g2", triggerId = "t2", replacementBehaviourId = "b2", interventionMode = "EARN")

        behaviourRepository.saveGoal(goal)
        behaviourRepository.saveTrigger(trigger)
        behaviourRepository.saveBehaviour(pause)
        behaviourRepository.savePolicy(policy)

        // In PARENT mode, parent rule ALWAYS wins
        val parentResult = resolver.resolvePolicy("com.google.android.youtube", UserMode.PARENT)
        assertTrue(parentResult is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.BLOCK, (parentResult as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    @Test
    fun testNoMatchWhenOutsideTimeWindow() = runBlocking {
        // Trigger configured only for 18:00 to 20:00
        val goal = GoalEntity(goalId = "g3", title = "Evening Rest")
        val trigger = TriggerEntity(
            triggerId = "t3",
            goalId = "g3",
            packageName = "com.dts.freefireth",
            startHour = 18,
            startMinute = 0,
            endHour = 20,
            endMinute = 0
        )
        val squats = ReplacementBehaviourEntity(behaviourId = "b3", title = "Squats")
        val policy = BehaviourPolicyEntity(policyId = "p3", goalId = "g3", triggerId = "t3", replacementBehaviourId = "b3")

        behaviourRepository.saveGoal(goal)
        behaviourRepository.saveTrigger(trigger)
        behaviourRepository.saveBehaviour(squats)
        behaviourRepository.savePolicy(policy)

        // Set time to 10:00 AM (outside window)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }

        val result = resolver.resolvePolicy(
            packageName = "com.dts.freefireth",
            userMode = UserMode.SELF,
            currentTimeMillis = cal.timeInMillis
        )

        assertTrue("Expected NoMatch outside active time window", result is PolicyResolutionResult.NoMatch)
    }

    @Test
    fun testGoalProgressIncrementation() = runBlocking {
        val goal = GoalEntity(goalId = "goal_fit_p", title = "Goal Fit", dailyTarget = 2)
        behaviourRepository.saveGoal(goal)

        behaviourRepository.recordGoalCompletion("goal_fit_p", 60, "2026-08-16")
        val p1 = behaviourRepository.getProgressForGoalFlow("goal_fit_p")

        // Increment again
        behaviourRepository.recordGoalCompletion("goal_fit_p", 60, "2026-08-16")
        val p2 = behaviourRepository.getProgressForGoalFlow("goal_fit_p")
        assertNotNull(p2)
    }

    @Test
    fun testExistingAllowBlockDelayEarnModesRegression() = runBlocking {
        val modes = listOf(RuleMode.ALLOW, RuleMode.BLOCK, RuleMode.DELAY, RuleMode.EARN)
        modes.forEach { mode ->
            val rule = AppRuleEntity(
                packageName = "com.test.app.${mode.name.lowercase()}",
                appDisplayName = "Test App",
                mode = mode,
                isEnabled = true
            )
            policyRepository.saveRule(rule)
            val res = resolver.resolvePolicy(rule.packageName, UserMode.PARENT)
            assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
            assertEquals(mode, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
        }
    }
}

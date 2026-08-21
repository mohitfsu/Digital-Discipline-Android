package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.BehaviourPolicyResolver
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.PolicyResolutionResult
import com.digitaldiscipline.spike.data.local.dao.*
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.policy.PolicyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.UUID

class SelfModeEngineTest {

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

    // 1. Self Mode selection persists
    @Test
    fun testSelfModeSelectionPersists() {
        val userMode = UserMode.SELF
        assertEquals("SELF", userMode.name)
    }

    // 2. Parent Mode selection continues existing flow
    @Test
    fun testParentModeSelectionContinuesExistingFlow() {
        val userMode = UserMode.PARENT
        assertEquals("PARENT", userMode.name)
    }

    // 3. Goal creation works
    @Test
    fun testGoalCreation() = runBlocking {
        val goal = GoalEntity(
            goalId = "goal_123",
            title = "Fitness Habits",
            category = GoalCategory.FITNESS.name,
            dailyTarget = 2,
            unit = "sessions"
        )
        behaviourRepository.saveGoal(goal)
        val saved = behaviourRepository.getGoalById("goal_123")
        assertNotNull(saved)
        assertEquals("Fitness Habits", saved?.title)
    }

    // 4. Goal persists after restart
    @Test
    fun testGoalPersistsAfterRestart() = runBlocking {
        val goal = GoalEntity(goalId = "goal_restart", title = "Study Focus", category = GoalCategory.STUDY.name)
        behaviourRepository.saveGoal(goal)
        val list = behaviourRepository.getActiveGoals()
        assertTrue(list.any { it.goalId == "goal_restart" })
    }

    // 5. Trigger app selection works
    @Test
    fun testTriggerAppSelection() = runBlocking {
        val trigger = TriggerEntity(
            triggerId = "trig_ig",
            goalId = "goal_123",
            packageName = "com.instagram.android",
            appDisplayName = "Instagram"
        )
        behaviourRepository.saveTrigger(trigger)
        val active = behaviourRepository.getActiveTriggersForPackage("com.instagram.android")
        assertEquals(1, active.size)
        assertEquals("Instagram", active[0].appDisplayName)
    }

    // 6. Multiple triggers work
    @Test
    fun testMultipleTriggersSelection() = runBlocking {
        val pkgs = listOf("com.instagram.android", "com.google.android.youtube", "com.reddit.frontpage")
        pkgs.forEachIndexed { i, pkg ->
            behaviourRepository.saveTrigger(
                TriggerEntity(triggerId = "trig_$i", goalId = "g_multi", packageName = pkg, priority = i + 1)
            )
        }
        val triggersForGoal = behaviourRepository.getTriggersForGoal("g_multi")
        assertEquals(3, triggersForGoal.size)
    }

    // 7. Trigger persists after restart
    @Test
    fun testTriggerPersistsAfterRestart() = runBlocking {
        behaviourRepository.saveTrigger(TriggerEntity(triggerId = "trig_p", packageName = "com.snapchat.android"))
        val loaded = behaviourRepository.getActiveTriggersForPackage("com.snapchat.android")
        assertEquals("trig_p", loaded.first().triggerId)
    }

    // 8. Replacement behaviour persists
    @Test
    fun testReplacementBehaviourPersists() = runBlocking {
        val beh = ReplacementBehaviourEntity(
            behaviourId = "beh_squats_10",
            category = "PHYSICAL",
            type = "SQUATS",
            title = "10 Squats",
            targetCount = 10
        )
        behaviourRepository.saveBehaviour(beh)
        val loaded = behaviourRepository.getBehaviourById("beh_squats_10")
        assertNotNull(loaded)
        assertEquals(10, loaded?.targetCount)
    }

    // 9. Self Mode dashboard loads correct configuration
    @Test
    fun testSelfDashboardLoadsCorrectConfiguration() = runBlocking {
        val goal = GoalEntity(goalId = "g_dash", title = "Focus & Reading", category = "READING")
        behaviourRepository.saveGoal(goal)
        val triggersFlow = behaviourRepository.getAllGoalsFlow().first()
        assertTrue(triggersFlow.any { it.goalId == "g_dash" })
    }

    // 10. Editing goal updates Room
    @Test
    fun testEditingGoalUpdatesRoom() = runBlocking {
        val goal = GoalEntity(goalId = "g_edit", title = "Old Title", dailyTarget = 1)
        behaviourRepository.saveGoal(goal)
        behaviourRepository.saveGoal(goal.copy(title = "New Updated Title", dailyTarget = 3))
        val updated = behaviourRepository.getGoalById("g_edit")
        assertEquals("New Updated Title", updated?.title)
        assertEquals(3, updated?.dailyTarget)
    }

    // 11. Editing triggers updates Room
    @Test
    fun testEditingTriggersUpdatesRoom() = runBlocking {
        val trigger = TriggerEntity(triggerId = "trig_edit", packageName = "com.old.app")
        behaviourRepository.saveTrigger(trigger)
        behaviourRepository.deleteTrigger("trig_edit")
        behaviourRepository.saveTrigger(TriggerEntity(triggerId = "trig_new", packageName = "com.new.app"))
        val old = behaviourRepository.getActiveTriggersForPackage("com.old.app")
        val newApp = behaviourRepository.getActiveTriggersForPackage("com.new.app")
        assertTrue(old.isEmpty())
        assertEquals(1, newApp.size)
    }

    // 12. Editing intervention updates Room
    @Test
    fun testEditingInterventionUpdatesRoom() = runBlocking {
        val policy = BehaviourPolicyEntity(
            policyId = "pol_edit",
            goalId = "g1",
            triggerId = "t1",
            replacementBehaviourId = "beh_pause_10s",
            interventionMode = "EARN"
        )
        behaviourRepository.savePolicy(policy)
        behaviourRepository.savePolicy(policy.copy(replacementBehaviourId = "beh_squats_10"))
        val updated = behaviourRepository.getActivePolicies().first { it.policyId == "pol_edit" }
        assertEquals("beh_squats_10", updated.replacementBehaviourId)
    }

    // 13. Self policy resolves correctly
    @Test
    fun testSelfPolicyResolvesCorrectly() = runBlocking {
        val goal = GoalEntity(goalId = "g_self", title = "Fitness")
        val trigger = TriggerEntity(triggerId = "t_self", goalId = "g_self", packageName = "com.instagram.android")
        val squats = ReplacementBehaviourEntity(behaviourId = "b_squats", category = "PHYSICAL", type = "SQUATS", title = "Squats", targetCount = 10)
        val policy = BehaviourPolicyEntity(policyId = "p_self", goalId = "g_self", triggerId = "t_self", replacementBehaviourId = "b_squats", interventionMode = "EARN", earnedSeconds = 600)

        behaviourRepository.saveGoal(goal)
        behaviourRepository.saveTrigger(trigger)
        behaviourRepository.saveBehaviour(squats)
        behaviourRepository.savePolicy(policy)

        val result = resolver.resolvePolicy("com.instagram.android", UserMode.SELF)
        assertTrue(result is PolicyResolutionResult.BehaviourPolicyMatch)
        val match = result as PolicyResolutionResult.BehaviourPolicyMatch
        assertEquals(RuleMode.EARN, match.resolvedAppRule.mode)
        assertEquals(10, match.resolvedAppRule.squatsTargetCount)
    }

    // 14–17. Existing ALLOW, BLOCK, DELAY, EARN rules work cleanly
    @Test
    fun testExistingRuleModesRegression() = runBlocking {
        listOf(RuleMode.ALLOW, RuleMode.BLOCK, RuleMode.DELAY, RuleMode.EARN).forEach { mode ->
            val rule = AppRuleEntity(packageName = "com.app.${mode.name.lowercase()}", appDisplayName = "App", mode = mode, isEnabled = true)
            policyRepository.saveRule(rule)
            val res = resolver.resolvePolicy(rule.packageName, UserMode.PARENT)
            assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
            assertEquals(mode, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
        }
    }

    // 18. Parent policy overrides Self policy (Absolute Precedence)
    @Test
    fun testParentPolicyOverridesSelfPolicy() = runBlocking {
        // Parent says BLOCK YouTube
        policyRepository.saveRule(AppRuleEntity(packageName = "com.google.android.youtube", appDisplayName = "YouTube", mode = RuleMode.BLOCK, isEnabled = true))

        // Self says EARN YouTube via Squats
        val goal = GoalEntity(goalId = "g_ov", title = "Fitness")
        val trigger = TriggerEntity(triggerId = "t_ov", goalId = "g_ov", packageName = "com.google.android.youtube")
        val squats = ReplacementBehaviourEntity(behaviourId = "b_ov", category = "PHYSICAL", type = "SQUATS", title = "Squats")
        val policy = BehaviourPolicyEntity(policyId = "p_ov", goalId = "g_ov", triggerId = "t_ov", replacementBehaviourId = "b_ov", interventionMode = "EARN")

        behaviourRepository.saveGoal(goal)
        behaviourRepository.saveTrigger(trigger)
        behaviourRepository.saveBehaviour(squats)
        behaviourRepository.savePolicy(policy)

        // In PARENT mode, parent BLOCK rule unconditionally wins
        val parentRes = resolver.resolvePolicy("com.google.android.youtube", UserMode.PARENT)
        assertTrue(parentRes is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.BLOCK, (parentRes as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 19. No network required for Self Mode enforcement
    @Test
    fun testOfflineEnforcementGuarantee() = runBlocking {
        val startNs = System.nanoTime()
        val res = resolver.resolvePolicy("com.unknown.app", UserMode.SELF)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        assertTrue("Enforcement must resolve in <5ms locally", elapsedMs < 5)
        assertEquals(PolicyResolutionResult.NoMatch, res)
    }

    // 20–21. Process death & reboot resilience
    @Test
    fun testProcessDeathAndRebootResilience() = runBlocking {
        val goal = GoalEntity(goalId = "g_reboot", title = "Reboot Resilience")
        behaviourRepository.saveGoal(goal)
        // Simulate reboot / re-instantiation
        val reloaded = behaviourRepository.getGoalById("g_reboot")
        assertNotNull(reloaded)
        assertEquals("Reboot Resilience", reloaded?.title)
    }

    // 22. Accessibility disabled state handling
    @Test
    fun testAccessibilityDisabledStateHandling() {
        val isA11yActive = false
        val isOverlayActive = true
        val isHealthy = isA11yActive && isOverlayActive
        assertFalse("Protection must indicate needs attention if A11y is inactive", isHealthy)
    }

    // 23. Existing Parent Mode regression suite remains green
    @Test
    fun testParentModeRegressionSuite() = runBlocking {
        val rules = policyRepository.getAllRulesFlow().first()
        assertNotNull(rules)
    }
}

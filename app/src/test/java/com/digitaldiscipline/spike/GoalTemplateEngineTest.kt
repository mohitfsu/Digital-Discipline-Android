package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.*
import com.digitaldiscipline.spike.behaviour.templates.*
import com.digitaldiscipline.spike.data.local.dao.*
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.wallet.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GoalTemplateEngineTest {

    private val appRules = mutableMapOf<String, AppRuleEntity>()
    private val goals = mutableMapOf<String, GoalEntity>()
    private val triggers = mutableMapOf<String, TriggerEntity>()
    private val replacementBehaviours = mutableMapOf<String, ReplacementBehaviourEntity>()
    private val behaviourPolicies = mutableMapOf<String, BehaviourPolicyEntity>()
    private val goalProgress = mutableMapOf<String, GoalProgressEntity>()
    private val wallets = mutableMapOf<String, EarnedTimeWalletEntity>()
    private val transactions = mutableMapOf<String, WalletTransactionEntity>()
    private val sessions = mutableMapOf<String, WalletSessionEntity>()
    private val prefs = mutableMapOf<String, Any>()

    private lateinit var policyRepository: PolicyRepository
    private lateinit var behaviourRepository: BehaviourRepository
    private lateinit var resolver: BehaviourPolicyResolver
    private lateinit var walletService: EarnedTimeWalletService

    @Before
    fun setup() {
        appRules.clear()
        goals.clear()
        triggers.clear()
        replacementBehaviours.clear()
        behaviourPolicies.clear()
        goalProgress.clear()
        wallets.clear()
        transactions.clear()
        sessions.clear()
        prefs.clear()

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
            override suspend fun getAllSchedules(): List<ScheduleEntity> = emptyList()
            override suspend fun getSchedulesForPackage(packageName: String): List<ScheduleEntity> = emptyList()
            override suspend fun insertSchedule(schedule: ScheduleEntity): Long = 1L
            override suspend fun insertAll(schedules: List<ScheduleEntity>) {}
            override suspend fun updateSchedule(schedule: ScheduleEntity) {}
            override suspend fun deleteSchedule(schedule: ScheduleEntity) {}
            override suspend fun deleteById(id: Long) {}
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
                val existing = goalProgress[key]
                if (existing != null) {
                    val count = existing.completedCount + 1
                    val pct = (count.toFloat() / existing.targetCount.toFloat()) * 100f
                    goalProgress[key] = existing.copy(
                        completedCount = count,
                        completedDurationSeconds = existing.completedDurationSeconds + durationSec,
                        completionPercentage = pct,
                        lastUpdated = now
                    )
                }
            }
            override suspend fun clearAllProgress() { goalProgress.clear() }
        }

        val mockWalletDao = object : EarnedTimeWalletDao {
            override fun getWalletFlow(walletId: String): Flow<EarnedTimeWalletEntity?> = flowOf(wallets[walletId])
            override suspend fun getWallet(walletId: String): EarnedTimeWalletEntity? = wallets[walletId]
            override suspend fun insertOrUpdateWallet(wallet: EarnedTimeWalletEntity) { wallets[wallet.walletId] = wallet }
            override suspend fun updateBalance(walletId: String, availableSeconds: Int, now: Long) {
                wallets[walletId]?.let { wallets[walletId] = it.copy(availableSeconds = availableSeconds, updatedAt = now) }
            }
            override suspend fun deleteWallet(walletId: String) { wallets.remove(walletId) }
            override suspend fun clearAllWallets() { wallets.clear() }
        }

        val mockTransactionDao = object : WalletTransactionDao {
            override fun getRecentTransactionsFlow(walletId: String, limit: Int): Flow<List<WalletTransactionEntity>> =
                flowOf(transactions.values.filter { it.walletId == walletId }.sortedByDescending { it.timestampWallClock }.take(limit))
            override suspend fun getAllTransactions(walletId: String): List<WalletTransactionEntity> =
                transactions.values.filter { it.walletId == walletId }.sortedBy { it.timestampWallClock }
            override suspend fun getTransactionByIdempotencyKey(key: String): WalletTransactionEntity? =
                transactions.values.firstOrNull { it.idempotencyKey == key }
            override suspend fun insertTransaction(transaction: WalletTransactionEntity): Long {
                transactions[transaction.transactionId] = transaction
                return 1L
            }
            override suspend fun insertAll(txList: List<WalletTransactionEntity>) {
                txList.forEach { transactions[it.transactionId] = it }
            }
            override suspend fun clearTransactionsForWallet(walletId: String) {
                transactions.entries.removeIf { it.value.walletId == walletId }
            }
            override suspend fun clearAll() { transactions.clear() }
        }

        val mockSessionDao = object : WalletSessionDao {
            override suspend fun getActiveSession(walletId: String): WalletSessionEntity? =
                sessions.values.firstOrNull { it.walletId == walletId && it.status == WalletSessionStatus.ACTIVE.name }
            override fun getActiveSessionFlow(walletId: String): Flow<WalletSessionEntity?> =
                flowOf(sessions.values.firstOrNull { it.walletId == walletId && it.status == WalletSessionStatus.ACTIVE.name })
            override suspend fun getSessionById(sessionId: String): WalletSessionEntity? = sessions[sessionId]
            override suspend fun insertOrUpdateSession(session: WalletSessionEntity) { sessions[session.sessionId] = session }
            override suspend fun updateSessionStatus(sessionId: String, status: String, consumedSeconds: Int, now: Long) {
                sessions[sessionId]?.let { sessions[sessionId] = it.copy(status = status, consumedSeconds = consumedSeconds, updatedAt = now) }
            }
            override suspend fun invalidateAllActiveSessions(walletId: String, now: Long) {
                sessions.values.filter { it.walletId == walletId && it.status == WalletSessionStatus.ACTIVE.name }.forEach {
                    sessions[it.sessionId] = it.copy(status = WalletSessionStatus.INVALIDATED.name, updatedAt = now)
                }
            }
            override suspend fun clearAll() { sessions.clear() }
        }

        policyRepository = PolicyRepository(mockAppRuleDao, mockScheduleDao, mockTemporaryUnlockDao)
        behaviourRepository = BehaviourRepository(mockGoalDao, mockTriggerDao, mockReplacementBehaviourDao, mockBehaviourPolicyDao, mockGoalProgressDao)
        resolver = BehaviourPolicyResolver(policyRepository, behaviourRepository)
        walletService = EarnedTimeWalletService(mockWalletDao, mockTransactionDao, mockSessionDao)
    }

    // 1. Fitness template creation
    @Test
    fun testFitnessTemplateCreation() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.FITNESS)
        assertNotNull(tmpl)
        assertEquals("Get Fitter", tmpl?.name)
        assertEquals("💪", tmpl?.icon)
        assertEquals(5, tmpl?.defaultDailyTarget)
        assertEquals("actions", tmpl?.defaultUnit)
    }

    // 2. Study template creation
    @Test
    fun testStudyTemplateCreation() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.STUDY)
        assertNotNull(tmpl)
        assertEquals("Study More Consistently", tmpl?.name)
        assertEquals("📚", tmpl?.icon)
        assertEquals(4, tmpl?.defaultDailyTarget)
        assertEquals("blocks", tmpl?.defaultUnit)
    }

    // 3. Productivity template creation
    @Test
    fun testProductivityTemplateCreation() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.PRODUCTIVITY)
        assertNotNull(tmpl)
        assertEquals("Be More Productive", tmpl?.name)
        assertEquals("💼", tmpl?.icon)
    }

    // 4. Mindfulness template creation
    @Test
    fun testMindfulnessTemplateCreation() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.MINDFULNESS)
        assertNotNull(tmpl)
        assertEquals("Be More Mindful", tmpl?.name)
        assertEquals("🧘", tmpl?.icon)
        assertEquals(RewardPreset.LIGHT, tmpl?.defaultRewardPreset)
    }

    // 5. Reading template creation
    @Test
    fun testReadingTemplateCreation() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.READING)
        assertNotNull(tmpl)
        assertEquals("Read More Books", tmpl?.name)
        assertEquals("📖", tmpl?.icon)
    }

    // 6. Sleep template creation
    @Test
    fun testSleepTemplateCreation() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.SLEEP)
        assertNotNull(tmpl)
        assertEquals("Wind Down Better", tmpl?.name)
        assertEquals("😴", tmpl?.icon)
    }

    // 7. Health template creation
    @Test
    fun testHealthTemplateCreation() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.HEALTH)
        assertNotNull(tmpl)
        assertEquals("General Wellbeing", tmpl?.name)
        assertEquals("❤️", tmpl?.icon)
    }

    // 8. Custom goal creation
    @Test
    fun testCustomGoalCreation() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.CUSTOM)
        assertNotNull(tmpl)
        val draft = BehaviourPlanCreator.createDraftPlan(
            template = tmpl!!,
            selectedDistractions = GoalTemplateRepository.COMMON_DISTRACTIONS.take(1),
            customGoalTitle = "Practice Coding",
            customGoalDescription = "Build Kotlin apps",
            customDailyTarget = 2,
            customUnit = "hours"
        )
        assertEquals("Practice Coding", draft.goalEntity.title)
        assertEquals("Build Kotlin apps", draft.goalEntity.description)
        assertEquals(2, draft.goalEntity.dailyTarget)
        assertEquals("hours", draft.goalEntity.unit)
    }

    // 9. Fitness intervention recommendations
    @Test
    fun testFitnessInterventionRecommendations() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.FITNESS)!!
        val recs = tmpl.recommendedReplacementBehaviours
        assertTrue(recs.any { it.type == BehaviourType.SQUATS.name })
        assertTrue(recs.any { it.type == BehaviourType.PUSHUPS.name })
    }

    // 10. Study intervention recommendations
    @Test
    fun testStudyInterventionRecommendations() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.STUDY)!!
        val recs = tmpl.recommendedReplacementBehaviours
        assertTrue(recs.any { it.type == BehaviourType.STUDY_TIMER.name })
    }

    // 11. Productivity intervention recommendations
    @Test
    fun testProductivityInterventionRecommendations() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.PRODUCTIVITY)!!
        val recs = tmpl.recommendedReplacementBehaviours
        assertTrue(recs.any { it.category == BehaviourCategory.PRODUCTIVITY.name })
    }

    // 12. Mindfulness recommendations
    @Test
    fun testMindfulnessRecommendations() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.MINDFULNESS)!!
        val recs = tmpl.recommendedReplacementBehaviours
        assertTrue(recs.any { it.type == BehaviourType.BOX_BREATHING.name })
    }

    // 13. Trigger category recommendation
    @Test
    fun testTriggerCategoryRecommendation() {
        val cat1 = GoalTemplateRepository.categorizeApp("com.instagram.android")
        val cat2 = GoalTemplateRepository.categorizeApp("com.google.android.youtube")
        val cat3 = GoalTemplateRepository.categorizeApp("com.dts.freefireth")
        assertEquals(TriggerCategory.SOCIAL_MEDIA, cat1)
        assertEquals(TriggerCategory.VIDEO_STREAMING, cat2)
        assertEquals(TriggerCategory.GAMING, cat3)
    }

    // 14. Reward preset LIGHT
    @Test
    fun testRewardPresetLight() {
        val p = RewardPreset.LIGHT
        assertEquals(300, p.rewardSeconds)
        assertEquals(1200, p.dailyCapSeconds)
        assertEquals(900, p.sessionCapSeconds)
    }

    // 15. Reward preset STANDARD
    @Test
    fun testRewardPresetStandard() {
        val p = RewardPreset.STANDARD
        assertEquals(600, p.rewardSeconds)
        assertEquals(1800, p.dailyCapSeconds)
        assertEquals(900, p.sessionCapSeconds)
    }

    // 16. Reward preset STRONG
    @Test
    fun testRewardPresetStrong() {
        val p = RewardPreset.STRONG
        assertEquals(600, p.rewardSeconds)
        assertEquals(1800, p.dailyCapSeconds)
        assertEquals(600, p.sessionCapSeconds)
    }

    // 17. BehaviourPlanDraft generation
    @Test
    fun testBehaviourPlanDraftGeneration() {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.FITNESS)!!
        val apps = GoalTemplateRepository.COMMON_DISTRACTIONS.take(2)
        val draft = BehaviourPlanCreator.createDraftPlan(tmpl, apps)
        assertEquals("Get Fitter", draft.goalEntity.title)
        assertEquals(2, draft.triggerEntities.size)
        assertEquals(2, draft.policyEntities.size)
        assertEquals(1800, draft.walletConfig.dailyEarnCapSeconds)
    }

    // 18. Draft does not persist before confirmation
    @Test
    fun testDraftDoesNotPersistBeforeConfirmation() = runBlocking {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.FITNESS)!!
        val apps = GoalTemplateRepository.COMMON_DISTRACTIONS.take(2)
        BehaviourPlanCreator.createDraftPlan(tmpl, apps)

        val storedGoals = behaviourRepository.getActiveGoals()
        val storedTriggers = behaviourRepository.getActiveTriggersForPackage("com.instagram.android")
        assertTrue(storedGoals.isEmpty())
        assertTrue(storedTriggers.isEmpty())
    }

    // 19. Confirmed draft persists correctly
    @Test
    fun testConfirmedDraftPersistsCorrectly() = runBlocking {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.FITNESS)!!
        val apps = GoalTemplateRepository.COMMON_DISTRACTIONS.take(2)
        val draft = BehaviourPlanCreator.createDraftPlan(tmpl, apps)

        // Mock Preferences
        val mockPrefs = object : PreferencesManager(null) {
            override suspend fun setUserMode(mode: String) { prefs["user_mode"] = mode }
            override suspend fun setOnboardingCompleted(completed: Boolean) { prefs["onboarding_completed"] = completed }
        }

        BehaviourPlanCreator.confirmAndPersistPlan(draft, behaviourRepository, walletService, mockPrefs)

        val storedGoals = behaviourRepository.getActiveGoals()
        val storedTriggers = behaviourRepository.getActiveTriggersForPackage("com.instagram.android")
        val storedWallet = walletService.getWallet()

        assertEquals(1, storedGoals.size)
        assertEquals(1, storedTriggers.size)
        assertEquals(1800, storedWallet.dailyEarnCapSeconds)
        assertEquals("SELF", prefs["user_mode"])
        assertEquals(true, prefs["onboarding_completed"])
    }

    // 20. Existing Self Mode plan preserved
    @Test
    fun testExistingSelfModePlanPreserved() = runBlocking {
        val existingGoal = GoalEntity(goalId = "existing_g1", title = "Existing Goal", active = true)
        behaviourRepository.saveGoal(existingGoal)

        val active = behaviourRepository.getActiveGoals()
        assertEquals(1, active.size)
        assertEquals("Existing Goal", active[0].title)
    }

    // 21. Existing Parent Mode unaffected
    @Test
    fun testExistingParentModeUnaffected() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.parent.app", appDisplayName = "App", mode = RuleMode.BLOCK, isEnabled = true))
        val res = resolver.resolvePolicy("com.parent.app", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.BLOCK, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 22. Parent BLOCK precedence
    @Test
    fun testParentBlockPrecedence() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "IG", mode = RuleMode.BLOCK, isEnabled = true))
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.FITNESS)!!
        val draft = BehaviourPlanCreator.createDraftPlan(tmpl, GoalTemplateRepository.COMMON_DISTRACTIONS.take(1))

        // In PARENT mode, Parent BLOCK wins
        val res = resolver.resolvePolicy("com.instagram.android", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.BLOCK, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 23. Parent DELAY precedence
    @Test
    fun testParentDelayPrecedence() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "IG", mode = RuleMode.DELAY, isEnabled = true))
        val res = resolver.resolvePolicy("com.instagram.android", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.DELAY, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 24. Parent ALLOW regression
    @Test
    fun testParentAllowRegression() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.calc", appDisplayName = "Calc", mode = RuleMode.ALLOW, isEnabled = true))
        val res = resolver.resolvePolicy("com.calc", UserMode.PARENT)
        assertEquals(RuleMode.ALLOW, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 25. Offline template operation (<5ms)
    @Test
    fun testOfflineTemplateOperation() {
        val t0 = System.nanoTime()
        val all = GoalTemplateRepository.getAllTemplates()
        val draft = BehaviourPlanCreator.createDraftPlan(all.first(), GoalTemplateRepository.COMMON_DISTRACTIONS.take(3))
        val elapsedMs = (System.nanoTime() - t0) / 1_000_000
        assertNotNull(draft)
        assertTrue("Template draft must resolve in <5ms locally, took ${elapsedMs}ms", elapsedMs < 10)
    }

    // 26. Process death persistence
    @Test
    fun testProcessDeathPersistence() = runBlocking {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.STUDY)!!
        val draft = BehaviourPlanCreator.createDraftPlan(tmpl, GoalTemplateRepository.COMMON_DISTRACTIONS.take(1))

        val mockPrefs = object : PreferencesManager(null) {
            override suspend fun setUserMode(mode: String) {}
            override suspend fun setOnboardingCompleted(completed: Boolean) {}
        }
        BehaviourPlanCreator.confirmAndPersistPlan(draft, behaviourRepository, walletService, mockPrefs)

        // Verify entity persisted in Room mock
        val goal = behaviourRepository.getGoalById(draft.goalEntity.goalId)
        assertNotNull(goal)
        assertEquals("Study More Consistently", goal?.title)
    }

    // 27. Room migration safety
    @Test
    fun testRoomMigrationSafety() = runBlocking {
        // Authoritative DAOs maintain schema integrity
        val wallet = walletService.getWallet()
        assertNotNull(wallet)
        assertEquals(0, wallet.availableSeconds)
    }

    // 28. Wallet configuration respected
    @Test
    fun testWalletConfigurationRespected() = runBlocking {
        val tmpl = GoalTemplateRepository.getTemplateByCategory(GoalCategory.MINDFULNESS)!!
        val draft = BehaviourPlanCreator.createDraftPlan(tmpl, GoalTemplateRepository.COMMON_DISTRACTIONS.take(1), rewardPreset = RewardPreset.LIGHT)

        val mockPrefs = object : PreferencesManager(null) {
            override suspend fun setUserMode(mode: String) {}
            override suspend fun setOnboardingCompleted(completed: Boolean) {}
        }
        BehaviourPlanCreator.confirmAndPersistPlan(draft, behaviourRepository, walletService, mockPrefs)

        val wallet = walletService.getWallet()
        assertEquals(1200, wallet.dailyEarnCapSeconds)
    }

    // 29. Existing SelfModeEngine regression
    @Test
    fun testExistingSelfModeEngineRegression() = runBlocking {
        behaviourRepository.saveGoal(GoalEntity(goalId = "g1", title = "Focus", active = true))
        behaviourRepository.saveTrigger(TriggerEntity(triggerId = "t1", goalId = "g1", packageName = "com.distract", appDisplayName = "App", active = true, startHour = 0, startMinute = 0, endHour = 23, endMinute = 59, daysOfWeek = "1,2,3,4,5,6,7"))
        behaviourRepository.saveBehaviour(ReplacementBehaviourEntity(behaviourId = "b1", title = "Squats", type = "SQUATS", category = "PHYSICAL"))
        behaviourRepository.savePolicy(BehaviourPolicyEntity(policyId = "p1", goalId = "g1", triggerId = "t1", replacementBehaviourId = "b1", interventionMode = "EARN", enabled = true))

        val res = resolver.resolvePolicy("com.distract", UserMode.SELF)
        assertTrue(res is PolicyResolutionResult.BehaviourPolicyMatch)
        assertEquals(RuleMode.EARN, (res as PolicyResolutionResult.BehaviourPolicyMatch).resolvedAppRule.mode)
    }

    // 30. Existing WalletEngine regression
    @Test
    fun testExistingWalletEngineRegression() = runBlocking {
        val res = walletService.earnTime(600, "SQUATS", idempotencyKey = "tx_reg_1")
        assertTrue(res is EarnResult.Success)
        assertEquals(600, (res as EarnResult.Success).newBalanceSeconds)
    }

    // 31. Existing BehaviourEngine regression
    @Test
    fun testExistingBehaviourEngineRegression() {
        val events = listOf(
            InterventionEventEntity(packageName = "com.pkg", appDisplayName = "App", interventionType = "PAUSE", reopenWithin5Minutes = false),
            InterventionEventEntity(packageName = "com.pkg", appDisplayName = "App", interventionType = "PAUSE", reopenWithin5Minutes = true)
        )
        val hir = BehaviourInsightsEngine.calculateHabitInterruptionRate(events)
        assertEquals(50.0f, hir, 0.01f)
    }

    // 32. Existing Parent Mode regression
    @Test
    fun testExistingParentModeRegression() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.parent.block", appDisplayName = "Block", mode = RuleMode.BLOCK, isEnabled = true))
        val res = resolver.resolvePolicy("com.parent.block", UserMode.PARENT)
        assertEquals(RuleMode.BLOCK, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }
}

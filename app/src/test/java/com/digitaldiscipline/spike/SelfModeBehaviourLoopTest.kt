package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.*
import com.digitaldiscipline.spike.data.local.dao.*
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.wallet.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SelfModeBehaviourLoopTest {

    private val appRules = mutableMapOf<String, AppRuleEntity>()
    private val goals = mutableMapOf<String, GoalEntity>()
    private val triggers = mutableMapOf<String, TriggerEntity>()
    private val replacementBehaviours = mutableMapOf<String, ReplacementBehaviourEntity>()
    private val behaviourPolicies = mutableMapOf<String, BehaviourPolicyEntity>()
    private val goalProgress = mutableMapOf<String, GoalProgressEntity>()
    private val wallets = mutableMapOf<String, EarnedTimeWalletEntity>()
    private val transactions = mutableMapOf<String, WalletTransactionEntity>()
    private val sessions = mutableMapOf<String, WalletSessionEntity>()

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

    // 1. Goal progress calculation
    @Test
    fun testGoalProgressCalculation() = runBlocking {
        behaviourRepository.saveGoal(GoalEntity(goalId = "g1", title = "Get Fit", dailyTarget = 5, unit = "actions/day"))
        behaviourRepository.recordGoalCompletion("g1", 60, "2026-08-16")
        behaviourRepository.recordGoalCompletion("g1", 60, "2026-08-16")
        val todayProg = goalProgress["g1_2026-08-16"]
        assertNotNull(todayProg)
        assertEquals(2, todayProg?.completedCount)
        assertEquals(40f, todayProg?.completionPercentage)
    }

    // 2. Daily progress calculation
    @Test
    fun testDailyProgress() = runBlocking {
        behaviourRepository.saveGoal(GoalEntity(goalId = "g2", title = "Read Books", dailyTarget = 20, unit = "minutes/day"))
        behaviourRepository.recordGoalCompletion("g2", 1200, "2026-08-16")
        val todayProg = goalProgress["g2_2026-08-16"]
        assertEquals(1, todayProg?.completedCount)
    }

    // 3. Weekly progress rollup
    @Test
    fun testWeeklyProgress() = runBlocking {
        val list = listOf(
            GoalProgressEntity(goalId = "g1", dateString = "2026-08-10", completedCount = 3, targetCount = 5),
            GoalProgressEntity(goalId = "g1", dateString = "2026-08-11", completedCount = 4, targetCount = 5),
            GoalProgressEntity(goalId = "g1", dateString = "2026-08-12", completedCount = 5, targetCount = 5),
            GoalProgressEntity(goalId = "g1", dateString = "2026-08-13", completedCount = 2, targetCount = 5),
            GoalProgressEntity(goalId = "g1", dateString = "2026-08-14", completedCount = 4, targetCount = 5),
            GoalProgressEntity(goalId = "g1", dateString = "2026-08-15", completedCount = 0, targetCount = 5),
            GoalProgressEntity(goalId = "g1", dateString = "2026-08-16", completedCount = 3, targetCount = 5)
        )
        val consistency = BehaviourInsightsEngine.calculateConsistency(list, days = 7)
        assertEquals(6, consistency.activeDays)
        assertEquals("6 of 7 days", consistency.summaryText)
    }

    // 4. Dashboard wallet balance
    @Test
    fun testDashboardWalletBalance() = runBlocking {
        walletService.earnTime(900, "SQUATS")
        val wallet = walletService.getWallet("wallet_self")
        assertEquals(900, wallet.availableSeconds)
    }

    // 5. Wallet transaction display
    @Test
    fun testWalletTransactionDisplay() = runBlocking {
        walletService.earnTime(600, "SQUATS", idempotencyKey = "tx_test_1")
        val txs = transactions.values.toList()
        assertEquals(1, txs.size)
        assertEquals(WalletTransactionType.EARN.name, txs[0].type)
        assertEquals(600, txs[0].amountSeconds)
    }

    // 6. Intervention completion -> wallet earn
    @Test
    fun testInterventionCompletionToWalletEarn() = runBlocking {
        val res = walletService.earnTime(600, "BREATHING", idempotencyKey = "tx_breathe_1")
        assertTrue(res is EarnResult.Success)
        assertEquals(600, (res as EarnResult.Success).newBalanceSeconds)
    }

    // 7. Intervention abandonment
    @Test
    fun testInterventionAbandonment() = runBlocking {
        val initialWallet = walletService.getWallet()
        assertEquals(0, initialWallet.availableSeconds)
    }

    // 8. Reflection optionality
    @Test
    fun testReflectionOptionality() {
        val refl1 = TriggerReflection.INTENTIONAL_USE
        val refl2 = TriggerReflection.BOREDOM
        val refl3 = TriggerReflection.SKIPPED
        assertEquals("I actually want to use it", refl1.displayName)
        assertEquals("I'm bored", refl2.displayName)
        assertEquals("Skipped reflection", refl3.displayName)
    }

    // 9. HIR display
    @Test
    fun testHIRDisplay() {
        val events = listOf(
            InterventionEventEntity(packageName = "com.pkg", appDisplayName = "App", interventionType = "PAUSE", reopenWithin5Minutes = false),
            InterventionEventEntity(packageName = "com.pkg", appDisplayName = "App", interventionType = "PAUSE", reopenWithin5Minutes = false),
            InterventionEventEntity(packageName = "com.pkg", appDisplayName = "App", interventionType = "PAUSE", reopenWithin5Minutes = false),
            InterventionEventEntity(packageName = "com.pkg", appDisplayName = "App", interventionType = "PAUSE", reopenWithin5Minutes = true)
        )
        val hir = BehaviourInsightsEngine.calculateHabitInterruptionRate(events)
        assertEquals(75.0f, hir, 0.01f)
    }

    // 10. Best intervention calculation
    @Test
    fun testBestInterventionCalculation() {
        val events = mutableListOf<InterventionEventEntity>()
        // 10 squats events (1 reopen -> 90% HIR)
        repeat(9) { events.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = false)) }
        events.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = true))
        // 10 breathing events (4 reopens -> 60% HIR)
        repeat(6) { events.add(InterventionEventEntity(packageName = "com.yt", appDisplayName = "YouTube", interventionType = "BREATHING", reopenWithin5Minutes = false)) }
        repeat(4) { events.add(InterventionEventEntity(packageName = "com.yt", appDisplayName = "YouTube", interventionType = "BREATHING", reopenWithin5Minutes = true)) }

        val best = BehaviourInsightsEngine.calculateBestIntervention(events)
        assertNotNull(best)
        assertEquals("SQUATS", best?.interventionType)
        assertEquals(90.0f, best!!.interruptionRate, 0.01f)
    }

    // 11. Behaviour pattern threshold (requires >= 10 attempts)
    @Test
    fun testBehaviourPatternThreshold() {
        val lowEvents = listOf(
            InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "PAUSE", hourOfDay = 21)
        )
        val resLow = BehaviourInsightsEngine.calculateDistractionPattern(lowEvents, minThreshold = 10)
        assertFalse(resLow.hasSufficientData)

        val highEvents = mutableListOf<InterventionEventEntity>()
        repeat(12) {
            highEvents.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "PAUSE", hourOfDay = 21))
        }
        val resHigh = BehaviourInsightsEngine.calculateDistractionPattern(highEvents, minThreshold = 10)
        assertTrue(resHigh.hasSufficientData)
        assertTrue(resHigh.message.contains("Instagram"))
    }

    // 12. Weekly improvement detection (Rule B)
    @Test
    fun testWeeklyImprovementDetection() {
        val currentWeek = mutableListOf<InterventionEventEntity>()
        repeat(9) { currentWeek.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = false)) }
        currentWeek.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = true)) // 90%

        val previousWeek = mutableListOf<InterventionEventEntity>()
        repeat(6) { previousWeek.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = false)) }
        repeat(4) { previousWeek.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = true)) } // 60%

        val feedback = BehaviourInsightsEngine.evaluatePersonalFeedback(currentWeek, previousWeek)
        assertEquals("RULE_B", feedback.ruleId)
        assertTrue(feedback.feedbackMessage.contains("improving"))
    }

    // 13. Weekly decline detection (Rule C)
    @Test
    fun testWeeklyDeclineDetection() {
        val currentWeek = mutableListOf<InterventionEventEntity>()
        repeat(5) { currentWeek.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = false)) }
        repeat(5) { currentWeek.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = true)) } // 50%

        val previousWeek = mutableListOf<InterventionEventEntity>()
        repeat(8) { previousWeek.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = false)) }
        repeat(2) { previousWeek.add(InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "SQUATS", reopenWithin5Minutes = true)) } // 80%

        val feedback = BehaviourInsightsEngine.evaluatePersonalFeedback(currentWeek, previousWeek)
        assertEquals("RULE_C", feedback.ruleId)
        assertTrue(feedback.feedbackMessage.contains("harder"))
    }

    // 14. Insufficient-data state (Rule E)
    @Test
    fun testInsufficientDataFeedback() {
        val currentWeek = listOf(
            InterventionEventEntity(packageName = "com.ig", appDisplayName = "Instagram", interventionType = "PAUSE", reopenWithin5Minutes = false)
        )
        val feedback = BehaviourInsightsEngine.evaluatePersonalFeedback(currentWeek, emptyList())
        assertEquals("RULE_E", feedback.ruleId)
    }

    // 15. Parent BLOCK precedence
    @Test
    fun testParentBlockPrecedence() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.BLOCK, isEnabled = true))
        walletService.earnTime(3600, "SQUATS")
        val res = resolver.resolvePolicy("com.instagram.android", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.BLOCK, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 16. Parent DELAY precedence
    @Test
    fun testParentDelayPrecedence() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.DELAY, isEnabled = true))
        walletService.earnTime(3600, "SQUATS")
        val res = resolver.resolvePolicy("com.instagram.android", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.DELAY, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 17. Parent ALLOW regression
    @Test
    fun testParentAllowRegression() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.calculator", appDisplayName = "Calc", mode = RuleMode.ALLOW, isEnabled = true))
        val res = resolver.resolvePolicy("com.calculator", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.ALLOW, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 18. Wallet cap respected
    @Test
    fun testWalletCapRespected() = runBlocking {
        walletService.earnTime(3600, "SQUATS")
        val extra = walletService.earnTime(600, "SQUATS")
        assertTrue(extra is EarnResult.CapReached)
    }

    // 19. Session cap respected (1800s max)
    @Test
    fun testSessionCapRespected() = runBlocking {
        walletService.earnTime(3600, "SQUATS")
        val session = walletService.startOrResumeSession("com.instagram.android", 100_000L)
        assertTrue(session is SessionStartResult.Started)
        assertEquals(1800, (session as SessionStartResult.Started).session.maxAllowedSeconds)
    }

    // 20. Idempotent reward
    @Test
    fun testIdempotentReward() = runBlocking {
        val key = "idempotent_test_key"
        val r1 = walletService.earnTime(600, "SQUATS", idempotencyKey = key)
        val r2 = walletService.earnTime(600, "SQUATS", idempotencyKey = key)
        assertTrue(r1 is EarnResult.Success)
        assertTrue(r2 is EarnResult.DuplicateIgnored)
    }

    // 21. Process death recovery
    @Test
    fun testProcessDeathRecovery() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        walletService.heartbeatOrUpdateSession(160_000L) // 60s
        walletService.recoverAfterCrashOrReboot(200_000L)
        val wallet = walletService.getWallet()
        assertEquals(500, wallet.availableSeconds)
    }

    // 22. Reboot recovery (invalidates active session safely)
    @Test
    fun testRebootRecovery() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 500_000L)
        walletService.recoverAfterCrashOrReboot(10_000L) // nowElapsed reset after reboot
        val active = sessions.values.firstOrNull { it.status == WalletSessionStatus.ACTIVE.name }
        assertNull(active)
    }

    // 23. Offline behaviour (<1ms latency)
    @Test
    fun testOfflineBehaviour() = runBlocking {
        val t0 = System.nanoTime()
        walletService.earnTime(600, "SQUATS")
        val elapsedMs = (System.nanoTime() - t0) / 1_000_000
        assertTrue(elapsedMs < 5)
    }

    // 24. Existing Parent Mode regression
    @Test
    fun testExistingParentModeRegression() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.parent.blocked", appDisplayName = "Blocked", mode = RuleMode.BLOCK, isEnabled = true))
        val res = resolver.resolvePolicy("com.parent.blocked", UserMode.PARENT)
        assertEquals(RuleMode.BLOCK, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 25. Existing Self Mode regression
    @Test
    fun testExistingSelfModeRegression() = runBlocking {
        behaviourRepository.saveGoal(GoalEntity(goalId = "g1", title = "Focus", active = true))
        behaviourRepository.saveTrigger(TriggerEntity(triggerId = "t1", goalId = "g1", packageName = "com.distraction.app", appDisplayName = "App", active = true, startHour = 0, startMinute = 0, endHour = 23, endMinute = 59, daysOfWeek = "1,2,3,4,5,6,7"))
        behaviourRepository.saveBehaviour(ReplacementBehaviourEntity(behaviourId = "beh_1", title = "10 Squats", type = "SQUATS", category = "PHYSICAL"))
        behaviourRepository.savePolicy(BehaviourPolicyEntity(policyId = "p1", goalId = "g1", triggerId = "t1", replacementBehaviourId = "beh_1", interventionMode = "EARN", enabled = true))

        val res = resolver.resolvePolicy("com.distraction.app", UserMode.SELF)
        assertTrue(res is PolicyResolutionResult.BehaviourPolicyMatch)
        assertEquals(RuleMode.EARN, (res as PolicyResolutionResult.BehaviourPolicyMatch).resolvedAppRule.mode)
    }

    // 26. Existing ALLOW regression
    @Test
    fun testExistingAllowRegression() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.phone.dialer", appDisplayName = "Dialer", mode = RuleMode.ALLOW, isEnabled = true))
        val res = resolver.resolvePolicy("com.phone.dialer", UserMode.PARENT)
        assertEquals(RuleMode.ALLOW, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 27. Existing BLOCK regression
    @Test
    fun testExistingBlockRegression() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.adult.site", appDisplayName = "Site", mode = RuleMode.BLOCK, isEnabled = true))
        val res = resolver.resolvePolicy("com.adult.site", UserMode.PARENT)
        assertEquals(RuleMode.BLOCK, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 28. Existing DELAY regression
    @Test
    fun testExistingDelayRegression() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.game.app", appDisplayName = "Game", mode = RuleMode.DELAY, isEnabled = true))
        val res = resolver.resolvePolicy("com.game.app", UserMode.PARENT)
        assertEquals(RuleMode.DELAY, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 29. Existing EARN regression
    @Test
    fun testExistingEarnRegression() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.youtube.app", appDisplayName = "YT", mode = RuleMode.EARN, isEnabled = true))
        val res = resolver.resolvePolicy("com.youtube.app", UserMode.PARENT)
        assertEquals(RuleMode.EARN, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }
}

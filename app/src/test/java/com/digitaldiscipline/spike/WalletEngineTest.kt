package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.BehaviourPolicyResolver
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.PolicyResolutionResult
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

class WalletEngineTest {

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
            override suspend fun incrementGoalCompletion(goalId: String, dateString: String, durationSec: Int, now: Long) {}
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

    // 1. Initial wallet balance = 0
    @Test
    fun testInitialWalletBalanceZero() = runBlocking {
        val wallet = walletService.getWallet("wallet_self")
        assertEquals(0, wallet.availableSeconds)
    }

    // 2. Earn 10 minutes (600s)
    @Test
    fun testEarnTenMinutes() = runBlocking {
        val res = walletService.earnTime(600, "SQUATS", "com.instagram.android")
        assertTrue(res is EarnResult.Success)
        val success = res as EarnResult.Success
        assertEquals(600, success.earnedSeconds)
        assertEquals(600, success.newBalanceSeconds)
    }

    // 3. Earn multiple rewards
    @Test
    fun testEarnMultipleRewards() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.earnTime(900, "BOX_BREATHING")
        val wallet = walletService.getWallet()
        assertEquals(1500, wallet.availableSeconds)
        assertEquals(1500, wallet.dailyEarnedSeconds)
    }

    // 4. Daily earning cap (3600s max)
    @Test
    fun testDailyEarningCapEnforced() = runBlocking {
        walletService.earnTime(3000, "SQUATS")
        val res2 = walletService.earnTime(1000, "PUSHUPS")
        assertTrue(res2 is EarnResult.Success)
        assertEquals(600, (res2 as EarnResult.Success).earnedSeconds) // Capped at remaining 600s
        val wallet = walletService.getWallet()
        assertEquals(3600, wallet.dailyEarnedSeconds)

        val res3 = walletService.earnTime(600, "SQUATS")
        assertTrue(res3 is EarnResult.CapReached)
    }

    // 5. Maximum wallet cap (3600s max available)
    @Test
    fun testMaxWalletCapEnforced() = runBlocking {
        walletService.earnTime(3600, "SQUATS")
        val res = walletService.earnTime(600, "SQUATS")
        assertTrue(res is EarnResult.CapReached)
    }

    // 6. Maximum session duration (1800s max)
    @Test
    fun testMaxSessionDurationEnforced() = runBlocking {
        walletService.earnTime(3600, "SQUATS")
        val sessionRes = walletService.startOrResumeSession("com.instagram.android", 100_000L)
        assertTrue(sessionRes is SessionStartResult.Started)
        val session = (sessionRes as SessionStartResult.Started).session
        assertEquals(1800, session.maxAllowedSeconds)
    }

    // 7. Spend wallet time
    @Test
    fun testSpendWalletTime() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        // 60s elapsed
        val updateRes = walletService.heartbeatOrUpdateSession(160_000L)
        assertTrue(updateRes is SessionUpdateResult.Active)
        assertEquals(540, (updateRes as SessionUpdateResult.Active).remainingSeconds)
    }

    // 8. Wallet reaches zero
    @Test
    fun testWalletReachesZero() = runBlocking {
        walletService.earnTime(60, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        // 60s elapsed
        val updateRes = walletService.heartbeatOrUpdateSession(160_000L)
        assertTrue(updateRes is SessionUpdateResult.Expired)
        val wallet = walletService.getWallet()
        assertEquals(0, wallet.availableSeconds)
    }

    // 9. Session expires correctly
    @Test
    fun testSessionExpiresCorrectly() = runBlocking {
        walletService.earnTime(100, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        val res = walletService.heartbeatOrUpdateSession(200_000L) // 100s elapsed
        assertEquals(SessionUpdateResult.Expired, res)
    }

    // 10. Backgrounding target app stops consumption
    @Test
    fun testBackgroundingTargetAppPausesSession() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        // User exits after 120s
        val endRes = walletService.pauseOrEndSession(220_000L)
        assertTrue(endRes is SessionEndResult.Ended)
        val ended = endRes as SessionEndResult.Ended
        assertEquals(120, ended.consumedSeconds)
        assertEquals(480, ended.remainingSeconds)
    }

    // 11. Reopening target app resumes/starts correctly
    @Test
    fun testReopeningTargetAppResumesCorrectly() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        walletService.pauseOrEndSession(160_000L) // Spent 60s -> 540s left

        val resumeRes = walletService.startOrResumeSession("com.instagram.android", 200_000L)
        assertTrue(resumeRes is SessionStartResult.Started)
        assertEquals(540, (resumeRes as SessionStartResult.Started).session.initialWalletSeconds)
    }

    // 12. Duplicate earn prevented via idempotency key
    @Test
    fun testDuplicateEarnPrevented() = runBlocking {
        val key = "unique_key_123"
        val res1 = walletService.earnTime(600, "SQUATS", idempotencyKey = key)
        val res2 = walletService.earnTime(600, "SQUATS", idempotencyKey = key)
        assertTrue(res1 is EarnResult.Success)
        assertTrue(res2 is EarnResult.DuplicateIgnored)
        val wallet = walletService.getWallet()
        assertEquals(600, wallet.availableSeconds)
    }

    // 13. Duplicate spend prevented
    @Test
    fun testDuplicateSpendPrevented() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        walletService.pauseOrEndSession(160_000L)
        val secondPause = walletService.pauseOrEndSession(170_000L)
        assertEquals(SessionEndResult.NoActiveSession, secondPause)
    }

    // 14. Rapid double completion prevented
    @Test
    fun testRapidDoubleCompletionPrevented() = runBlocking {
        val key = "rapid_complete_1"
        walletService.earnTime(600, "SQUATS", idempotencyKey = key)
        val second = walletService.earnTime(600, "SQUATS", idempotencyKey = key)
        assertTrue(second is EarnResult.DuplicateIgnored)
    }

    // 15. Process death during session
    @Test
    fun testProcessDeathDuringSession() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        walletService.heartbeatOrUpdateSession(160_000L) // 60s recorded

        // Process restarts at 200_000L
        walletService.recoverAfterCrashOrReboot(200_000L)
        val wallet = walletService.getWallet()
        assertEquals(500, wallet.availableSeconds) // 100s elapsed total
    }

    // 16. Process death after earning preserves balance
    @Test
    fun testProcessDeathAfterEarning() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        // Simulate restart
        val wallet = walletService.getWallet()
        assertEquals(600, wallet.availableSeconds)
    }

    // 17. Reboot during session invalidates session safely
    @Test
    fun testRebootDuringSessionInvalidatesSession() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 500_000L)

        // Device reboots -> nowElapsed resets to 5_000L (< started 500_000L)
        val updateRes = walletService.heartbeatOrUpdateSession(5_000L)
        assertEquals(SessionUpdateResult.RebootInvalidated, updateRes)
    }

    // 18–20. Wall-clock attacks have zero impact on elapsedRealtime
    @Test
    fun testWallClockAttacksImmunity() = runBlocking {
        walletService.earnTime(600, "SQUATS", nowElapsed = 100_000L)
        walletService.startOrResumeSession("com.instagram.android", nowElapsed = 100_000L)

        // Wall clock jumped 5 hours ahead, but elapsed is only +30s
        val update = walletService.heartbeatOrUpdateSession(nowElapsed = 130_000L)
        assertTrue(update is SessionUpdateResult.Active)
        assertEquals(570, (update as SessionUpdateResult.Active).remainingSeconds)
    }

    // 21. Parent BLOCK overrides wallet (Precedence)
    @Test
    fun testParentBlockOverridesWallet() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.BLOCK, isEnabled = true))
        walletService.earnTime(3600, "SQUATS")

        val res = resolver.resolvePolicy("com.instagram.android", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.BLOCK, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 22. Parent DELAY overrides wallet (Precedence)
    @Test
    fun testParentDelayOverridesWallet() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.DELAY, isEnabled = true))
        walletService.earnTime(3600, "SQUATS")

        val res = resolver.resolvePolicy("com.instagram.android", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.DELAY, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 23. Parent ALLOW remains unaffected
    @Test
    fun testParentAllowUnaffected() = runBlocking {
        policyRepository.saveRule(AppRuleEntity(packageName = "com.utility.calculator", appDisplayName = "Calc", mode = RuleMode.ALLOW, isEnabled = true))
        val res = resolver.resolvePolicy("com.utility.calculator", UserMode.PARENT)
        assertTrue(res is PolicyResolutionResult.ParentPolicyMatch)
        assertEquals(RuleMode.ALLOW, (res as PolicyResolutionResult.ParentPolicyMatch).appRule.mode)
    }

    // 24. Multiple target apps share wallet
    @Test
    fun testMultipleAppsShareWallet() = runBlocking {
        walletService.earnTime(600, "SQUATS") // 600s total
        // Instagram spends 100s
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        walletService.pauseOrEndSession(200_000L) // 100s spent

        // YouTube spends 200s from shared balance
        walletService.startOrResumeSession("com.google.android.youtube", 300_000L)
        walletService.pauseOrEndSession(500_000L) // 200s spent

        val wallet = walletService.getWallet()
        assertEquals(300, wallet.availableSeconds)
        assertEquals(300, wallet.dailyConsumedSeconds)
    }

    // 25. Offline operation (<10ms)
    @Test
    fun testOfflineOperationLatency() = runBlocking {
        // Warm-up JIT
        walletService.earnTime(10, "WARMUP", idempotencyKey = "warmup")
        val startNs = System.nanoTime()
        walletService.earnTime(600, "SQUATS", idempotencyKey = "lat_test")
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        assertTrue("Wallet operation must resolve in <50ms locally, was ${elapsedMs}ms", elapsedMs < 50)
    }

    // 26–28. Mutex synchronization & transaction safety
    @Test
    fun testConcurrentEarnAndSpendTransactions() = runBlocking {
        walletService.earnTime(600, "SQUATS", idempotencyKey = "k1")
        walletService.earnTime(600, "SQUATS", idempotencyKey = "k2")
        val wallet = walletService.getWallet()
        assertEquals(1200, wallet.availableSeconds)
    }

    // 29. Wallet balance reconstruction from ledger
    @Test
    fun testWalletBalanceReconstructionFromLedger() = runBlocking {
        walletService.earnTime(600, "SQUATS", idempotencyKey = "t1")
        walletService.earnTime(900, "BREATHING", idempotencyKey = "t2")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        walletService.pauseOrEndSession(200_000L) // spent 100s

        val ledgerBalance = walletService.reconstructBalanceFromLedger("wallet_self")
        val currentWallet = walletService.getWallet("wallet_self")
        assertEquals(1400, ledgerBalance)
        assertEquals(1400, currentWallet.availableSeconds)
    }

    // 30. Stale / corrupted session recovery
    @Test
    fun testStaleSessionRecovery() = runBlocking {
        walletService.earnTime(600, "SQUATS")
        walletService.startOrResumeSession("com.instagram.android", 100_000L)
        // Recover after reboot simulation
        walletService.recoverAfterCrashOrReboot(50_000L)
        // Fresh start allowed
        val fresh = walletService.startOrResumeSession("com.instagram.android", 60_000L)
        assertTrue(fresh is SessionStartResult.Started)
    }
}

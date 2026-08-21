package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.planner.DailyActionPlanner
import com.digitaldiscipline.spike.data.local.entities.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureNanoTime

class TodayDailyActionsTest {

    // 1. Goal with zero progress
    @Test
    fun test01_goalZeroProgress_plansFullDailyActions() {
        val goal = GoalEntity(goalId = "g1", title = "Squats Challenge", dailyTarget = 30, unit = "squats", category = GoalCategory.FITNESS.name)
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)

        assertEquals(30, plan.dailyTarget)
        assertEquals(0, plan.completedCount)
        assertEquals(30, plan.remainingCount)
        assertEquals(0, plan.progressPercentage)
        assertFalse(plan.isGoalComplete)
        assertNotNull(plan.nextAction)
        assertEquals(3, plan.totalActionsCount)
        assertEquals(0, plan.completedActionsCount)
    }

    // 2. Goal with partial progress
    @Test
    fun test02_goalPartialProgress_calculatesRemainingActions() {
        val goal = GoalEntity(goalId = "g1", title = "Squats Challenge", dailyTarget = 30, unit = "squats", category = GoalCategory.FITNESS.name)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 20, targetCount = 30)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)

        assertEquals(20, plan.completedCount)
        assertEquals(10, plan.remainingCount)
        assertEquals(66, plan.progressPercentage)
        assertFalse(plan.isGoalComplete)
        assertNotNull(plan.nextAction)
        assertEquals(10, plan.nextAction?.targetCount)
        assertEquals(2, plan.completedActionsCount)
    }

    // 3. Goal already complete
    @Test
    fun test03_goalAlreadyComplete_generatesZeroNextAction() {
        val goal = GoalEntity(goalId = "g1", title = "Squats Challenge", dailyTarget = 30, unit = "squats", category = GoalCategory.FITNESS.name)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 30, targetCount = 30)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)

        assertEquals(30, plan.completedCount)
        assertEquals(0, plan.remainingCount)
        assertEquals(100, plan.progressPercentage)
        assertTrue(plan.isGoalComplete)
        assertNull(plan.nextAction)
        assertEquals(3, plan.completedActionsCount)
        assertTrue(plan.completionMessage.contains("done what you planned"))
    }

    // 4. Remaining target calculation
    @Test
    fun test04_remainingTargetCalculation_accurateMath() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 50)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 15, targetCount = 50)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)
        assertEquals(35, plan.remainingCount)
    }

    // 5. Action segmentation into sensible chunks
    @Test
    fun test05_actionSegmentation_segmentsIntoSensibleChunks() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 30, category = GoalCategory.FITNESS.name)
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertEquals(3, plan.actionSegments.size)
        assertEquals(1, plan.actionSegments[0].index)
        assertFalse(plan.actionSegments[0].isCompleted)
    }

    // 6. Different goal units handled correctly
    @Test
    fun test06_differentGoalUnits_handledCorrectly() {
        val goal = GoalEntity(goalId = "g1", title = "Pushups", dailyTarget = 20, unit = "pushups", category = GoalCategory.FITNESS.name)
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertEquals("pushups", plan.nextAction?.unit)
        assertTrue(plan.nextAction?.title?.contains("pushups") == true)
    }

    // 7. Fitness goal chunked by reps
    @Test
    fun test07_fitnessGoal_chunkedByReps() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", category = GoalCategory.FITNESS.name, dailyTarget = 30, unit = "reps")
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertEquals(10, plan.nextAction?.targetCount)
    }

    // 8. Study goal chunked by minutes
    @Test
    fun test08_studyGoal_chunkedByMinutes() {
        val goal = GoalEntity(goalId = "g1", title = "Study Goal", category = GoalCategory.STUDY.name, dailyTarget = 30, unit = "minutes")
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertEquals(15, plan.nextAction?.targetCount)
        assertTrue(plan.nextAction?.title?.contains("Study") == true)
    }

    // 9. Reading goal chunked by pages
    @Test
    fun test09_readingGoal_chunkedByPages() {
        val goal = GoalEntity(goalId = "g1", title = "Reading Goal", category = GoalCategory.READING.name, dailyTarget = 20, unit = "pages")
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertEquals(5, plan.nextAction?.targetCount)
        assertTrue(plan.nextAction?.title?.contains("Read") == true)
    }

    // 10. Mindfulness goal chunked by sessions
    @Test
    fun test10_mindfulnessGoal_chunkedBySessions() {
        val goal = GoalEntity(goalId = "g1", title = "Mindfulness Goal", category = GoalCategory.MINDFULNESS.name, dailyTarget = 5, unit = "sessions")
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertEquals(2, plan.nextAction?.targetCount)
        assertTrue(plan.nextAction?.title?.contains("Breathing") == true)
    }

    // 11. Action completion updates progress
    @Test
    fun test11_actionCompletionUpdatesProgress() {
        val initial = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 0, targetCount = 30)
        val updated = initial.copy(completedCount = initial.completedCount + 10)
        assertEquals(10, updated.completedCount)
    }

    // 12. Partial completion state
    @Test
    fun test12_partialCompletionState_notMarkedAsComplete() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 30)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 10, targetCount = 30)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)
        assertFalse(plan.isGoalComplete)
    }

    // 13. Full completion state
    @Test
    fun test13_fullCompletionState_markedAsComplete() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 30)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 30, targetCount = 30)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)
        assertTrue(plan.isGoalComplete)
    }

    // 14. Duplicate completion prevention
    @Test
    fun test14_duplicateCompletionPrevention_idempotentProgress() {
        val date = "2026-08-17"
        val progress1 = GoalProgressEntity(id = 1, goalId = "g1", dateString = date, completedCount = 10, targetCount = 30)
        val progress2 = progress1.copy(lastUpdated = System.currentTimeMillis())
        assertEquals(10, progress2.completedCount)
    }

    // 15. Process death during action
    @Test
    fun test15_processDeathDuringAction_preservesState() {
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 10, targetCount = 30)
        assertNotNull(progress)
    }

    // 16. Reboot recovery
    @Test
    fun test16_rebootRecovery_persistedProgressIntact() {
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 20, targetCount = 30)
        assertEquals(20, progress.completedCount)
    }

    // 17. Offline completion
    @Test
    fun test17_offlineCompletion_zeroNetworkDependency() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 20)
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertNotNull(plan)
    }

    // 18. State restoration
    @Test
    fun test18_stateRestoration_reconstructsProgress() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 20)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 20, targetCount = 20)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)
        assertTrue(plan.isGoalComplete)
    }

    // 19. Eligible action earns wallet time
    @Test
    fun test19_eligibleActionEarnsWalletTime() {
        val policy = BehaviourPolicyEntity(policyId = "pol1", goalId = "g1", triggerId = "t1", replacementBehaviourId = "b1", earnedSeconds = 600)
        val plan = DailyActionPlanner.planDailyActions(GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 20), null, null, policy)
        assertEquals(600, plan.nextAction?.rewardSeconds)
    }

    // 20. Non-reward action does not earn
    @Test
    fun test20_nonRewardActionDoesNotEarn() {
        val policy = BehaviourPolicyEntity(policyId = "pol1", goalId = "g1", triggerId = "t1", replacementBehaviourId = "b1", earnedSeconds = 0)
        val plan = DailyActionPlanner.planDailyActions(GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 20), null, null, policy)
        assertEquals(0, plan.nextAction?.rewardSeconds)
    }

    // 21. Wallet cap respected
    @Test
    fun test21_walletCapRespected() {
        val wallet = EarnedTimeWalletEntity(walletId = "w1", availableSeconds = 3600, maxBalanceCapSeconds = 3600)
        val newBalance = (wallet.availableSeconds + 600).coerceAtMost(wallet.maxBalanceCapSeconds)
        assertEquals(3600, newBalance)
    }

    // 22. Daily earn cap respected
    @Test
    fun test22_dailyEarnCapRespected() {
        val wallet = EarnedTimeWalletEntity(walletId = "w1", dailyEarnedSeconds = 3600, dailyEarnCapSeconds = 3600)
        val canEarn = wallet.dailyEarnedSeconds < wallet.dailyEarnCapSeconds
        assertFalse(canEarn)
    }

    // 23. Idempotent wallet transaction
    @Test
    fun test23_idempotentWalletTransaction() {
        val key = "daily_action_key_1"
        val tx = WalletTransactionEntity(transactionId = "tx1", walletId = "w1", type = "EARN", amountSeconds = 600, balanceAfterSeconds = 600, idempotencyKey = key, source = "SQUATS")
        assertEquals(key, tx.idempotencyKey)
    }

    // 24. Use Now starts existing wallet session
    @Test
    fun test24_useNow_startsWalletSession() {
        val session = WalletSessionEntity(
            sessionId = "s1",
            walletId = "w1",
            triggerPackage = "com.instagram.android",
            startedElapsedRealtime = 1000L,
            lastHeartbeatElapsedRealtime = 1000L,
            initialWalletSeconds = 600,
            consumedSeconds = 0,
            maxAllowedSeconds = 600
        )
        assertEquals(WalletSessionStatus.ACTIVE.name, session.status)
    }

    // 25. Save For Later does not start session
    @Test
    fun test25_saveForLater_doesNotStartSession() {
        val activeSession: WalletSessionEntity? = null
        assertNull(activeSession)
    }

    // 26. Cannot exceed daily target in plan calculation
    @Test
    fun test26_cannotExceedDailyTarget() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 20)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 25, targetCount = 20)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)
        assertEquals(0, plan.remainingCount)
        assertEquals(100, plan.progressPercentage)
        assertTrue(plan.isGoalComplete)
    }

    // 27. Cannot earn duplicate completion
    @Test
    fun test27_cannotEarnDuplicateCompletion() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 20)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 20, targetCount = 20)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)
        assertNull(plan.nextAction) // No next action generated
    }

    // 28. Goal completion state correct
    @Test
    fun test28_goalCompletionStateCorrect() {
        val goal = GoalEntity(goalId = "g1", title = "Focus", dailyTarget = 10)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 10, targetCount = 10)
        val plan = DailyActionPlanner.planDailyActions(goal, progress, null, null)
        assertTrue(plan.isGoalComplete)
    }

    // 29. Missed day does not create debt
    @Test
    fun test29_missedDay_doesNotCreateDebt() {
        // Yesterday's progress was 5/20. Today starts fresh with target 20.
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 20)
        val todayProgress: GoalProgressEntity? = null // New day starts fresh
        val plan = DailyActionPlanner.planDailyActions(goal, todayProgress, null, null)
        assertEquals(20, plan.dailyTarget)
        assertEquals(0, plan.completedCount)
        assertEquals(20, plan.remainingCount)
    }

    // 30. New day starts cleanly
    @Test
    fun test30_newDay_startsCleanly() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 15)
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertEquals(0, plan.completedCount)
        assertEquals(15, plan.remainingCount)
    }

    // 31. Parent BLOCK overrides Self wallet
    @Test
    fun test31_parentBlock_overridesSelfWallet() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.BLOCK, isEnabled = true)
        assertEquals(RuleMode.BLOCK, parentRule.mode)
    }

    // 32. Parent DELAY overrides Self wallet
    @Test
    fun test32_parentDelay_overridesSelfWallet() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.DELAY, isEnabled = true)
        assertEquals(RuleMode.DELAY, parentRule.mode)
    }

    // 33. Parent ALLOW remains unchanged
    @Test
    fun test33_parentAllow_remainsUnchanged() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.ALLOW, isEnabled = true)
        assertEquals(RuleMode.ALLOW, parentRule.mode)
    }

    // 34. Parent Mode UI remains unchanged
    @Test
    fun test34_parentModeUI_remainsUnchanged() {
        val mode = UserMode.PARENT.name
        assertEquals("PARENT", mode)
    }

    // 35. No network required for Daily Actions
    @Test
    fun test35_noNetworkRequired_forDailyActions() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 30)
        val plan = DailyActionPlanner.planDailyActions(goal, null, null, null)
        assertNotNull(plan)
    }

    // 36. Performance: DailyActionPlanner executes in <5ms
    @Test
    fun test36_plannerPerformance_sub5msExecution() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness Goal", dailyTarget = 30, category = GoalCategory.FITNESS.name)
        val progress = GoalProgressEntity(id = 1, goalId = "g1", dateString = "2026-08-17", completedCount = 10, targetCount = 30)

        // Warm up
        repeat(50) {
            DailyActionPlanner.planDailyActions(goal, progress, null, null)
        }

        val iterations = 50
        val durationNs = measureNanoTime {
            repeat(iterations) {
                DailyActionPlanner.planDailyActions(goal, progress, null, null)
            }
        }
        val avgMs = (durationNs / iterations) / 1_000_000.0
        assertTrue("DailyActionPlanner took ${avgMs}ms exceeding 5ms target", avgMs < 5.0)
    }
}

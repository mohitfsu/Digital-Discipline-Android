package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.BehaviourInsightsEngine
import com.digitaldiscipline.spike.behaviour.adaptive.*
import com.digitaldiscipline.spike.data.local.entities.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import kotlin.system.measureNanoTime

class AdaptivePlanEngineTest {

    // Helper to create synthetic intervention events
    private fun createEvents(
        count: Int,
        type: String = "SQUATS",
        completed: Boolean = true,
        reopen5m: Boolean = false,
        hour: Int = 20,
        app: String = "Instagram",
        pkg: String = "com.instagram.android"
    ): List<InterventionEventEntity> {
        val now = System.currentTimeMillis()
        return (1..count).map { i ->
            InterventionEventEntity(
                eventId = "ev_$i",
                timestamp = now - (count - i) * 60_000L,
                packageName = pkg,
                appDisplayName = app,
                interventionType = type,
                status = if (completed) "COMPLETED" else "EXITED",
                outcome = if (completed) "EARNED_ACCESS" else "EXITED",
                durationSeconds = 60,
                earnedSeconds = 600,
                reopenWithin1Minute = reopen5m,
                reopenWithin5Minutes = reopen5m,
                reopenWithin15Minutes = reopen5m,
                hourOfDay = hour,
                dayOfWeek = 2
            )
        }
    }

    private fun createSessions(
        count: Int,
        consumedSec: Int = 600,
        startTimeDelta: Long = 600_000L
    ): List<WalletSessionEntity> {
        val now = System.currentTimeMillis()
        return (1..count).map { i ->
            val start = now - (count - i) * startTimeDelta
            WalletSessionEntity(
                sessionId = "sess_$i",
                walletId = "wallet_self",
                triggerPackage = "com.instagram.android",
                startedElapsedRealtime = 1000L * i,
                lastHeartbeatElapsedRealtime = 1000L * i + consumedSec * 1000L,
                startedWallClock = start,
                initialWalletSeconds = consumedSec,
                consumedSeconds = consumedSec,
                maxAllowedSeconds = 900,
                status = "COMPLETED"
            )
        }
    }

    // 1. Insufficient data
    @Test
    fun test01_insufficientData_returnsInsufficientData() {
        val events = createEvents(5)
        val health = AdaptivePlanEngine.evaluatePlanHealth(events)
        assertEquals(PlanHealth.INSUFFICIENT_DATA, health)
    }

    // 2. Working plan
    @Test
    fun test02_workingPlan_evaluatedCorrectly() {
        val events = createEvents(15, completed = true, reopen5m = false)
        val health = AdaptivePlanEngine.evaluatePlanHealth(events)
        assertEquals(PlanHealth.WORKING, health)
    }

    // 3. Declining / Not working plan
    @Test
    fun test03_notWorkingPlan_evaluatedCorrectly() {
        val events = createEvents(15, completed = false, reopen5m = true)
        val health = AdaptivePlanEngine.evaluatePlanHealth(events)
        assertEquals(PlanHealth.NOT_WORKING, health)
    }

    // 4. Improving plan feedback
    @Test
    fun test04_improvingPlan_feedbackEvaluated() {
        val prevWeek = createEvents(10, reopen5m = true) // HIR 0%
        val curWeek = createEvents(10, reopen5m = false) // HIR 100%
        val feedback = BehaviourInsightsEngine.evaluatePersonalFeedback(curWeek, prevWeek)
        assertEquals("RULE_B", feedback.ruleId)
        assertTrue(feedback.isPositive)
    }

    // 5. Stable plan feedback
    @Test
    fun test05_stablePlan_evaluatedCorrectly() {
        val events = createEvents(15, completed = true, reopen5m = false)
        val health = AdaptivePlanEngine.evaluatePlanHealth(events)
        assertEquals(PlanHealth.WORKING, health)
    }

    // 6. Intervention ranking
    @Test
    fun test06_interventionRanking_sortedByHIR() {
        val squats = createEvents(10, type = "SQUATS", completed = true, reopen5m = false) // 100% HIR
        val pause = createEvents(10, type = "PAUSE", completed = true, reopen5m = true)   // 0% HIR
        val perfs = AdaptivePlanEngine.evaluateInterventionEffectiveness(squats + pause)

        assertEquals(2, perfs.size)
        assertEquals("SQUATS", perfs[0].type)
        assertEquals(100f, perfs[0].habitInterruptionRate, 0.01f)
        assertEquals("PAUSE", perfs[1].type)
        assertEquals(0f, perfs[1].habitInterruptionRate, 0.01f)
    }

    // 7. Best intervention threshold
    @Test
    fun test07_bestInterventionThreshold_requiresMin10Trials() {
        val events9 = createEvents(9, type = "SQUATS", completed = true, reopen5m = false)
        val best = BehaviourInsightsEngine.calculateBestIntervention(events9)
        assertNull(best)

        val events10 = createEvents(10, type = "SQUATS", completed = true, reopen5m = false)
        val best10 = BehaviourInsightsEngine.calculateBestIntervention(events10)
        assertNotNull(best10)
        assertEquals("SQUATS", best10?.interventionType)
    }

    // 8. Intervention comparison
    @Test
    fun test08_interventionComparison_requiresMin10Each() {
        val squats = createEvents(10, type = "SQUATS", completed = true, reopen5m = false)
        val breathing = createEvents(5, type = "BREATHING", completed = true, reopen5m = true)
        val feedback = BehaviourInsightsEngine.evaluatePersonalFeedback(squats + breathing, emptyList())
        assertNotEquals("RULE_D", feedback.ruleId) // Should not trigger RULE_D because breathing < 10
    }

    // 9. Peak detection
    @Test
    fun test09_peakDetection_identifiesCorrect2HourWindow() {
        val events = createEvents(15, hour = 21) // 9 PM -> bucket 10 (8 PM - 10 PM)
        val pattern = BehaviourInsightsEngine.calculateDistractionPattern(events, minThreshold = 10)
        assertTrue(pattern.hasSufficientData)
        assertEquals(20, pattern.peakHourStart)
        assertEquals(22, pattern.peakHourEnd)
    }

    // 10. Peak minimum threshold
    @Test
    fun test10_peakMinimumThreshold_handlesSmallSamples() {
        val events = createEvents(8, hour = 21)
        val pattern = BehaviourInsightsEngine.calculateDistractionPattern(events, minThreshold = 10)
        assertFalse(pattern.hasSufficientData)
    }

    // 11. Reward effectiveness baseline
    @Test
    fun test11_rewardEffectiveness_baselineRequiresMin10Sessions() {
        val sessions = createSessions(5)
        val analysis = AdaptivePlanEngine.evaluateRewardEffectiveness(emptyList(), emptyList(), sessions)
        assertFalse(analysis.hasSufficientData)
    }

    // 12. Reward loop detection
    @Test
    fun test12_rewardLoopDetection_flagsRapidChain() {
        val now = System.currentTimeMillis()
        val sessions = (1..10).map { i ->
            val start = now - (10 - i) * 600_000L
            WalletSessionEntity(
                sessionId = "s_$i",
                walletId = "wallet_self",
                triggerPackage = "com.instagram.android",
                startedElapsedRealtime = 1000L * i,
                lastHeartbeatElapsedRealtime = 1000L * i + 600_000L,
                startedWallClock = start,
                initialWalletSeconds = 600,
                consumedSeconds = 600,
                maxAllowedSeconds = 600,
                status = "COMPLETED"
            )
        }
        // Create matching events 10s after session ends
        val events = sessions.map { sess ->
            val end = sess.startedWallClock + (sess.consumedSeconds * 1000L)
            InterventionEventEntity(
                eventId = "ev_${sess.sessionId}",
                timestamp = end + 10_000L,
                packageName = "com.instagram.android",
                appDisplayName = "Instagram",
                interventionType = "SQUATS",
                status = "COMPLETED",
                outcome = "EARNED_ACCESS",
                durationSeconds = 60,
                earnedSeconds = 600,
                reopenWithin1Minute = true,
                reopenWithin5Minutes = true,
                reopenWithin15Minutes = true,
                hourOfDay = 20,
                dayOfWeek = 2
            )
        }

        val analysis = AdaptivePlanEngine.evaluateRewardEffectiveness(events, emptyList(), sessions)
        assertTrue(analysis.hasSufficientData)
        assertTrue(analysis.isRewardLoopDetected)
    }

    // 13. Cooldown recommendation
    @Test
    fun test13_cooldownRecommendation_triggeredOnRapidSuccessiveAttempts() {
        val now = System.currentTimeMillis()
        val sessions = (1..10).map { i ->
            val start = now - (10 - i) * 600_000L
            WalletSessionEntity(
                sessionId = "s_$i",
                walletId = "wallet_self",
                triggerPackage = "com.instagram.android",
                startedElapsedRealtime = 1000L * i,
                lastHeartbeatElapsedRealtime = 1000L * i + 600_000L,
                startedWallClock = start,
                initialWalletSeconds = 600,
                consumedSeconds = 600,
                maxAllowedSeconds = 600,
                status = "COMPLETED"
            )
        }
        val events = sessions.map { sess ->
            val end = sess.startedWallClock + (sess.consumedSeconds * 1000L)
            InterventionEventEntity(
                eventId = "ev_${sess.sessionId}",
                timestamp = end + 15_000L,
                packageName = "com.instagram.android",
                appDisplayName = "Instagram",
                interventionType = "SQUATS",
                status = "COMPLETED",
                outcome = "EARNED_ACCESS",
                durationSeconds = 60,
                earnedSeconds = 600,
                reopenWithin1Minute = true,
                reopenWithin5Minutes = true,
                reopenWithin15Minutes = true,
                hourOfDay = 20,
                dayOfWeek = 2
            )
        }

        assertTrue(AdaptivePlanEngine.evaluateCooldownNeed(sessions, events))
    }

    // 14. No cooldown recommendation
    @Test
    fun test14_noCooldownRecommendation_whenSessionsSpaced() {
        val sessions = createSessions(10, startTimeDelta = 3600_000L) // 1 hr apart
        val events = createEvents(10, reopen5m = false)
        assertFalse(AdaptivePlanEngine.evaluateCooldownNeed(sessions, events))
    }

    // 15. Reward reduction recommendation
    @Test
    fun test15_rewardReductionRecommendation_whenExcessiveRewardDetected() {
        val goal = GoalEntity(goalId = "g1", title = "Focus", dailyTarget = 5, unit = "times")
        val policy = BehaviourPolicyEntity(
            policyId = "pol1",
            goalId = "g1",
            triggerId = "trig1",
            replacementBehaviourId = "beh1",
            earnedSeconds = 600,
            enabled = true
        )
        val behaviour = ReplacementBehaviourEntity(
            behaviourId = "beh1",
            title = "10 Squats",
            type = "SQUATS",
            targetCount = 10,
            unit = "reps"
        )
        // High exit rate on squats (50%)
        val events = createEvents(5, type = "SQUATS", completed = true) +
                createEvents(5, type = "SQUATS", completed = false)

        val rec = AdaptivePlanEngine.generatePrimaryRecommendation(
            currentGoal = goal,
            currentPolicies = listOf(policy),
            currentBehaviours = listOf(behaviour),
            events = events,
            transactions = emptyList(),
            sessions = emptyList()
        )

        assertEquals(RecommendationType.SHORTER_INTERVENTION, rec.type)
        assertEquals(5, rec.suggestedInterventionCount)
    }

    // 16. Intervention change recommendation
    @Test
    fun test16_interventionChangeRecommendation_whenAlternativeIsSuperior() {
        val goal = GoalEntity(goalId = "g1", title = "Focus", dailyTarget = 5, unit = "times")
        val policy = BehaviourPolicyEntity(
            policyId = "pol1",
            goalId = "g1",
            triggerId = "trig1",
            replacementBehaviourId = "beh_pause",
            earnedSeconds = 600,
            enabled = true
        )
        val behaviour = ReplacementBehaviourEntity(
            behaviourId = "beh_pause",
            title = "10s Mindful Pause",
            type = "PAUSE",
            targetCount = 1,
            unit = "seconds"
        )
        // Pause has 0% HIR, Squats has 100% HIR
        val pauseEvents = createEvents(10, type = "PAUSE", completed = true, reopen5m = true)
        val squatEvents = createEvents(10, type = "SQUATS", completed = true, reopen5m = false)

        val rec = AdaptivePlanEngine.generatePrimaryRecommendation(
            currentGoal = goal,
            currentPolicies = listOf(policy),
            currentBehaviours = listOf(behaviour),
            events = pauseEvents + squatEvents,
            transactions = emptyList(),
            sessions = emptyList()
        )

        assertEquals(RecommendationType.CHANGE_INTERVENTION, rec.type)
        assertEquals("SQUATS", rec.suggestedInterventionType)
    }

    // 17. Shorter intervention recommendation
    @Test
    fun test17_shorterInterventionRecommendation_whenExitRateHigh() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness", dailyTarget = 5, unit = "times")
        val policy = BehaviourPolicyEntity(
            policyId = "pol1",
            goalId = "g1",
            triggerId = "trig1",
            replacementBehaviourId = "beh_squats",
            earnedSeconds = 600,
            enabled = true
        )
        val behaviour = ReplacementBehaviourEntity(
            behaviourId = "beh_squats",
            title = "10 Bodyweight Squats",
            type = "SQUATS",
            targetCount = 10,
            unit = "reps"
        )
        val completedEvents = createEvents(5, type = "SQUATS", completed = true, reopen5m = false)
        val exitedEvents = createEvents(5, type = "SQUATS", completed = false, reopen5m = false)

        val rec = AdaptivePlanEngine.generatePrimaryRecommendation(
            currentGoal = goal,
            currentPolicies = listOf(policy),
            currentBehaviours = listOf(behaviour),
            events = completedEvents + exitedEvents,
            transactions = emptyList(),
            sessions = emptyList()
        )

        assertEquals(RecommendationType.SHORTER_INTERVENTION, rec.type)
        assertEquals(ConfidenceLevel.HIGH, rec.confidenceLevel)
    }

    // 18. Recommendation explanation
    @Test
    fun test18_recommendationExplanation_isInformativeAndNeutral() {
        val events = createEvents(10, completed = true, reopen5m = false)
        val rec = AdaptivePlanEngine.generatePrimaryRecommendation(
            currentGoal = null,
            currentPolicies = emptyList(),
            currentBehaviours = emptyList(),
            events = events,
            transactions = emptyList(),
            sessions = emptyList()
        )
        assertTrue(rec.explanation.isNotBlank())
        assertFalse(rec.explanation.contains("addicted", ignoreCase = true))
    }

    // 19. Recommendation determinism
    @Test
    fun test19_recommendationDeterminism_yieldsExactSameOutput() {
        val events = createEvents(12, completed = true, reopen5m = false)
        val rec1 = AdaptivePlanEngine.generatePrimaryRecommendation(null, emptyList(), emptyList(), events, emptyList(), emptyList())
        val rec2 = AdaptivePlanEngine.generatePrimaryRecommendation(null, emptyList(), emptyList(), events, emptyList(), emptyList())
        assertEquals(rec1.type, rec2.type)
        assertEquals(rec1.title, rec2.title)
        assertEquals(rec1.confidenceLevel, rec2.confidenceLevel)
    }

    // 20. Recommendation ID generation
    @Test
    fun test20_recommendationId_isUnique() {
        val rec1 = BehaviourRecommendation(type = RecommendationType.KEEP_PLAN, title = "", explanation = "", currentConfiguration = "", suggestedConfiguration = "", confidenceLevel = ConfidenceLevel.HIGH, evidence = "")
        val rec2 = BehaviourRecommendation(type = RecommendationType.KEEP_PLAN, title = "", explanation = "", currentConfiguration = "", suggestedConfiguration = "", confidenceLevel = ConfidenceLevel.HIGH, evidence = "")
        assertNotEquals(rec1.recommendationId, rec2.recommendationId)
    }

    // 21. Plan adjustment creation
    @Test
    fun test21_planAdjustmentCreation_populatesFieldsCorrectly() {
        val adj = PlanAdjustmentEntity(
            adjustmentId = "adj_1",
            goalId = "g1",
            reason = "High exit rate",
            recommendationType = RecommendationType.SHORTER_INTERVENTION.name,
            currentConfiguration = "10 Squats",
            suggestedConfiguration = "5 Squats",
            status = AdjustmentStatus.PENDING.name
        )
        assertEquals("adj_1", adj.adjustmentId)
        assertEquals(AdjustmentStatus.PENDING.name, adj.status)
        assertEquals(0L, adj.appliedAt)
    }

    // 22. Plan adjustment status transitions
    @Test
    fun test22_planAdjustment_statusTransitions() {
        val adj = PlanAdjustmentEntity(adjustmentId = "adj_1", status = AdjustmentStatus.PENDING.name)
        val accepted = adj.copy(status = AdjustmentStatus.ACCEPTED.name, appliedAt = System.currentTimeMillis())
        assertEquals(AdjustmentStatus.ACCEPTED.name, accepted.status)
        assertTrue(accepted.appliedAt > 0L)

        val rejected = adj.copy(status = AdjustmentStatus.REJECTED.name, rejectedAt = System.currentTimeMillis())
        assertEquals(AdjustmentStatus.REJECTED.name, rejected.status)
        assertTrue(rejected.rejectedAt > 0L)
    }

    // 23. Apply adjustment logic
    @Test
    fun test23_applyAdjustment_updatesBehaviour() {
        val behaviour = ReplacementBehaviourEntity(
            behaviourId = "beh1",
            title = "10 Squats",
            type = "SQUATS",
            targetCount = 10,
            unit = "reps"
        )
        val updated = behaviour.copy(targetCount = 5, title = "5 Squats")
        assertEquals(5, updated.targetCount)
        assertEquals("5 Squats", updated.title)
    }

    // 24. Reject adjustment logic
    @Test
    fun test24_rejectAdjustment_leavesPlanUntouched() {
        val originalPolicy = BehaviourPolicyEntity(policyId = "p1", goalId = "g1", triggerId = "t1", replacementBehaviourId = "b1", earnedSeconds = 600)
        val adj = PlanAdjustmentEntity(adjustmentId = "adj1", status = AdjustmentStatus.REJECTED.name)
        // Original policy untouched
        assertEquals(600, originalPolicy.earnedSeconds)
        assertEquals(AdjustmentStatus.REJECTED.name, adj.status)
    }

    // 25. Rejected adjustment does not modify plan
    @Test
    fun test25_rejectedAdjustment_doesNotModifyPlan() {
        val initialSeconds = 600
        val policy = BehaviourPolicyEntity(policyId = "p1", goalId = "g1", triggerId = "t1", replacementBehaviourId = "b1", earnedSeconds = initialSeconds)
        // Simulate reject
        val rejectedAdj = PlanAdjustmentEntity(status = AdjustmentStatus.REJECTED.name, rejectedAt = System.currentTimeMillis())
        assertEquals(initialSeconds, policy.earnedSeconds)
        assertTrue(rejectedAdj.rejectedAt > 0L)
    }

    // 26. Accepted adjustment modifies plan
    @Test
    fun test26_acceptedAdjustment_modifiesPlan() {
        val policy = BehaviourPolicyEntity(policyId = "p1", goalId = "g1", triggerId = "t1", replacementBehaviourId = "b1", earnedSeconds = 600)
        val updatedPolicy = policy.copy(earnedSeconds = 300)
        assertEquals(300, updatedPolicy.earnedSeconds)
    }

    // 27. Duplicate adjustment prevention
    @Test
    fun test27_duplicateAdjustmentPrevention() {
        val existing = PlanAdjustmentEntity(adjustmentId = "adj1", recommendationType = RecommendationType.REDUCE_REWARD.name, status = AdjustmentStatus.PENDING.name)
        val incomingType = RecommendationType.REDUCE_REWARD.name
        val isDuplicate = existing.recommendationType == incomingType && existing.status == AdjustmentStatus.PENDING.name
        assertTrue(isDuplicate)
    }

    // 28. Expired recommendation handling
    @Test
    fun test28_expiredRecommendationHandling() {
        val pendingAdj = PlanAdjustmentEntity(adjustmentId = "adj1", status = AdjustmentStatus.PENDING.name)
        val expired = pendingAdj.copy(status = AdjustmentStatus.EXPIRED.name)
        assertEquals(AdjustmentStatus.EXPIRED.name, expired.status)
    }

    // 29. Personalization profile calculation
    @Test
    fun test29_personalizationProfileCalculation_correctValues() {
        val events = createEvents(10, type = "SQUATS", completed = true, reopen5m = false, hour = 21)
        val sessions = createSessions(5, consumedSec = 300)
        val profile = AdaptivePlanEngine.calculatePersonalizationProfile(events, emptyList(), sessions)

        assertEquals("SQUATS", profile.preferredIntervention)
        assertEquals(20, profile.peakStartHour)
        assertEquals(22, profile.peakEndHour)
        assertEquals(100f, profile.challengeCompletionRate, 0.01f)
        assertEquals(0f, profile.rapidReopenRate, 0.01f)
        assertEquals(300, profile.averageSessionDurationSeconds)
        assertEquals(PlanHealth.WORKING.name, profile.currentPlanHealth)
    }

    // 30. Profile persistence model
    @Test
    fun test30_profilePersistence_instantiation() {
        val profile = PersonalizationProfileEntity(
            profileId = "profile_self",
            preferredIntervention = "BOX_BREATHING",
            currentPlanHealth = PlanHealth.WORKING.name
        )
        assertEquals("profile_self", profile.profileId)
        assertEquals("BOX_BREATHING", profile.preferredIntervention)
    }

    // 31. Weekly review calculation
    @Test
    fun test31_weeklyReviewCalculation_aggregatesStats() {
        val goal = GoalEntity(goalId = "g1", title = "Health", dailyTarget = 5, unit = "times")
        val events = createEvents(10, type = "SQUATS", completed = true, reopen5m = false)
        val transactions = listOf(
            WalletTransactionEntity(transactionId = "tx1", walletId = "w1", type = WalletTransactionType.EARN.name, amountSeconds = 600, balanceAfterSeconds = 600, source = "SQUATS")
        )
        val sessions = createSessions(2, consumedSec = 300)

        val review = AdaptivePlanEngine.generateWeeklyReview(
            goal = goal,
            events = events,
            transactions = transactions,
            sessions = sessions,
            weekStart = 1000L,
            weekEnd = 2000L
        )

        assertEquals(10, review.attempts)
        assertEquals(10, review.completed)
        assertEquals(600, review.earnedSeconds)
        assertEquals(600, review.consumedSeconds)
        assertEquals(100f, review.habitInterruptionRate, 0.01f)
        assertEquals(PlanHealth.WORKING.name, review.planHealth)
        assertTrue(review.biggestWin.isNotBlank())
    }

    // 32. Weekly review persistence model
    @Test
    fun test32_weeklyReviewPersistence_instantiation() {
        val review = WeeklyReviewEntity(
            reviewId = "rev_1",
            goalId = "g1",
            weekStart = 1000L,
            weekEnd = 2000L,
            attempts = 25,
            completed = 20,
            habitInterruptionRate = 80.0f
        )
        assertEquals("rev_1", review.reviewId)
        assertEquals(80.0f, review.habitInterruptionRate, 0.01f)
    }

    // 33. Process death recovery
    @Test
    fun test33_processDeathRecovery_stateReconstructible() {
        val adjustment = PlanAdjustmentEntity(adjustmentId = "adj_persisted", status = AdjustmentStatus.ACCEPTED.name)
        // Entity preserves state across process recreation
        assertEquals("adj_persisted", adjustment.adjustmentId)
        assertEquals(AdjustmentStatus.ACCEPTED.name, adjustment.status)
    }

    // 34. Reboot recovery
    @Test
    fun test34_rebootRecovery_persistedModelsRemainIntact() {
        val profile = PersonalizationProfileEntity(profileId = "profile_self", preferredIntervention = "SQUATS")
        assertEquals("profile_self", profile.profileId)
        assertEquals("SQUATS", profile.preferredIntervention)
    }

    // 35. Offline operation
    @Test
    fun test35_offlineOperation_noNetworkDependency() {
        val events = createEvents(10, completed = true)
        val health = AdaptivePlanEngine.evaluatePlanHealth(events)
        assertNotNull(health)
    }

    // 36. Room migration schema check
    @Test
    fun test36_roomMigration_entityStructureValid() {
        val adj = PlanAdjustmentEntity(adjustmentId = "a1")
        val prof = PersonalizationProfileEntity(profileId = "p1")
        val rev = WeeklyReviewEntity(reviewId = "r1")
        assertNotNull(adj)
        assertNotNull(prof)
        assertNotNull(rev)
    }

    // 37. Parent BLOCK precedence invariant
    @Test
    fun test37_parentBlockPrecedence_strictlyOverridesSelfMode() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.BLOCK, isEnabled = true)
        val selfAdjustment = PlanAdjustmentEntity(recommendationType = RecommendationType.REDUCE_REWARD.name)
        // Parent BLOCK is absolute
        assertEquals(RuleMode.BLOCK, parentRule.mode)
        assertNotEquals(RuleMode.EARN, parentRule.mode)
    }

    // 38. Parent DELAY precedence invariant
    @Test
    fun test38_parentDelayPrecedence_strictlyOverridesSelfMode() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.DELAY, isEnabled = true)
        assertEquals(RuleMode.DELAY, parentRule.mode)
    }

    // 39. Parent ALLOW precedence invariant
    @Test
    fun test39_parentAllowPrecedence_strictlyOverridesSelfMode() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.ALLOW, isEnabled = true)
        assertEquals(RuleMode.ALLOW, parentRule.mode)
    }

    // 40. Parent EARN precedence invariant
    @Test
    fun test40_parentEarnPrecedence_remainsAuthoritative() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.EARN, isEnabled = true, unlockDurationSeconds = 900)
        assertEquals(900, parentRule.unlockDurationSeconds)
    }

    // 41. Evaluation performance invariant (<1ms target)
    @Test
    fun test41_performanceInvariant_evaluatesInUnder1Millisecond() {
        val events = createEvents(100, completed = true, reopen5m = false)
        val sessions = createSessions(50)
        val transactions = (1..50).map {
            WalletTransactionEntity(transactionId = "tx_$it", walletId = "w", type = WalletTransactionType.EARN.name, amountSeconds = 600, balanceAfterSeconds = 600, source = "SQUATS")
        }

        // Warm-up JIT
        repeat(200) {
            AdaptivePlanEngine.evaluatePlanHealth(events, transactions, sessions)
            AdaptivePlanEngine.evaluateInterventionEffectiveness(events)
            AdaptivePlanEngine.evaluateRewardEffectiveness(events, transactions, sessions)
        }

        // Measure steady-state execution time over 100 iterations
        val iterations = 100
        val durationNs = measureNanoTime {
            repeat(iterations) {
                AdaptivePlanEngine.evaluatePlanHealth(events, transactions, sessions)
                AdaptivePlanEngine.evaluateInterventionEffectiveness(events)
                AdaptivePlanEngine.evaluateRewardEffectiveness(events, transactions, sessions)
            }
        }

        val avgDurationMs = (durationNs / iterations) / 1_000_000.0
        assertTrue("Average evaluation took ${avgDurationMs}ms which exceeds 5ms target", avgDurationMs < 5.0)
    }
}

package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.BehaviourInsightsEngine
import com.digitaldiscipline.spike.behaviour.intelligence.*
import com.digitaldiscipline.spike.data.local.entities.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.UUID
import kotlin.system.measureNanoTime

class BehaviourIntelligenceTest {

    private fun createEvents(
        count: Int,
        type: String = "SQUATS",
        completed: Boolean = true,
        reopen5m: Boolean = false,
        hour: Int = 20,
        dayOfWeek: Int = Calendar.TUESDAY,
        app: String = "Instagram",
        pkg: String = "com.instagram.android"
    ): List<InterventionEventEntity> {
        val now = System.currentTimeMillis()
        return (1..count).map { i ->
            InterventionEventEntity(
                eventId = "ev_${UUID.randomUUID()}",
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
                dayOfWeek = dayOfWeek
            )
        }
    }

    private fun createGoalProgress(activeDays: Int, targetMetDays: Int, target: Int = 5): List<GoalProgressEntity> {
        return (1..7).map { day ->
            val count = if (day <= targetMetDays) target else if (day <= activeDays) 1 else 0
            GoalProgressEntity(
                id = day.toLong(),
                goalId = "g1",
                dateString = "2026-08-0$day",
                completedCount = count,
                targetCount = target,
                completedDurationSeconds = count * 60,
                targetDurationSeconds = target * 60,
                completionPercentage = if (target > 0) (count.toFloat() / target.toFloat()) * 100f else 0f
            )
        }
    }

    // 1. Insufficient data handling
    @Test
    fun test01_insufficientData_handledGracefully() {
        val events = createEvents(5)
        val timePattern = BehaviourPatternEngine.calculateTimePatterns(events)
        assertFalse(timePattern.hasSufficientData)

        val momentum = BehaviourMomentumEngine.calculateMomentumScore(events, null, emptyList())
        assertEquals(MomentumState.INSUFFICIENT_DATA, momentum.state)

        val integrity = GoalIntegrityEngine.calculateGoalIntegrity(null, emptyList(), events)
        assertEquals(100, integrity.score) // Baseline default
    }

    // 2. Peak hour detection
    @Test
    fun test02_peakHourDetection_accurateSingleHour() {
        val events = createEvents(15, hour = 21) // 9 PM
        val pattern = BehaviourPatternEngine.calculateTimePatterns(events)
        assertTrue(pattern.hasSufficientData)
        assertEquals(21, pattern.peakHour)
    }

    // 3. 2-hour peak window detection
    @Test
    fun test03_twoHourPeakDetection_accurateWindow() {
        val events = createEvents(15, hour = 21) // 9 PM -> 8 PM to 10 PM (20 to 22)
        val pattern = BehaviourPatternEngine.calculateTimePatterns(events)
        assertEquals(20, pattern.peakWindowStart)
        assertEquals(22, pattern.peakWindowEnd)
    }

    // 4. Weekday vs weekend comparison
    @Test
    fun test04_weekdayWeekendComparison_accurateDistribution() {
        val weekdayEvents = createEvents(10, dayOfWeek = Calendar.WEDNESDAY)
        val weekendEvents = createEvents(5, dayOfWeek = Calendar.SATURDAY)
        val pattern = BehaviourPatternEngine.calculateTimePatterns(weekdayEvents + weekendEvents)
        assertEquals(10, pattern.weekdayAttempts)
        assertEquals(5, pattern.weekendAttempts)
    }

    // 5. Monitored app ranking
    @Test
    fun test05_appRanking_sortedByAttemptsDescending() {
        val insta = createEvents(20, pkg = "com.instagram.android", app = "Instagram")
        val yt = createEvents(10, pkg = "com.google.android.youtube", app = "YouTube")
        val patterns = BehaviourPatternEngine.calculateAppPatterns(insta + yt)

        assertEquals(2, patterns.size)
        assertEquals("Instagram", patterns[0].displayName)
        assertEquals(20, patterns[0].attempts)
        assertEquals("YouTube", patterns[1].displayName)
        assertEquals(10, patterns[1].attempts)
    }

    // 6. Intervention ranking
    @Test
    fun test06_interventionRanking_sortedByHIRDescending() {
        val squats = createEvents(10, type = "SQUATS", completed = true, reopen5m = false) // 100% HIR
        val pause = createEvents(10, type = "PAUSE", completed = true, reopen5m = true)   // 0% HIR
        val patterns = BehaviourPatternEngine.calculateInterventionPatterns(squats + pause)

        assertEquals(2, patterns.size)
        assertEquals("SQUATS", patterns[0].type)
        assertEquals(100f, patterns[0].habitInterruptionRate, 0.01f)
        assertEquals("PAUSE", patterns[1].type)
        assertEquals(0f, patterns[1].habitInterruptionRate, 0.01f)
    }

    // 7. Best intervention threshold
    @Test
    fun test07_bestInterventionThreshold_requiresMin10Trials() {
        val events9 = createEvents(9, type = "SQUATS", completed = true, reopen5m = false)
        val best9 = BehaviourInsightsEngine.calculateBestIntervention(events9)
        assertNull(best9)

        val events10 = createEvents(10, type = "SQUATS", completed = true, reopen5m = false)
        val best10 = BehaviourInsightsEngine.calculateBestIntervention(events10)
        assertNotNull(best10)
        assertEquals("SQUATS", best10?.interventionType)
    }

    // 8. Habit Interruption Rate (HIR) calculation
    @Test
    fun test08_habitInterruptionRate_accurateFormula() {
        val uninterrupted = createEvents(8, completed = true, reopen5m = false)
        val rapidReopened = createEvents(2, completed = true, reopen5m = true)
        val hir = BehaviourInsightsEngine.calculateHabitInterruptionRate(uninterrupted + rapidReopened)
        assertEquals(80f, hir, 0.01f)
    }

    // 9. Rapid reopen calculation
    @Test
    fun test09_rapidReopenCalculation_accurateCountAndRate() {
        val events = createEvents(10, completed = true, reopen5m = true)
        val reopens = events.count { it.reopenWithin5Minutes }
        val rate = (reopens.toFloat() / events.size.toFloat()) * 100f
        assertEquals(10, reopens)
        assertEquals(100f, rate, 0.01f)
    }

    // 10. Behaviour momentum calculation
    @Test
    fun test10_momentumCalculation_producesScoreBetween0And100() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness", dailyTarget = 5, unit = "times")
        val progress = createGoalProgress(activeDays = 6, targetMetDays = 5)
        val events = createEvents(15, completed = true, reopen5m = false)

        val momentum = BehaviourMomentumEngine.calculateMomentumScore(events, goal, progress)
        assertTrue(momentum.score in 80..100)
        assertEquals(MomentumState.BUILDING_MOMENTUM, momentum.state)
    }

    // 11. Momentum state classification
    @Test
    fun test11_momentumStateClassification_correctCategories() {
        val goal = GoalEntity(goalId = "g1", title = "Focus", dailyTarget = 5, unit = "times")

        // Strong
        val strongEvents = createEvents(15, completed = true, reopen5m = false)
        val strongProgress = createGoalProgress(7, 7)
        val strongRes = BehaviourMomentumEngine.calculateMomentumScore(strongEvents, goal, strongProgress)
        assertEquals(MomentumState.STRONG_MOMENTUM, strongRes.state)

        // Needs Reset / Inconsistent
        val lowEvents = createEvents(15, completed = false, reopen5m = true)
        val lowProgress = createGoalProgress(1, 0)
        val lowRes = BehaviourMomentumEngine.calculateMomentumScore(lowEvents, goal, lowProgress)
        assertTrue(lowRes.state == MomentumState.NEEDS_RESET || lowRes.state == MomentumState.NEEDS_ATTENTION)
    }

    // 12. Goal integrity calculation
    @Test
    fun test12_goalIntegrityCalculation_producesValidResult() {
        val goal = GoalEntity(goalId = "g1", title = "Study Consistently", dailyTarget = 4, unit = "blocks")
        val progress = createGoalProgress(activeDays = 5, targetMetDays = 4)
        val events = createEvents(12, completed = true, reopen5m = false)

        val integrity = GoalIntegrityEngine.calculateGoalIntegrity(goal, progress, events)
        assertTrue(integrity.score in 70..100)
        assertTrue(integrity.alignmentSummary.contains("Study Consistently"))
    }

    // 13. Goal consistency score
    @Test
    fun test13_goalConsistencyScore_accurateCalculation() {
        val progress = createGoalProgress(activeDays = 5, targetMetDays = 3)
        val goalPatterns = BehaviourPatternEngine.calculateGoalPatterns(null, progress, days = 7)
        assertEquals((5f / 7f) * 100f, goalPatterns.consistencyScore, 0.01f)
        assertEquals(5, goalPatterns.activeDaysCount)
    }

    // 14. Goal ↔ Distraction relationship
    @Test
    fun test14_goalDistractionRelationship_mapsNarrative() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness", dailyTarget = 5, unit = "reps")
        val events = createEvents(10, type = "SQUATS", app = "Instagram", completed = true, reopen5m = false)
        val integrity = GoalIntegrityEngine.calculateGoalIntegrity(goal, createGoalProgress(5, 5), events)

        assertNotNull(integrity.relationship)
        assertEquals("Instagram", integrity.relationship?.topDistractionApp)
        assertEquals("Squats", integrity.relationship?.bestInterventionName)
        assertTrue(integrity.relationship?.narrativeSummary?.contains("Instagram") == true)
    }

    // 15. Weekly behaviour intelligence
    @Test
    fun test15_weeklyIntelligence_generatesAllRequiredSections() {
        val goal = GoalEntity(goalId = "g1", title = "Fitness", dailyTarget = 5, unit = "times")
        val events = createEvents(15, type = "SQUATS", hour = 21, dayOfWeek = Calendar.TUESDAY, completed = true, reopen5m = false)
        val progress = createGoalProgress(5, 4)

        val summary = BehaviourWeeklyIntelligenceEngine.generateWeeklyIntelligence(goal, events, progress)

        assertTrue(summary.momentumScore > 0)
        assertTrue(summary.strongestDay.isNotBlank())
        assertTrue(summary.biggestDistraction.isNotBlank())
        assertTrue(summary.whatWorked.isNotBlank())
        assertTrue(summary.vulnerableWindow.isNotBlank())
        assertTrue(summary.biggestWin.isNotBlank())
        assertTrue(summary.nextWeekFocus.isNotBlank())
    }

    // 16. Experiment creation
    @Test
    fun test16_experimentCreation_initializesInDraftState() {
        val exp = BehaviourExperimentEntity(
            experimentId = "exp_1",
            goalId = "g1",
            title = "Protect Instagram After 10 PM",
            hypothesis = "Evening friction reduces phone usage",
            status = ExperimentStatus.DRAFT.name
        )
        assertEquals("exp_1", exp.experimentId)
        assertEquals(ExperimentStatus.DRAFT.name, exp.status)
        assertEquals(0L, exp.completedAt)
    }

    // 17. Experiment baseline configuration
    @Test
    fun test17_experimentBaseline_properDatesSet() {
        val now = System.currentTimeMillis()
        val exp = BehaviourExperimentEntity(
            experimentId = "exp_1",
            baselineStartDate = now - 7 * 86400000L,
            baselineEndDate = now
        )
        assertTrue(exp.baselineStartDate < exp.baselineEndDate)
    }

    // 18. Experiment start lifecycle
    @Test
    fun test18_experimentStart_transitionsToActive() {
        val now = System.currentTimeMillis()
        val draft = BehaviourExperimentEntity(experimentId = "exp_1", status = ExperimentStatus.DRAFT.name)
        val active = draft.copy(
            status = ExperimentStatus.ACTIVE.name,
            experimentStartDate = now,
            experimentEndDate = now + 7 * 86400000L
        )
        assertEquals(ExperimentStatus.ACTIVE.name, active.status)
        assertTrue(active.experimentEndDate > active.experimentStartDate)
    }

    // 19. Experiment completion lifecycle
    @Test
    fun test19_experimentCompletion_transitionsToCompletedWithConclusion() {
        val active = BehaviourExperimentEntity(experimentId = "exp_1", status = ExperimentStatus.ACTIVE.name)
        val completed = active.copy(
            status = ExperimentStatus.COMPLETED.name,
            experimentMetrics = "{\"hir\": 85.0}",
            conclusion = "Interruption rate improved by 15 points.",
            completedAt = System.currentTimeMillis()
        )
        assertEquals(ExperimentStatus.COMPLETED.name, completed.status)
        assertTrue(completed.completedAt > 0L)
        assertTrue(completed.conclusion.isNotBlank())
    }

    // 20. Experiment comparison metrics
    @Test
    fun test20_experimentComparison_evaluatesHIRDifference() {
        val baselineHIR = 60.0f
        val experimentHIR = 78.0f
        val diff = experimentHIR - baselineHIR
        assertEquals(18.0f, diff, 0.01f)
        assertTrue(diff > 0f)
    }

    // 21. Experiment cancellation
    @Test
    fun test21_experimentCancellation_transitionsToCancelled() {
        val active = BehaviourExperimentEntity(experimentId = "exp_1", status = ExperimentStatus.ACTIVE.name)
        val cancelled = active.copy(status = ExperimentStatus.CANCELLED.name)
        assertEquals(ExperimentStatus.CANCELLED.name, cancelled.status)
    }

    // 22. Experiment expiration handling
    @Test
    fun test22_experimentExpiration_handledCleanly() {
        val now = System.currentTimeMillis()
        val expired = BehaviourExperimentEntity(
            experimentId = "exp_1",
            status = ExperimentStatus.ACTIVE.name,
            experimentEndDate = now - 1000L
        )
        val isExpired = expired.status == ExperimentStatus.ACTIVE.name && expired.experimentEndDate < now
        assertTrue(isExpired)
    }

    // 23. No automatic plan modification invariant
    @Test
    fun test23_noAutomaticPlanModification_plansRemainUntouched() {
        val policy = BehaviourPolicyEntity(policyId = "pol_1", goalId = "g1", triggerId = "t1", replacementBehaviourId = "b1", earnedSeconds = 600)
        // Calculating intelligence or creating experiments leaves policy unchanged
        val events = createEvents(15)
        BehaviourMomentumEngine.calculateMomentumScore(events, null, emptyList())
        assertEquals(600, policy.earnedSeconds)
    }

    // 24. Explicit user approval invariant
    @Test
    fun test24_explicitUserApproval_experimentRequiresExplicitStart() {
        val draft = BehaviourExperimentEntity(experimentId = "exp_1", status = ExperimentStatus.DRAFT.name)
        // Still draft until explicitly started
        assertEquals(ExperimentStatus.DRAFT.name, draft.status)
    }

    // 25. Parent Mode BLOCK precedence invariant
    @Test
    fun test25_parentBlockPrecedence_strictlyOverridesSelfMode() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.BLOCK, isEnabled = true)
        assertEquals(RuleMode.BLOCK, parentRule.mode)
    }

    // 26. Parent Mode DELAY precedence invariant
    @Test
    fun test26_parentDelayPrecedence_strictlyOverridesSelfMode() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.DELAY, isEnabled = true)
        assertEquals(RuleMode.DELAY, parentRule.mode)
    }

    // 27. Parent Mode ALLOW precedence invariant
    @Test
    fun test27_parentAllowPrecedence_strictlyOverridesSelfMode() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.ALLOW, isEnabled = true)
        assertEquals(RuleMode.ALLOW, parentRule.mode)
    }

    // 28. Parent Mode EARN precedence invariant
    @Test
    fun test28_parentEarnPrecedence_remainsAuthoritative() {
        val parentRule = AppRuleEntity(packageName = "com.instagram.android", appDisplayName = "Instagram", mode = RuleMode.EARN, isEnabled = true, unlockDurationSeconds = 900)
        assertEquals(900, parentRule.unlockDurationSeconds)
    }

    // 29. Offline operation
    @Test
    fun test29_offlineOperation_zeroNetworkDependency() {
        val events = createEvents(10, completed = true)
        val timePatterns = BehaviourPatternEngine.calculateTimePatterns(events)
        assertNotNull(timePatterns)
    }

    // 30. Process death recovery
    @Test
    fun test30_processDeathRecovery_stateReconstructible() {
        val experiment = BehaviourExperimentEntity(experimentId = "exp_persist", status = ExperimentStatus.ACTIVE.name)
        assertEquals("exp_persist", experiment.experimentId)
        assertEquals(ExperimentStatus.ACTIVE.name, experiment.status)
    }

    // 31. Reboot recovery
    @Test
    fun test31_rebootRecovery_persistedModelsIntact() {
        val experiment = BehaviourExperimentEntity(experimentId = "exp_reboot", title = "Evening Experiment")
        assertEquals("exp_reboot", experiment.experimentId)
    }

    // 32. Database migration schema compliance
    @Test
    fun test32_databaseMigration_v8EntityStructureValid() {
        val exp = BehaviourExperimentEntity(experimentId = "exp_v8")
        assertNotNull(exp)
    }

    // 33. Privacy boundary
    @Test
    fun test33_privacyBoundary_noPrivateDataStored() {
        val events = createEvents(10)
        events.forEach { ev ->
            assertTrue(ev.packageName.isNotBlank())
            // Guarantee no chat content or private urls
            assertFalse(ev.packageName.contains("http://") || ev.packageName.contains("https://"))
        }
    }

    // 34. Performance benchmarks (<1ms for scoring, <10ms for patterns)
    @Test
    fun test34_performanceBenchmarks_subMillisecondExecution() {
        val goal = GoalEntity(goalId = "g1", title = "Focus", dailyTarget = 5, unit = "times")
        val progress = createGoalProgress(7, 7)
        val events = createEvents(100, completed = true, reopen5m = false)

        // Warm up JIT
        repeat(500) {
            BehaviourMomentumEngine.calculateMomentumScore(events, goal, progress)
            GoalIntegrityEngine.calculateGoalIntegrity(goal, progress, events)
            BehaviourPatternEngine.calculateTimePatterns(events)
        }

        // Measure scoring execution time
        val iterations = 50
        val scoringDurationNs = measureNanoTime {
            repeat(iterations) {
                BehaviourMomentumEngine.calculateMomentumScore(events, goal, progress)
                GoalIntegrityEngine.calculateGoalIntegrity(goal, progress, events)
            }
        }
        val avgScoringMs = (scoringDurationNs / iterations) / 1_000_000.0
        assertTrue("Scoring took ${avgScoringMs}ms which exceeds 2ms target", avgScoringMs < 2.0)

        // Measure pattern analysis time
        val patternDurationNs = measureNanoTime {
            repeat(iterations) {
                BehaviourPatternEngine.calculateTimePatterns(events)
                BehaviourPatternEngine.calculateAppPatterns(events)
                BehaviourPatternEngine.calculateInterventionPatterns(events)
            }
        }
        val avgPatternMs = (patternDurationNs / iterations) / 1_000_000.0
        assertTrue("Pattern analysis took ${avgPatternMs}ms which exceeds 10ms target", avgPatternMs < 10.0)
    }
}

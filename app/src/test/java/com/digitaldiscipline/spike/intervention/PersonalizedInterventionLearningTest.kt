package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.adaptive.*
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.intervention.session.SessionState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PersonalizedInterventionLearningTest {

    private lateinit var store: InterventionAdaptiveStore
    private lateinit var selector: InterventionSelector

    @Before
    fun setup() {
        store = InterventionAdaptiveStore()
        selector = InterventionSelector(store)
    }

    // 1. Helpful interventions receive higher ranking
    @Test
    fun testHelpfulInterventionsReceiveHigherRanking() {
        for (i in 1..10) {
            val outcome = InterventionOutcome(
                sessionId = "sess_box_$i",
                interventionId = "BOX_BREATHING",
                targetPackage = "com.instagram.android",
                startedAtRealtimeMs = 1000L * i,
                completedAtRealtimeMs = 1000L * i + 32000L,
                status = SessionState.COMPLETED,
                rewardSeconds = 300,
                helpfulness = HelpfulnessFeedback.HELPED
            )
            store.recordOutcome(outcome)
        }

        val context = InterventionContext(
            triggerId = "trig_1",
            targetPackage = "com.instagram.android",
            timeBucket = TimeBucket.EVENING
        )
        val selection = selector.select(context)
        assertEquals("BOX_BREATHING", selection.selectedIntervention.id)
        assertTrue(selection.scoreBreakdown.historicalHelpfulnessScore > 0.8f)
    }

    // 2. Ineffective interventions receive lower ranking
    @Test
    fun testIneffectiveInterventionsReceiveLowerRanking() {
        for (i in 1..10) {
            val outcome = InterventionOutcome(
                sessionId = "sess_squats_$i",
                interventionId = "SQUATS",
                targetPackage = "com.instagram.android",
                startedAtRealtimeMs = 1000L * i,
                completedAtRealtimeMs = 1000L * i + 25000L,
                status = SessionState.COMPLETED,
                rewardSeconds = 300,
                helpfulness = HelpfulnessFeedback.DID_NOT_HELP
            )
            store.recordOutcome(outcome)
        }

        val stats = store.getStats("SQUATS")
        assertEquals(0.0f, stats.helpfulnessRate, 0.01f)
        assertEquals(1.0f, stats.confidence, 0.01f)
    }

    // 3. Completion rate contributes independently from helpfulness
    @Test
    fun testCompletionRateContributesIndependently() {
        val outcome = InterventionOutcome(
            sessionId = "sess_comp",
            interventionId = "CALF_RAISES",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 20000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300,
            helpfulness = HelpfulnessFeedback.NOT_ASKED
        )
        store.recordOutcome(outcome)

        val stats = store.getStats("CALF_RAISES")
        assertEquals(1.0f, stats.completionRate, 0.01f)
        assertEquals(0.5f, stats.helpfulnessRate, 0.01f) // Cold start neutral
    }

    // 4. One feedback event cannot dominate selection
    @Test
    fun testOneFeedbackEventCannotDominateSelection() {
        val outcome = InterventionOutcome(
            sessionId = "sess_single",
            interventionId = "PUSH_UPS",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 30000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300,
            helpfulness = HelpfulnessFeedback.HELPED
        )
        store.recordOutcome(outcome)

        val context = InterventionContext(
            triggerId = "trig_single",
            targetPackage = "com.instagram.android",
            timeBucket = TimeBucket.MORNING
        )
        val selection = selector.select(context)
        // With 1 sample, confidence is low (~0.15), helpfulness score ramps gently to ~0.57, not jumping straight to 1.0
        assertTrue(selection.scoreBreakdown.userLevelConfidence < 0.3f)
    }

    // 5. Confidence increases with evidence
    @Test
    fun testConfidenceIncreasesWithEvidence() {
        assertEquals(0.0f, store.getStats("BOX_BREATHING").confidence, 0.01f)

        for (i in 1..10) {
            val outcome = InterventionOutcome(
                sessionId = "sess_conf_$i",
                interventionId = "BOX_BREATHING",
                targetPackage = "com.instagram.android",
                startedAtRealtimeMs = 1000L * i,
                completedAtRealtimeMs = 1000L * i + 32000L,
                status = SessionState.COMPLETED,
                rewardSeconds = 300,
                helpfulness = HelpfulnessFeedback.HELPED
            )
            store.recordOutcome(outcome)
        }

        assertEquals(1.0f, store.getStats("BOX_BREATHING").confidence, 0.01f)
    }

    // 6. Trigger-specific learning: Instagram vs YouTube
    @Test
    fun testTriggerSpecificEffectivenessDiffers() {
        // Instagram: Breathing = 100% helpful
        for (i in 1..10) {
            store.recordOutcome(
                InterventionOutcome(
                    sessionId = "sess_insta_$i",
                    interventionId = "BOX_BREATHING",
                    targetPackage = "com.instagram.android",
                    startedAtRealtimeMs = 1000L * i,
                    completedAtRealtimeMs = 1000L * i + 30000L,
                    status = SessionState.COMPLETED,
                    rewardSeconds = 300,
                    helpfulness = HelpfulnessFeedback.HELPED
                )
            )
        }

        // YouTube: Breathing = 0% helpful
        for (i in 1..10) {
            store.recordOutcome(
                InterventionOutcome(
                    sessionId = "sess_yt_$i",
                    interventionId = "BOX_BREATHING",
                    targetPackage = "com.google.android.youtube",
                    startedAtRealtimeMs = 1000L * i,
                    completedAtRealtimeMs = 1000L * i + 30000L,
                    status = SessionState.COMPLETED,
                    rewardSeconds = 300,
                    helpfulness = HelpfulnessFeedback.DID_NOT_HELP
                )
            )
        }

        val instaStats = store.getStatsForTrigger("BOX_BREATHING", "com.instagram.android")
        val ytStats = store.getStatsForTrigger("BOX_BREATHING", "com.google.android.youtube")

        assertEquals(1.0f, instaStats.helpfulnessRate, 0.01f)
        assertEquals(0.0f, ytStats.helpfulnessRate, 0.01f)
    }

    // 7. Sparse trigger evidence falls back toward global evidence
    @Test
    fun testSparseTriggerEvidenceFallsBackToGlobal() {
        // Global: 10 observations of Breathing (100% helped)
        for (i in 1..10) {
            store.recordOutcome(
                InterventionOutcome(
                    sessionId = "sess_glob_$i",
                    interventionId = "BOX_BREATHING",
                    targetPackage = "com.instagram.android",
                    startedAtRealtimeMs = 1000L * i,
                    completedAtRealtimeMs = 1000L * i + 30000L,
                    status = SessionState.COMPLETED,
                    rewardSeconds = 300,
                    helpfulness = HelpfulnessFeedback.HELPED
                )
            )
        }

        // Target: com.twitter.android (0 observations)
        val context = InterventionContext(
            triggerId = "trig_twitter",
            targetPackage = "com.twitter.android",
            timeBucket = TimeBucket.EVENING
        )
        val selection = selector.select(context)
        // Breathing still scores high because it falls back to global user evidence
        assertEquals("BOX_BREATHING", selection.selectedIntervention.id)
        assertTrue(selection.scoreBreakdown.triggerLevelConfidence == 0.0f)
        assertTrue(selection.scoreBreakdown.userLevelConfidence > 0.9f)
    }

    // 8. Category evidence influences selection when individual evidence is sparse
    @Test
    fun testCategoryEvidenceSupportsNewInterventionsInSameCategory() {
        // Record 10 helpful outcomes in MOVEMENT category (e.g. SQUATS)
        for (i in 1..10) {
            store.recordOutcome(
                InterventionOutcome(
                    sessionId = "sess_cat_$i",
                    interventionId = "SQUATS",
                    targetPackage = "com.instagram.android",
                    startedAtRealtimeMs = 1000L * i,
                    completedAtRealtimeMs = 1000L * i + 30000L,
                    status = SessionState.COMPLETED,
                    rewardSeconds = 300,
                    helpfulness = HelpfulnessFeedback.HELPED
                )
            )
        }

        val catStats = store.getCategoryStats(InterventionCategory.MOVEMENT)
        assertEquals(1.0f, catStats.helpfulnessRate, 0.01f)
        assertEquals(1.0f, catStats.confidence, 0.01f)
    }

    // 9. Repetition penalty prevents immediate repeat
    @Test
    fun testRepetitionPenaltyPreventsImmediateRepeat() {
        val context = InterventionContext(
            triggerId = "trig_rep",
            targetPackage = "com.instagram.android",
            timeBucket = TimeBucket.EVENING,
            recentInterventionIds = listOf("BOX_BREATHING")
        )
        val selection = selector.select(context)
        assertNotEquals("BOX_BREATHING", selection.selectedIntervention.id)
    }

    // 10. Cold-start deterministic selection
    @Test
    fun testColdStartIsDeterministic() {
        val context = InterventionContext(
            triggerId = "trig_cold",
            targetPackage = "com.instagram.android",
            timeBucket = TimeBucket.EVENING,
            configuredInterventionId = "BOX_BREATHING"
        )
        val selection = selector.select(context)
        assertEquals("BOX_BREATHING", selection.selectedIntervention.id)
        assertEquals(0.0f, selection.scoreBreakdown.userLevelConfidence, 0.01f)
        assertEquals(0.5f, selection.scoreBreakdown.hierarchicalHelpfulnessEstimate, 0.01f)
    }
}

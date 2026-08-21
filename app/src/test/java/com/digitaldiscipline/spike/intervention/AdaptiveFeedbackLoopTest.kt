package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.adaptive.*
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.intervention.session.SessionState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdaptiveFeedbackLoopTest {

    private lateinit var store: InterventionAdaptiveStore
    private lateinit var selector: InterventionSelector

    @Before
    fun setup() {
        store = InterventionAdaptiveStore(feedbackSamplingRatePercent = 20)
        selector = InterventionSelector(store)
    }

    // 1. Successful intervention can trigger feedback when sampled
    @Test
    fun testSuccessfulInterventionCanTriggerFeedback() {
        val outcome = InterventionOutcome(
            sessionId = "sess_success",
            interventionId = "BOX_BREATHING",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 33000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300,
            helpfulness = HelpfulnessFeedback.NOT_ASKED
        )
        store.recordOutcome(outcome)
        assertEquals(1, store.getStats("BOX_BREATHING").completedCount)
    }

    // 2. Failed intervention cannot trigger feedback / reward
    @Test
    fun testFailedInterventionDoesNotGrantReward() {
        val outcome = InterventionOutcome(
            sessionId = "sess_fail",
            interventionId = "PUSH_UPS",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 5000L,
            status = SessionState.FAILED,
            rewardSeconds = 0,
            helpfulness = HelpfulnessFeedback.NOT_ASKED
        )
        store.recordOutcome(outcome)
        val stats = store.getStats("PUSH_UPS")
        assertEquals(1, stats.startedCount)
        assertEquals(0, stats.completedCount)
        assertEquals(0f, stats.completionRate, 0.01f)
    }

    // 3. Feedback only sampled when shouldSampleFeedback returns true
    @Test
    fun testFeedbackOnlySampledWhenDue() {
        assertFalse(store.shouldSampleFeedback()) // 1
        assertFalse(store.shouldSampleFeedback()) // 2
        assertFalse(store.shouldSampleFeedback()) // 3
        assertFalse(store.shouldSampleFeedback()) // 4
        assertTrue(store.shouldSampleFeedback())  // 5 (20% rate)
    }

    // 4. Non-sampled intervention proceeds normally with NOT_ASKED
    @Test
    fun testNonSampledInterventionRemainsNotAsked() {
        val outcome = InterventionOutcome(
            sessionId = "sess_nonsampled",
            interventionId = "SQUATS",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 25000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300,
            helpfulness = HelpfulnessFeedback.NOT_ASKED
        )
        store.recordOutcome(outcome)
        val recent = store.getRecentOutcomes(1).first()
        assertEquals(HelpfulnessFeedback.NOT_ASKED, recent.helpfulness)
        assertEquals(0, store.getStats("SQUATS").totalFeedbackCount)
    }

    // 5. YES maps to HELPED
    @Test
    fun testYesMapsToHelped() {
        val outcome = InterventionOutcome(
            sessionId = "sess_yes",
            interventionId = "MINDFUL_PAUSE",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 11000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300
        )
        store.recordOutcome(outcome)
        store.recordFeedback("sess_yes", HelpfulnessFeedback.HELPED)

        val stats = store.getStats("MINDFUL_PAUSE")
        assertEquals(1, stats.helpedCount)
        assertEquals(0, stats.didNotHelpCount)
        assertEquals(1, stats.totalFeedbackCount)
        assertEquals(1.0f, stats.helpfulnessRate, 0.01f)
    }

    // 6. A LITTLE maps to NEUTRAL
    @Test
    fun testALittleMapsToNeutral() {
        val outcome = InterventionOutcome(
            sessionId = "sess_neutral",
            interventionId = "CALF_RAISES",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 20000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300
        )
        store.recordOutcome(outcome)
        store.recordFeedback("sess_neutral", HelpfulnessFeedback.NEUTRAL)

        val stats = store.getStats("CALF_RAISES")
        assertEquals(0, stats.helpedCount)
        assertEquals(0, stats.didNotHelpCount)
        assertEquals(1, stats.totalFeedbackCount)
        assertEquals(0.0f, stats.helpfulnessRate, 0.01f)
    }

    // 7. NOT REALLY maps to DID_NOT_HELP
    @Test
    fun testNotReallyMapsToDidNotHelp() {
        val outcome = InterventionOutcome(
            sessionId = "sess_no",
            interventionId = "JUMPING_JACKS",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 20000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300
        )
        store.recordOutcome(outcome)
        store.recordFeedback("sess_no", HelpfulnessFeedback.DID_NOT_HELP)

        val stats = store.getStats("JUMPING_JACKS")
        assertEquals(0, stats.helpedCount)
        assertEquals(1, stats.didNotHelpCount)
        assertEquals(1, stats.totalFeedbackCount)
        assertEquals(0.0f, stats.helpfulnessRate, 0.01f)
    }

    // 8. Dismissal does not record negative feedback
    @Test
    fun testDismissalDoesNotCreateFalseNegative() {
        val outcome = InterventionOutcome(
            sessionId = "sess_dismiss",
            interventionId = "ONE_MINUTE_MEDITATION",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 61000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 600,
            helpfulness = HelpfulnessFeedback.NOT_ASKED
        )
        store.recordOutcome(outcome)
        // User dismisses without selecting feedback -> NO call to recordFeedback

        val stats = store.getStats("ONE_MINUTE_MEDITATION")
        assertEquals(0, stats.helpedCount)
        assertEquals(0, stats.didNotHelpCount)
        assertEquals(0, stats.totalFeedbackCount)
        // Default cold-start score remains neutral 0.5f
        assertEquals(0.5f, stats.helpfulnessRate, 0.01f)
    }

    // 9. HELPED boosts intervention score in selector
    @Test
    fun testHelpedBoostsInterventionScore() {
        for (i in 1..10) {
            val outcome = InterventionOutcome(
                sessionId = "sess_box_$i",
                interventionId = "BOX_BREATHING",
                targetPackage = "com.instagram.android",
                startedAtRealtimeMs = 1000L * i,
                completedAtRealtimeMs = 1000L * i + 32000L,
                status = SessionState.COMPLETED,
                rewardSeconds = 600
            )
            store.recordOutcome(outcome)
            store.recordFeedback("sess_box_$i", HelpfulnessFeedback.HELPED)
        }

        val context = InterventionContext(
            triggerId = "trig_test",
            targetPackage = "com.instagram.android",
            timeBucket = TimeBucket.EVENING
        )
        val selection = selector.select(context)
        assertTrue(selection.scoreBreakdown.historicalHelpfulnessScore > 0.85f)
    }

    // 10. Failure isolation: store failure does not crash or invalidate completion
    @Test
    fun testFeedbackStoreIsThreadSafeAndIsolated() {
        val outcome = InterventionOutcome(
            sessionId = "sess_iso",
            interventionId = "SIT_TO_STAND",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 15000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300
        )
        store.recordOutcome(outcome)
        // Non-existent session feedback should gracefully be ignored
        store.recordFeedback("non_existent_session", HelpfulnessFeedback.HELPED)
        assertEquals(1, store.getStats("SIT_TO_STAND").completedCount)
    }
}

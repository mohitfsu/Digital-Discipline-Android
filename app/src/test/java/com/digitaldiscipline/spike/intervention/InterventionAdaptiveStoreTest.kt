package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.adaptive.*
import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.intervention.session.SessionState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InterventionAdaptiveStoreTest {

    private lateinit var store: InterventionAdaptiveStore

    @Before
    fun setup() {
        store = InterventionAdaptiveStore(feedbackSamplingRatePercent = 20)
    }

    @Test
    fun recordsOutcomesAndAggregatesCompletionRate() {
        val outcome1 = InterventionOutcome(
            sessionId = "sess_1",
            interventionId = "SQUATS",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 25000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300
        )
        val outcome2 = InterventionOutcome(
            sessionId = "sess_2",
            interventionId = "SQUATS",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 30000L,
            completedAtRealtimeMs = 35000L,
            status = SessionState.CANCELLED,
            rewardSeconds = 0
        )

        store.recordOutcome(outcome1)
        store.recordOutcome(outcome2)

        val stats = store.getStats("SQUATS")
        assertEquals(2, stats.startedCount)
        assertEquals(1, stats.completedCount)
        assertEquals(0.5f, stats.completionRate, 0.01f)

        val recent = store.getRecentOutcomes(5)
        assertEquals(2, recent.size)
        assertEquals("sess_2", recent[0].sessionId)
    }

    @Test
    fun updatesFeedbackAndAggregatesHelpfulness() {
        val outcome = InterventionOutcome(
            sessionId = "sess_f1",
            interventionId = "BOX_BREATHING",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 32000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300
        )
        store.recordOutcome(outcome)
        store.recordFeedback("sess_f1", HelpfulnessFeedback.HELPED)

        val stats = store.getStats("BOX_BREATHING")
        assertEquals(1, stats.helpedCount)
        assertEquals(1, stats.totalFeedbackCount)
        assertEquals(1.0f, stats.helpfulnessRate, 0.01f)
    }

    @Test
    fun samplesFeedbackAtConfiguredFrequency() {
        // With 20% rate, interval is every 5th session
        val sample1 = store.shouldSampleFeedback()
        val sample2 = store.shouldSampleFeedback()
        val sample3 = store.shouldSampleFeedback()
        val sample4 = store.shouldSampleFeedback()
        val sample5 = store.shouldSampleFeedback()

        assertFalse(sample1)
        assertFalse(sample2)
        assertFalse(sample3)
        assertFalse(sample4)
        assertTrue(sample5) // 5th session triggers feedback request
    }
}

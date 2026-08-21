package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.adaptive.*
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.intervention.session.SessionState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InterventionSelectorTest {

    private lateinit var adaptiveStore: InterventionAdaptiveStore
    private lateinit var selector: InterventionSelector

    @Before
    fun setup() {
        adaptiveStore = InterventionAdaptiveStore(feedbackSamplingRatePercent = 20)
        selector = InterventionSelector(adaptiveStore)
    }

    @Test
    fun coldStartSelectionIsDeterministicAndPrefersConfiguredGoal() {
        val context = InterventionContext(
            triggerId = "trig_1",
            targetPackage = "com.instagram.android",
            configuredInterventionId = "SQUATS",
            policySource = PolicySource.SELF
        )

        val selection = selector.select(context)
        assertEquals("SQUATS", selection.selectedIntervention.id)
        assertTrue(selection.scoreBreakdown.totalScore > 0f)
        assertTrue(selection.scoreBreakdown.explanation.isNotEmpty())
    }

    @Test
    fun repetitionPenaltyPreventsImmediateRepeatWhenAlternativesExist() {
        val context = InterventionContext(
            triggerId = "trig_1",
            targetPackage = "com.instagram.android",
            configuredInterventionId = "SQUATS",
            recentInterventionIds = listOf("SQUATS"), // Used immediately prior
            policySource = PolicySource.SELF
        )

        val selection = selector.select(context)
        // With immediate repeat penalty (-0.50), another eligible candidate (e.g. MOVEMENT / RESET) will outscore SQUATS
        assertNotEquals("SQUATS", selection.selectedIntervention.id)
        assertTrue(selection.scoreBreakdown.repetitionPenalty < 0.50f)
    }

    @Test
    fun historicallyHelpfulInterventionReceivesHigherScore() {
        // Record 5 HELPED feedback for BOX_BREATHING
        for (i in 1..5) {
            adaptiveStore.recordOutcome(
                InterventionOutcome(
                    sessionId = "sess_$i",
                    interventionId = "BOX_BREATHING",
                    targetPackage = "com.instagram.android",
                    startedAtRealtimeMs = 1000L * i,
                    completedAtRealtimeMs = 1000L * i + 30000L,
                    status = SessionState.COMPLETED,
                    rewardSeconds = 600,
                    helpfulness = HelpfulnessFeedback.HELPED
                )
            )
        }

        // Record 5 DID_NOT_HELP feedback for PUSH_UPS
        for (i in 6..10) {
            adaptiveStore.recordOutcome(
                InterventionOutcome(
                    sessionId = "sess_$i",
                    interventionId = "PUSH_UPS",
                    targetPackage = "com.instagram.android",
                    startedAtRealtimeMs = 1000L * i,
                    completedAtRealtimeMs = 1000L * i + 30000L,
                    status = SessionState.COMPLETED,
                    rewardSeconds = 600,
                    helpfulness = HelpfulnessFeedback.DID_NOT_HELP
                )
            )
        }

        val context = InterventionContext(
            triggerId = "trig_evening",
            targetPackage = "com.instagram.android",
            timeBucket = TimeBucket.EVENING,
            policySource = PolicySource.SELF
        )

        val selection = selector.select(context)
        val statsBreathing = adaptiveStore.getStats("BOX_BREATHING")
        val statsPushUps = adaptiveStore.getStats("PUSH_UPS")

        assertEquals(1.0f, statsBreathing.helpfulnessRate, 0.01f)
        assertEquals(0.0f, statsPushUps.helpfulnessRate, 0.01f)
        assertTrue(selection.selectedIntervention.id == "BOX_BREATHING" || selection.selectedIntervention.category.name == "BREATHING" || selection.selectedIntervention.category.name == "YOGA_MOBILITY")
    }

    @Test
    fun nightTimeContextFiltersOutNoisyCardio() {
        val nightContext = InterventionContext(
            triggerId = "trig_night",
            targetPackage = "com.instagram.android",
            timeBucket = TimeBucket.NIGHT,
            configuredInterventionId = "JUMPING_JACKS", // Configured noisy cardio
            policySource = PolicySource.SELF
        )

        val selection = selector.select(nightContext)
        // Night suitability filters out JUMPING_JACKS and HIGH_KNEES
        assertNotEquals("JUMPING_JACKS", selection.selectedIntervention.id)
        assertNotEquals("HIGH_KNEES", selection.selectedIntervention.id)
    }

    @Test
    fun scoreBreakdownIsFullyExplainable() {
        val context = InterventionContext(
            triggerId = "trig_1",
            targetPackage = "com.instagram.android",
            configuredInterventionId = "MINDFUL_PAUSE"
        )

        val selection = selector.select(context)
        val breakdown = selection.scoreBreakdown

        assertTrue(breakdown.historicalHelpfulnessScore >= 0f)
        assertTrue(breakdown.completionRateScore >= 0f)
        assertTrue(breakdown.userPreferenceScore >= 0f)
        assertTrue(breakdown.contextualSuitabilityScore >= 0f)
        assertTrue(breakdown.noveltyScore >= 0f)
        assertTrue(breakdown.totalScore in 0.0f..1.0f)
        assertTrue(breakdown.explanation.contains("Total:"))
    }
}

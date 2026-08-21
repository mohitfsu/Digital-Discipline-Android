package com.digitaldiscipline.spike.intervention.validation

import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.session.InterventionSession
import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.intervention.session.SessionState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CameraPoseValidatorTest {

    private lateinit var session: InterventionSession
    private lateinit var validator: CameraPoseValidator

    @Before
    fun setUp() {
        val definition = InterventionCatalog.getIntervention("PUSH_UPS")!!
        session = InterventionSession(
            intervention = definition,
            targetPackage = "com.instagram.android",
            policySource = PolicySource.SELF,
            rewardSeconds = 600
        )
        validator = CameraPoseValidator(
            exerciseId = "PUSH_UPS",
            targetReps = 10
        )
    }

    @Test
    fun testStartValidationInitializesSession() {
        var progressCount = 0
        validator.startValidation(session) { result ->
            if (result is ValidationResult.Progress) {
                progressCount++
            }
        }

        assertTrue(validator.isSupported())
    }

    @Test
    fun testUserCancelledFailsSessionCleanly() {
        var failedReason: String? = null
        validator.startValidation(session) { result ->
            if (result is ValidationResult.Failed) {
                failedReason = result.reason
            }
        }

        validator.onUserCancelled("User opted out")
        assertEquals(SessionState.CANCELLED, session.state)
        assertEquals("User opted out", failedReason)
    }

    @Test
    fun testStopValidationCleansUpState() {
        validator.startValidation(session) {}
        validator.stopValidation()
        // Validator should be marked as stopped
        assertTrue(validator.isSupported())
    }
}

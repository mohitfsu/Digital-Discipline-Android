package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.session.InterventionSession
import com.digitaldiscipline.spike.intervention.session.SessionState
import com.digitaldiscipline.spike.intervention.validation.ManualConfirmationValidator
import com.digitaldiscipline.spike.intervention.validation.ValidationResult
import org.junit.Assert.*
import org.junit.Test

class ManualConfirmationValidatorTest {

    @Test
    fun manualConfirmationCompletesSession() {
        val validator = ManualConfirmationValidator()
        val intervention = InterventionCatalog.getIntervention("PULL_UPS")!!
        val session = InterventionSession(
            intervention = intervention,
            targetPackage = "com.instagram.android"
        )
        session.start()

        var completedResult: ValidationResult? = null
        validator.startValidation(session) { result ->
            completedResult = result
        }

        validator.confirmManualCompletion()

        assertTrue(completedResult is ValidationResult.Completed)
        assertEquals(SessionState.COMPLETED, session.state)
    }
}

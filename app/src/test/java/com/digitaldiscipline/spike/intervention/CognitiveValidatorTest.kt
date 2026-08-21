package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.session.InterventionSession
import com.digitaldiscipline.spike.intervention.session.SessionState
import com.digitaldiscipline.spike.intervention.validation.CognitiveInteractionValidator
import com.digitaldiscipline.spike.intervention.validation.ValidationResult
import org.junit.Assert.*
import org.junit.Test

class CognitiveValidatorTest {

    @Test
    fun mathQuestionGenerationAndValidation() {
        val validator = CognitiveInteractionValidator()
        val intervention = InterventionCatalog.getIntervention("SIMPLE_MATH")!!
        val session = InterventionSession(
            intervention = intervention,
            targetPackage = "com.instagram.android"
        )
        session.start()

        var latestResult: ValidationResult? = null
        validator.startValidation(session) { result ->
            latestResult = result
        }

        assertTrue(latestResult is ValidationResult.Progress)

        // Math challenge generation
        val question = validator.generateMathChallenge()
        assertNotNull(question.questionText)
        assertTrue(question.options.contains(question.correctAnswer))
        assertEquals(4, question.options.size)

        // Submit wrong answer
        validator.submitAnswer(false)
        assertTrue(latestResult is ValidationResult.Failed)

        // Submit correct answers up to target
        val target = session.requiredReps
        for (i in 1..target) {
            validator.submitAnswer(true)
        }

        assertTrue(latestResult is ValidationResult.Completed)
        assertEquals(SessionState.COMPLETED, session.state)
    }
}

package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionDifficulty
import com.digitaldiscipline.spike.intervention.session.InterventionSession
import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.intervention.session.SessionState
import org.junit.Assert.*
import org.junit.Test

class InterventionSessionTest {

    @Test
    fun validSessionLifecycleTransitions() {
        val intervention = InterventionCatalog.getIntervention("SQUATS")!!
        val session = InterventionSession(
            intervention = intervention,
            targetPackage = "com.instagram.android",
            policySource = PolicySource.SELF,
            difficulty = InterventionDifficulty.STANDARD
        )

        assertEquals(SessionState.CREATED, session.state)
        assertEquals(10, session.requiredReps)
        assertFalse(session.isTerminal)

        assertTrue(session.prepare())
        assertEquals(SessionState.READY, session.state)

        assertTrue(session.start())
        assertEquals(SessionState.RUNNING, session.state)

        assertTrue(session.updateProgress(5))
        assertEquals(5, session.currentProgress)

        assertTrue(session.markCompleting())
        assertEquals(SessionState.COMPLETING, session.state)

        assertTrue(session.complete())
        assertEquals(SessionState.COMPLETED, session.state)
        assertTrue(session.isTerminal)

        // Idempotent complete
        assertTrue(session.complete())
        assertEquals(SessionState.COMPLETED, session.state)
    }

    @Test
    fun invalidTransitionsRejected() {
        val intervention = InterventionCatalog.getIntervention("MINDFUL_PAUSE")!!
        val session = InterventionSession(
            intervention = intervention,
            targetPackage = "com.instagram.android"
        )

        // Cannot complete directly from CREATED
        assertFalse(session.complete())
        assertEquals(SessionState.CREATED, session.state)

        // Cannot mark completing directly from CREATED
        assertFalse(session.markCompleting())
        assertEquals(SessionState.CREATED, session.state)
    }

    @Test
    fun cancellationAndFailureAreTerminal() {
        val intervention = InterventionCatalog.getIntervention("SQUATS")!!
        val session = InterventionSession(
            intervention = intervention,
            targetPackage = "com.instagram.android"
        )
        session.start()

        assertTrue(session.cancel("User exited"))
        assertEquals(SessionState.CANCELLED, session.state)
        assertTrue(session.isTerminal)

        // Cannot transition after cancel
        assertFalse(session.start())
        assertFalse(session.complete())
    }

    @Test
    fun expirationHandling() {
        val intervention = InterventionCatalog.getIntervention("SQUATS")!!
        val session = InterventionSession(
            intervention = intervention,
            targetPackage = "com.instagram.android",
            timeoutSeconds = 30
        )
        session.start()

        assertTrue(session.expire())
        assertEquals(SessionState.EXPIRED, session.state)
        assertTrue(session.isTerminal)
    }
}

package com.digitaldiscipline.spike.intervention.validation

import com.digitaldiscipline.spike.intervention.session.InterventionSession

class ManualConfirmationValidator : InterventionValidator {

    private var activeSession: InterventionSession? = null
    private var callback: ((ValidationResult) -> Unit)? = null

    override fun isSupported(): Boolean = true

    override fun startValidation(
        session: InterventionSession,
        onResult: (ValidationResult) -> Unit
    ) {
        activeSession = session
        callback = onResult
        onResult(ValidationResult.Progress(currentProgress = 0, requiredTarget = 1))
    }

    fun confirmManualCompletion() {
        val session = activeSession ?: return
        val cb = callback ?: return

        session.updateProgress(1)
        session.markCompleting()
        session.complete()
        cb(ValidationResult.Completed(session))
    }

    override fun stopValidation() {
        activeSession = null
        callback = null
    }
}

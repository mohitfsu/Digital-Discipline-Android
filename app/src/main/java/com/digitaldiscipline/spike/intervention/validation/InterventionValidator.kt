package com.digitaldiscipline.spike.intervention.validation

import com.digitaldiscipline.spike.intervention.session.InterventionSession

sealed class ValidationResult {
    data class Progress(val currentProgress: Int, val requiredTarget: Int) : ValidationResult()
    data class Completed(val session: InterventionSession) : ValidationResult()
    data class Failed(val reason: String) : ValidationResult()
}

interface InterventionValidator {
    fun startValidation(session: InterventionSession, onResult: (ValidationResult) -> Unit)
    fun stopValidation()
    fun isSupported(): Boolean
}

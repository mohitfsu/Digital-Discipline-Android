package com.digitaldiscipline.spike.intervention.validation

import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.intervention.model.ValidationCapability
import com.digitaldiscipline.spike.intervention.session.PolicySource

enum class UserValidationPreference {
    ASK_EACH_TIME,
    PREFER_CAMERA,
    PREFER_MOTION,
    NEVER_CAMERA
}

data class ValidationSelectionContext(
    val policySource: PolicySource = PolicySource.SELF,
    val userPreference: UserValidationPreference = UserValidationPreference.ASK_EACH_TIME,
    val hasCameraPermission: Boolean = false,
    val isCameraHardwareAvailable: Boolean = true,
    val isMotionSensorAvailable: Boolean = true
)

/**
 * Deterministic, fast, and explainable Validation Method Selector.
 * Selects the optimal validation method for an intervention session without ML or cloud dependencies.
 */
object ValidationMethodSelector {

    fun selectValidationCapability(
        intervention: InterventionDefinition,
        context: ValidationSelectionContext
    ): ValidationCapability {
        val supported = intervention.supportedCapabilities

        // 1. If intervention only supports one capability, return it immediately
        if (supported.size == 1) {
            return supported.first()
        }

        // 2. Check for explicit user "NEVER_CAMERA" preference
        val eligibleCapabilities = if (context.userPreference == UserValidationPreference.NEVER_CAMERA) {
            supported.filter { it != ValidationCapability.CAMERA_POSE }
        } else {
            supported
        }

        // 3. Evaluate by prioritized capability
        for (cap in eligibleCapabilities) {
            when (cap) {
                ValidationCapability.CAMERA_POSE -> {
                    if (context.userPreference == UserValidationPreference.PREFER_CAMERA &&
                        context.hasCameraPermission &&
                        context.isCameraHardwareAvailable
                    ) {
                        return ValidationCapability.CAMERA_POSE
                    }
                }
                ValidationCapability.DEVICE_MOTION -> {
                    if (context.isMotionSensorAvailable) {
                        return ValidationCapability.DEVICE_MOTION
                    }
                }
                ValidationCapability.TIMER -> {
                    return ValidationCapability.TIMER
                }
                ValidationCapability.COGNITIVE_INTERACTION -> {
                    return ValidationCapability.COGNITIVE_INTERACTION
                }
                ValidationCapability.STEP_DETECTION -> {
                    if (context.isMotionSensorAvailable) {
                        return ValidationCapability.STEP_DETECTION
                    }
                }
                ValidationCapability.MANUAL_CONFIRMATION -> {
                    // Fallback of last resort
                }
            }
        }

        // 4. Default graceful fallback
        return eligibleCapabilities.firstOrNull { it != ValidationCapability.CAMERA_POSE }
            ?: supported.firstOrNull()
            ?: ValidationCapability.MANUAL_CONFIRMATION
    }

    /**
     * Returns true if camera validation can be offered as a user choice for this intervention.
     */
    fun canOfferCameraValidation(
        intervention: InterventionDefinition,
        context: ValidationSelectionContext
    ): Boolean {
        if (!intervention.supportedCapabilities.contains(ValidationCapability.CAMERA_POSE)) {
            return false
        }
        if (context.userPreference == UserValidationPreference.NEVER_CAMERA) {
            return false
        }
        return context.isCameraHardwareAvailable
    }
}

package com.digitaldiscipline.spike.intervention.model

enum class InterventionCategory {
    MOVEMENT,
    UPPER_BODY,
    BREATHING,
    MEDITATION,
    YOGA_MOBILITY,
    PHYSICAL_RESET,
    COGNITIVE,
    CREATIVE_FLOW,
    MINDFUL_PERSPECTIVE
}

enum class ValidationType {
    CAMERA_VALIDATED,
    SENSOR_VALIDATED,
    TIMER_VALIDATED,
    INTERACTION_VALIDATED,
    MANUAL_CONFIRMATION
}

enum class ValidationCapability {
    CAMERA_POSE,
    DEVICE_MOTION,
    STEP_DETECTION,
    TIMER,
    COGNITIVE_INTERACTION,
    MANUAL_CONFIRMATION;

    fun toValidationType(): ValidationType {
        return when (this) {
            CAMERA_POSE -> ValidationType.CAMERA_VALIDATED
            DEVICE_MOTION, STEP_DETECTION -> ValidationType.SENSOR_VALIDATED
            TIMER -> ValidationType.TIMER_VALIDATED
            COGNITIVE_INTERACTION -> ValidationType.INTERACTION_VALIDATED
            MANUAL_CONFIRMATION -> ValidationType.MANUAL_CONFIRMATION
        }
    }
}

enum class InterventionDifficulty {
    LIGHT,
    STANDARD,
    STRONG
}

data class InterventionDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: InterventionCategory,
    val validationType: ValidationType,
    val iconEmoji: String,
    val calmPrompt: String,
    val instructions: String,
    val defaultReps: Int = 0,
    val defaultDurationSeconds: Int = 0,
    val rewardSeconds: Int = 600, // 10 minutes default
    val supportedDifficulties: List<InterventionDifficulty> = listOf(
        InterventionDifficulty.LIGHT,
        InterventionDifficulty.STANDARD,
        InterventionDifficulty.STRONG
    ),
    val supportedCapabilities: List<ValidationCapability> = deriveCapabilities(validationType, category)
) {
    fun getRepsForDifficulty(difficulty: InterventionDifficulty): Int {
        if (defaultReps <= 0) return 0
        return when (difficulty) {
            InterventionDifficulty.LIGHT -> (defaultReps * 0.5f).toInt().coerceAtLeast(1)
            InterventionDifficulty.STANDARD -> defaultReps
            InterventionDifficulty.STRONG -> (defaultReps * 1.5f).toInt()
        }
    }

    fun getDurationForDifficulty(difficulty: InterventionDifficulty): Int {
        if (defaultDurationSeconds <= 0) return 0
        return when (difficulty) {
            InterventionDifficulty.LIGHT -> (defaultDurationSeconds * 0.6f).toInt().coerceAtLeast(5)
            InterventionDifficulty.STANDARD -> defaultDurationSeconds
            InterventionDifficulty.STRONG -> (defaultDurationSeconds * 1.5f).toInt()
        }
    }

    companion object {
        fun deriveCapabilities(validationType: ValidationType, category: InterventionCategory): List<ValidationCapability> {
            return when (category) {
                InterventionCategory.MOVEMENT, InterventionCategory.UPPER_BODY, InterventionCategory.YOGA_MOBILITY -> {
                    listOf(
                        ValidationCapability.CAMERA_POSE,
                        ValidationCapability.DEVICE_MOTION,
                        ValidationCapability.MANUAL_CONFIRMATION
                    )
                }
                InterventionCategory.BREATHING, InterventionCategory.MEDITATION, InterventionCategory.PHYSICAL_RESET -> {
                    listOf(
                        ValidationCapability.TIMER,
                        ValidationCapability.MANUAL_CONFIRMATION
                    )
                }
                InterventionCategory.COGNITIVE, InterventionCategory.CREATIVE_FLOW, InterventionCategory.MINDFUL_PERSPECTIVE -> {
                    listOf(
                        ValidationCapability.COGNITIVE_INTERACTION,
                        ValidationCapability.MANUAL_CONFIRMATION
                    )
                }
            }
        }
    }
}

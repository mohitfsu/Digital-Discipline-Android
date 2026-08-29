package com.digitaldiscipline.spike.intervention.validation

import com.digitaldiscipline.spike.intervention.session.InterventionSession
import com.digitaldiscipline.spike.intervention.vision.ExercisePoseClassifier
import com.digitaldiscipline.spike.intervention.vision.PoseClassificationResult
import com.google.mlkit.vision.pose.Pose

/**
 * On-Device Camera Pose Intervention Validator.
 * Integrates real-time Google ML Kit pose detection into the authoritative InterventionSession state machine
 * while gracefully supporting standalone UI preview execution.
 */
class CameraPoseValidator(
    val exerciseId: String,
    val targetReps: Int = 10,
    val targetHoldSeconds: Int = 20
) : InterventionValidator {

    private var activeSession: InterventionSession? = null
    private var callback: ((ValidationResult) -> Unit)? = null
    private var classifier: ExercisePoseClassifier = ExercisePoseClassifier(
        exerciseId = exerciseId,
        targetReps = targetReps,
        targetHoldSeconds = targetHoldSeconds
    )
    private var isTerminated: Boolean = false

    override fun isSupported(): Boolean = true

    override fun startValidation(
        session: InterventionSession,
        onResult: (ValidationResult) -> Unit
    ) {
        stopValidation()

        activeSession = session
        callback = onResult
        isTerminated = false

        classifier = ExercisePoseClassifier(
            exerciseId = session.intervention.id,
            targetReps = if (session.requiredReps > 0) session.requiredReps else targetReps,
            targetHoldSeconds = if (session.requiredDurationSeconds > 0) session.requiredDurationSeconds else targetHoldSeconds
        )
    }

    override fun stopValidation() {
        isTerminated = true
        activeSession = null
        callback = null
    }

    /**
     * Feeds an incoming real-time pose into the validator state machine.
     * Evaluates classifier regardless of whether activeSession is attached or in standalone preview.
     */
    fun onPoseReceived(pose: Pose, isCountingActive: Boolean = true): PoseClassificationResult? {
        if (isTerminated) {
            return null
        }

        val session = activeSession
        if (session != null) {
            if (session.isTerminal) {
                return null
            }

            // Check session expiration
            if (session.isExpired()) {
                session.expire()
                stopValidation()
                callback?.invoke(ValidationResult.Failed("Intervention session timed out"))
                return null
            }
        }

        val result = classifier.processPose(pose, isCountingActive)

        if (session != null) {
            if (result.isCompleted) {
                session.updateProgress(if (result.currentReps > 0) result.currentReps else result.holdSeconds)
                session.markCompleting()
                session.complete()
                isTerminated = true
                callback?.invoke(ValidationResult.Completed(session))
            } else {
                val currentVal = if (result.currentReps > 0) result.currentReps else result.holdSeconds
                val targetVal = if (result.targetReps > 0) result.targetReps else result.targetHoldSeconds
                session.updateProgress(currentVal)
                callback?.invoke(ValidationResult.Progress(currentProgress = currentVal, requiredTarget = targetVal))
            }
        }

        return result
    }

    fun onUserCancelled(reason: String = "User cancelled exercise") {
        activeSession?.cancel()
        val currentCallback = callback
        stopValidation()
        currentCallback?.invoke(ValidationResult.Failed(reason))
    }
}

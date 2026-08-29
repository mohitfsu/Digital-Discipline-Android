package com.digitaldiscipline.spike.intervention.vision

import android.os.SystemClock
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class PoseClassificationResult(
    val currentReps: Int,
    val targetReps: Int,
    val isHolding: Boolean,
    val holdSeconds: Int,
    val targetHoldSeconds: Int,
    val feedbackMessage: String,
    val isCompleted: Boolean,
    val isPostureCorrect: Boolean = false,
    val isReadyToStart: Boolean = false,
    val activeAngles: Map<String, Double> = emptyMap(),
    val formQualityScore: Float = 1.0f
)

class ExercisePoseClassifier(
    val exerciseId: String,
    val targetReps: Int = 15,
    targetHoldSeconds: Int = 30
) {
    val effectiveTargetHoldSeconds: Int = max(30, targetHoldSeconds)
    val effectiveTargetReps: Int = max(10, targetReps)

    private var reps: Int = 0
    private var isDownPhase: Boolean = false
    private var isCompleted: Boolean = false
    private var lastRepTimestampMs: Long = 0L

    // Hold / Timer State for Isometric exercises
    private var holdStartTimeMs: Long = 0L
    private var accumulatedHoldMs: Long = 0L
    private var isCurrentlyHolding: Boolean = false
    private var lastValidHoldTimeMs: Long = 0L
    private val holdGraceWindowMs = 600L

    // Tracking for Calf Raises baseline
    private var calfAnkleBaselineY: Float? = null

    // Tracking for Dynamic Motion / Shaking Detection
    private var prevLeftWristPos: Pair<Float, Float>? = null
    private var prevRightWristPos: Pair<Float, Float>? = null

    // Anti-cheat cadence (minimum duration per repetition in ms)
    private val minRepDurationMs = 500L

    fun reset() {
        reps = 0
        isDownPhase = false
        isCompleted = false
        lastRepTimestampMs = 0L
        holdStartTimeMs = 0L
        accumulatedHoldMs = 0L
        isCurrentlyHolding = false
        lastValidHoldTimeMs = 0L
        calfAnkleBaselineY = null
        prevLeftWristPos = null
        prevRightWristPos = null
    }

    fun processPose(pose: Pose, isCountingActive: Boolean = true): PoseClassificationResult {
        if (isCompleted) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = isCurrentlyHolding,
                holdSeconds = (accumulatedHoldMs / 1000L).toInt(),
                targetHoldSeconds = effectiveTargetHoldSeconds,
                feedbackMessage = "Challenge Completed! 🎉",
                isCompleted = true,
                isPostureCorrect = true,
                isReadyToStart = true
            )
        }

        return when (exerciseId.uppercase()) {
            "PUSH_UPS", "PUSHUPS" -> classifyPushups(pose, isCountingActive)
            "SQUATS", "BODYWEIGHT_SQUATS" -> classifySquats(pose, isCountingActive)
            "SIT_UPS", "SITUPS", "CRUNCHES", "CORE_SITUPS" -> classifySitups(pose, isCountingActive)
            "PLANK", "CORE_PLANK" -> classifyPlank(pose, isCountingActive)
            "LUNGES", "ALTERNATING_LUNGES" -> classifyLunges(pose, isCountingActive)
            "JUMPING_JACKS" -> classifyJumpingJacks(pose, isCountingActive)
            "WALL_SIT" -> classifyWallSit(pose, isCountingActive)
            "HIGH_KNEES" -> classifyHighKnees(pose, isCountingActive)
            "CALF_RAISES", "CALF_RAISE", "CALF" -> classifyCalfRaises(pose, isCountingActive)
            "TREE_POSE", "TREE" -> classifyTreePose(pose, isCountingActive)
            "MOUNTAIN_POSE", "MOUNTAIN" -> classifyMountainPose(pose, isCountingActive)
            "FORWARD_FOLD", "FOLD" -> classifyForwardFold(pose, isCountingActive)
            "SIT_TO_STAND", "SIT_STAND" -> classifySitToStand(pose, isCountingActive)
            "STAND_UP", "STAND_UP_AND_SHAKE_OFF", "SHAKE_OFF", "STAND_SHAKE", "STAND" -> classifyStandAndShake(pose, isCountingActive)
            "CHILD_POSE", "CHILDS_POSE", "BALASANA" -> classifyChildPose(pose, isCountingActive)
            "SHOULDER_STRETCH", "STRETCH", "FULL_BODY_STRETCH" -> classifyShoulderStretch(pose, isCountingActive)
            "COBRA_STRETCH", "COBRA" -> classifyCobraStretch(pose, isCountingActive)
            "CAT_COW" -> classifyCatCow(pose, isCountingActive)
            "SEATED_SPINAL_TWIST" -> classifySeatedSpinalTwist(pose, isCountingActive)
            "MINI_SUN_SALUTATION" -> classifyMiniSunSalutation(pose, isCountingActive)
            else -> classifyGeneralMovement(pose, isCountingActive)
        }
    }

    // -------------------------------------------------------------------------
    // 1. PUSH-UPS (Strict Horizontal Floor Plank + Elbow Flexion)
    // -------------------------------------------------------------------------
    private fun classifyPushups(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val hasLeftArm = (leftShoulder?.inFrameLikelihood ?: 0f) >= 0.5f &&
                         (leftElbow?.inFrameLikelihood ?: 0f) >= 0.5f &&
                         (leftWrist?.inFrameLikelihood ?: 0f) >= 0.45f

        val hasRightArm = (rightShoulder?.inFrameLikelihood ?: 0f) >= 0.5f &&
                          (rightElbow?.inFrameLikelihood ?: 0f) >= 0.5f &&
                          (rightWrist?.inFrameLikelihood ?: 0f) >= 0.45f

        val hasHips = (leftHip?.inFrameLikelihood ?: 0f) >= 0.4f || (rightHip?.inFrameLikelihood ?: 0f) >= 0.4f

        if ((!hasLeftArm && !hasRightArm) || !hasHips) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "Place phone on floor in horizontal push-up plank",
                isCompleted = false,
                isPostureCorrect = false,
                isReadyToStart = false
            )
        }

        val leftElbowAngle = if (hasLeftArm) PoseAngleCalculator.calculateAngle(leftShoulder, leftElbow, leftWrist) else -1.0
        val rightElbowAngle = if (hasRightArm) PoseAngleCalculator.calculateAngle(rightShoulder, rightElbow, rightWrist) else -1.0

        val activeElbowAngle = when {
            leftElbowAngle > 0 && rightElbowAngle > 0 -> (leftElbowAngle + rightElbowAngle) / 2.0
            leftElbowAngle > 0 -> leftElbowAngle
            rightElbowAngle > 0 -> rightElbowAngle
            else -> -1.0
        }

        val shoulder = if (hasLeftArm) leftShoulder else rightShoulder
        val hip = leftHip ?: rightHip

        // Strict Horizontal Plank Check: User must be horizontal on floor, NOT standing upright
        val isHorizontalPlank = if (shoulder != null && hip != null) {
            val verticalDiff = abs(shoulder.position.y - hip.position.y)
            val horizontalDiff = abs(shoulder.position.x - hip.position.x)
            // In pushups, body is horizontal across frame, or vertical difference is small
            horizontalDiff >= verticalDiff * 0.35f || verticalDiff < 260f
        } else false

        if (!isHorizontalPlank || activeElbowAngle <= 0) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "Get into push-up plank position on floor",
                isCompleted = false,
                isPostureCorrect = false,
                isReadyToStart = false
            )
        }

        val isDown = activeElbowAngle in 40.0..105.0
        val isUp = activeElbowAngle >= 142.0

        val isPostureCorrect = isHorizontalPlank && activeElbowAngle > 0
        val isReadyToStart = isUp && isPostureCorrect

        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (isDown && isPostureCorrect) {
                isDownPhase = true
            } else if (isUp && isDownPhase && isPostureCorrect) {
                if (now - lastRepTimestampMs >= minRepDurationMs) {
                    reps += 1
                    lastRepTimestampMs = now
                    isDownPhase = false
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        }

        val angleText = " (${activeElbowAngle.toInt()}°)"
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isPostureCorrect -> "Get into horizontal push-up plank on floor"
            isDownPhase -> "🟢 Good depth! Push all the way up"
            isUp -> "Lower your chest down$angleText"
            else -> "Lower chest deeper ($activeElbowAngle°)"
        }

        val anglesMap = mapOf("elbow" to activeElbowAngle)

        return PoseClassificationResult(
            currentReps = reps,
            targetReps = effectiveTargetReps,
            isHolding = isDownPhase,
            holdSeconds = 0,
            targetHoldSeconds = 0,
            feedbackMessage = feedback,
            isCompleted = isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart,
            activeAngles = anglesMap
        )
    }

    // -------------------------------------------------------------------------
    // 2. SQUATS (Full Standing Upright -> Deep Knee Bend -> Stand Up)
    // -------------------------------------------------------------------------
    private fun classifySquats(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val hasLeftLeg = (leftHip?.inFrameLikelihood ?: 0f) >= 0.5f &&
                         (leftKnee?.inFrameLikelihood ?: 0f) >= 0.5f &&
                         (leftAnkle?.inFrameLikelihood ?: 0f) >= 0.4f

        val hasRightLeg = (rightHip?.inFrameLikelihood ?: 0f) >= 0.5f &&
                          (rightKnee?.inFrameLikelihood ?: 0f) >= 0.5f &&
                          (rightAnkle?.inFrameLikelihood ?: 0f) >= 0.4f

        if (!hasLeftLeg && !hasRightLeg) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "Step back so hips, knees & feet are visible",
                isCompleted = false,
                isPostureCorrect = false,
                isReadyToStart = false
            )
        }

        val leftKneeAngle = if (hasLeftLeg) PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle) else -1.0
        val rightKneeAngle = if (hasRightLeg) PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle) else -1.0

        val activeKneeAngle = when {
            leftKneeAngle > 0 && rightKneeAngle > 0 -> (leftKneeAngle + rightKneeAngle) / 2.0
            leftKneeAngle > 0 -> leftKneeAngle
            rightKneeAngle > 0 -> rightKneeAngle
            else -> -1.0
        }

        val shoulder = leftShoulder ?: rightShoulder
        val hip = if (hasLeftLeg) leftHip else rightHip

        // Standing upright check: Torso is vertical in frame
        val isVerticalTorso = if (shoulder != null && hip != null) {
            abs(hip.position.y - shoulder.position.y) >= abs(hip.position.x - shoulder.position.x) * 1.1f
        } else true

        val isDeepSquat = activeKneeAngle in 45.0..118.0
        val isStandingUp = activeKneeAngle >= 155.0

        val isPostureCorrect = isVerticalTorso && activeKneeAngle > 0
        val isReadyToStart = isStandingUp && isPostureCorrect

        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (isDeepSquat && isPostureCorrect) {
                isDownPhase = true
            } else if (isStandingUp && isDownPhase && isPostureCorrect) {
                if (now - lastRepTimestampMs >= minRepDurationMs) {
                    reps += 1
                    lastRepTimestampMs = now
                    isDownPhase = false
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        }

        val angleText = if (activeKneeAngle > 0) " (${activeKneeAngle.toInt()}°)" else ""
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isPostureCorrect -> "Step back so your full body is in frame"
            isDownPhase -> "🟢 Deep squat reached! Now stand up straight"
            isStandingUp -> "Squat down with knees bent to 90°$angleText"
            else -> "Squat deeper to parallel$angleText"
        }

        val anglesMap = if (activeKneeAngle > 0) mapOf("knee" to activeKneeAngle) else emptyMap()

        return PoseClassificationResult(
            currentReps = reps,
            targetReps = effectiveTargetReps,
            isHolding = isDownPhase,
            holdSeconds = 0,
            targetHoldSeconds = 0,
            feedbackMessage = feedback,
            isCompleted = isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart,
            activeAngles = anglesMap
        )
    }

    // -------------------------------------------------------------------------
    // 3. SIT-UPS & CRUNCHES (Torso Flexion + Hip Hinge Elevation)
    // -------------------------------------------------------------------------
    private fun classifySitups(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip
        val knee = leftKnee ?: rightKnee

        val hasUpperBody = (shoulder?.inFrameLikelihood ?: 0f) >= 0.5f && (hip?.inFrameLikelihood ?: 0f) >= 0.45f

        if (!hasUpperBody) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "Lie on floor with head & torso in camera view",
                isCompleted = false,
                isPostureCorrect = false,
                isReadyToStart = false
            )
        }

        val leftHipAngle = PoseAngleCalculator.calculateAngle(leftShoulder, leftHip, leftKnee)
        val rightHipAngle = PoseAngleCalculator.calculateAngle(rightShoulder, rightHip, rightKnee)
        val activeHipAngle = when {
            leftHipAngle > 0 && rightHipAngle > 0 -> (leftHipAngle + rightHipAngle) / 2.0
            leftHipAngle > 0 -> leftHipAngle
            rightHipAngle > 0 -> rightHipAngle
            else -> -1.0
        }

        val isLyingFlat = (activeHipAngle >= 135.0) || (abs(shoulder!!.position.y - hip!!.position.y) < 65f)
        val isCurledUp = (activeHipAngle in 30.0..95.0) || (knee != null && shoulder!!.position.y < hip!!.position.y - 65f)

        val isPostureCorrect = isLyingFlat || isCurledUp || activeHipAngle > 0
        val isReadyToStart = isLyingFlat

        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (isCurledUp && isPostureCorrect) {
                isDownPhase = true
            } else if (isLyingFlat && isDownPhase && isPostureCorrect) {
                if (now - lastRepTimestampMs >= minRepDurationMs) {
                    reps += 1
                    lastRepTimestampMs = now
                    isDownPhase = false
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        }

        val angleText = if (activeHipAngle > 0) " (${activeHipAngle.toInt()}°)" else ""
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isPostureCorrect -> "Lie down on mat facing camera"
            isDownPhase -> "🟢 Reached top! Lower back to floor"
            isLyingFlat -> "Sit up and curl torso forward$angleText"
            else -> "Curl up toward your knees$angleText"
        }

        val anglesMap = if (activeHipAngle > 0) mapOf("hip" to activeHipAngle) else emptyMap()

        return PoseClassificationResult(
            currentReps = reps,
            targetReps = effectiveTargetReps,
            isHolding = isDownPhase,
            holdSeconds = 0,
            targetHoldSeconds = 0,
            feedbackMessage = feedback,
            isCompleted = isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart,
            activeAngles = anglesMap
        )
    }

    // -------------------------------------------------------------------------
    // 4. JUMPING JACKS (Dual-Arm Abduction + Stance Spread Synchronization)
    // -------------------------------------------------------------------------
    private fun classifyJumpingJacks(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val hasShoulders = (leftShoulder?.inFrameLikelihood ?: 0f) >= 0.5f || (rightShoulder?.inFrameLikelihood ?: 0f) >= 0.5f
        if (!hasShoulders) {
            return PoseClassificationResult(
                reps, effectiveTargetReps, false, 0, 0,
                "Position full body in camera view", isCompleted,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val leftArmAngle = PoseAngleCalculator.calculateAngle(leftHip, leftShoulder, leftWrist ?: leftElbow)
        val rightArmAngle = PoseAngleCalculator.calculateAngle(rightHip, rightShoulder, rightWrist ?: rightElbow)

        val handsAboveHead = (leftWrist != null && leftShoulder != null && leftWrist.position.y < leftShoulder.position.y) ||
                             (rightWrist != null && rightShoulder != null && rightWrist.position.y < rightShoulder.position.y) ||
                             (leftArmAngle >= 115.0 || rightArmAngle >= 115.0)

        val handsDownAtSides = (leftArmAngle in 0.0..60.0 || rightArmAngle in 0.0..60.0) ||
                               (leftWrist != null && leftShoulder != null && leftWrist.position.y > leftShoulder.position.y + 40f)

        val isPostureCorrect = hasShoulders
        val isReadyToStart = handsDownAtSides && isPostureCorrect

        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (handsAboveHead && isPostureCorrect) {
                isDownPhase = true
            } else if (handsDownAtSides && isDownPhase && isPostureCorrect) {
                if (now - lastRepTimestampMs >= 350L) {
                    reps += 1
                    lastRepTimestampMs = now
                    isDownPhase = false
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isPostureCorrect -> "Step back so full body is visible"
            isDownPhase -> "🟢 Arms overhead! Now jump back"
            else -> "Jump & raise arms overhead ($reps/$effectiveTargetReps)"
        }

        return PoseClassificationResult(
            reps, effectiveTargetReps, isDownPhase, 0, 0,
            feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart
        )
    }

    // -------------------------------------------------------------------------
    // 5. PLANK (Linear Spine Hold + Horizontal Alignment)
    // -------------------------------------------------------------------------
    private fun classifyPlank(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val hasLandmarks = (shoulder?.inFrameLikelihood ?: 0f) >= 0.5f && (hip?.inFrameLikelihood ?: 0f) >= 0.4f
        if (!hasLandmarks) {
            return PoseClassificationResult(
                0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds,
                "Lie horizontally on floor for plank", false,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val spineAngle = PoseAngleCalculator.calculateAngle(shoulder, hip, ankle ?: knee)
        val now = SystemClock.elapsedRealtime()

        val isAligned = (spineAngle in 140.0..195.0) || (abs(shoulder!!.position.y - hip!!.position.y) < 180f)
        val isPostureCorrect = isAligned
        val isReadyToStart = isPostureCorrect

        if (isCountingActive) {
            if (isPostureCorrect) {
                lastValidHoldTimeMs = now
                if (!isCurrentlyHolding) {
                    isCurrentlyHolding = true
                    holdStartTimeMs = now
                } else {
                    accumulatedHoldMs += (now - holdStartTimeMs)
                    holdStartTimeMs = now
                }
                val holdSec = (accumulatedHoldMs / 1000L).toInt()
                if (holdSec >= effectiveTargetHoldSeconds) isCompleted = true
            } else {
                if (now - lastValidHoldTimeMs > holdGraceWindowMs) {
                    isCurrentlyHolding = false
                }
                holdStartTimeMs = now
            }
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isPostureCorrect -> "🟢 Holding Plank! (${totalSec}/${effectiveTargetHoldSeconds}s)"
            else -> "⚠️ Align body horizontally for plank (hold paused)"
        }

        val anglesMap = if (spineAngle > 0) mapOf("spine" to spineAngle) else emptyMap()

        return PoseClassificationResult(
            totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding,
            totalSec, effectiveTargetHoldSeconds, feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart,
            activeAngles = anglesMap
        )
    }

    // -------------------------------------------------------------------------
    // 6. LUNGES (Alternating Knee Drops)
    // -------------------------------------------------------------------------
    private fun classifyLunges(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)

        val minKneeAngle = when {
            leftAngle > 0 && rightAngle > 0 -> min(leftAngle, rightAngle)
            leftAngle > 0 -> leftAngle
            rightAngle > 0 -> rightAngle
            else -> -1.0
        }

        val isPostureCorrect = leftAngle > 0 || rightAngle > 0
        val isReadyToStart = (leftAngle >= 150.0 || rightAngle >= 150.0) && isPostureCorrect

        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (minKneeAngle in 50.0..130.0 && isPostureCorrect) {
                isDownPhase = true
            } else if ((minKneeAngle >= 152.0 || minKneeAngle < 0) && isDownPhase && isPostureCorrect) {
                if (now - lastRepTimestampMs >= minRepDurationMs) {
                    reps += 1
                    lastRepTimestampMs = now
                    isDownPhase = false
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isPostureCorrect -> "Show legs and hips in camera view"
            isDownPhase -> "🟢 Lunge depth reached! Push back up"
            else -> "Step forward into a steady lunge ($reps/$effectiveTargetReps)"
        }

        val anglesMap = if (minKneeAngle > 0) mapOf("knee" to minKneeAngle) else emptyMap()

        return PoseClassificationResult(
            reps, effectiveTargetReps, isDownPhase, 0, 0,
            feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart,
            activeAngles = anglesMap
        )
    }

    // -------------------------------------------------------------------------
    // 7. WALL SIT (Multi-Point Knee & Hip Angle Hold)
    // -------------------------------------------------------------------------
    private fun classifyWallSit(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val hasLegs = (hip?.inFrameLikelihood ?: 0f) >= 0.5f && (knee?.inFrameLikelihood ?: 0f) >= 0.5f
        if (!hasLegs) {
            return PoseClassificationResult(
                0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds,
                "Position body against wall with hips and knees visible", false,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val kneeAngle = PoseAngleCalculator.calculateAngle(hip, knee, ankle)
        val hipAngle = PoseAngleCalculator.calculateAngle(shoulder, hip, knee)
        val now = SystemClock.elapsedRealtime()

        val isKneeBent = kneeAngle > 0 && kneeAngle in 60.0..140.0
        val isHipBent = hipAngle > 0 && hipAngle in 60.0..135.0

        val isPostureCorrect = (isKneeBent || isHipBent)
        val isReadyToStart = isPostureCorrect

        if (isCountingActive) {
            if (isPostureCorrect) {
                lastValidHoldTimeMs = now
                if (!isCurrentlyHolding) {
                    isCurrentlyHolding = true
                    holdStartTimeMs = now
                } else {
                    accumulatedHoldMs += (now - holdStartTimeMs)
                    holdStartTimeMs = now
                }
                val holdSec = (accumulatedHoldMs / 1000L).toInt()
                if (holdSec >= effectiveTargetHoldSeconds) isCompleted = true
            } else {
                if (now - lastValidHoldTimeMs > holdGraceWindowMs) {
                    isCurrentlyHolding = false
                }
                holdStartTimeMs = now
            }
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isPostureCorrect -> "🟢 Holding Wall Sit! (${totalSec}/${effectiveTargetHoldSeconds}s)"
            else -> "⚠️ Sit back with knees bent at 90° (timer paused)"
        }

        val anglesMap = mutableMapOf<String, Double>()
        if (kneeAngle > 0) anglesMap["knee"] = kneeAngle
        if (hipAngle > 0) anglesMap["hip"] = hipAngle

        return PoseClassificationResult(
            totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding,
            totalSec, effectiveTargetHoldSeconds, feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart,
            activeAngles = anglesMap
        )
    }

    // -------------------------------------------------------------------------
    // 8. HIGH KNEES
    // -------------------------------------------------------------------------
    private fun classifyHighKnees(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val hasHips = (leftHip?.inFrameLikelihood ?: 0f) >= 0.5f && (rightHip?.inFrameLikelihood ?: 0f) >= 0.5f
        if (!hasHips || leftKnee == null || rightKnee == null) {
            return PoseClassificationResult(
                reps, effectiveTargetReps, false, 0, 0,
                "Show hips and knees in camera view", isCompleted,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val torsoLength = if (shoulder != null) abs(leftHip!!.position.y - shoulder.position.y).coerceAtLeast(60f) else 120f
        val leftKneeHigh = (leftHip!!.position.y - leftKnee.position.y) / torsoLength > -0.3f
        val rightKneeHigh = (rightHip!!.position.y - rightKnee.position.y) / torsoLength > -0.3f
        val isPostureCorrect = true
        val isReadyToStart = true
        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (leftKneeHigh || rightKneeHigh) {
                if (!isDownPhase) {
                    isDownPhase = true
                    if (now - lastRepTimestampMs >= 350L) {
                        reps += 1
                        lastRepTimestampMs = now
                        if (reps >= effectiveTargetReps) isCompleted = true
                    }
                }
            } else {
                isDownPhase = false
            }
        }

        val feedback = if (isDownPhase) "🟢 High knee drive!" else "Drive knees up high! ($reps/$effectiveTargetReps)"
        return PoseClassificationResult(
            reps, effectiveTargetReps, isDownPhase, 0, 0,
            feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart
        )
    }

    // -------------------------------------------------------------------------
    // 9. CALF RAISES (Adaptive Baseline Heel Elevation)
    // -------------------------------------------------------------------------
    private fun classifyCalfRaises(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val hasLeftAnkle = leftAnkle != null && leftAnkle.inFrameLikelihood >= 0.45f
        val hasRightAnkle = rightAnkle != null && rightAnkle.inFrameLikelihood >= 0.45f
        val hasAnkles = hasLeftAnkle || hasRightAnkle

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip

        val currentElevationY: Float? = when {
            hasLeftAnkle && hasRightAnkle -> (leftAnkle!!.position.y + rightAnkle!!.position.y) / 2f
            hasLeftAnkle -> leftAnkle!!.position.y
            hasRightAnkle -> rightAnkle!!.position.y
            else -> null
        }

        if (currentElevationY == null) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "🧍 Point camera down so feet & ankles are visible",
                isCompleted = false,
                isPostureCorrect = false,
                isReadyToStart = false
            )
        }

        val torsoLength = if (shoulder != null && hip != null) abs(hip.position.y - shoulder.position.y).coerceAtLeast(60f) else 120f
        val liftThreshold = if (hasAnkles) 12f else (torsoLength * 0.04f).coerceAtLeast(10f)
        val resetThreshold = if (hasAnkles) 4f else (torsoLength * 0.015f).coerceAtLeast(3f)

        val baseline = calfAnkleBaselineY
        if (baseline == null) {
            calfAnkleBaselineY = currentElevationY
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "Stand flat, ready to rise onto toes",
                isCompleted = false,
                isPostureCorrect = true,
                isReadyToStart = true
            )
        }

        val liftDelta = baseline - currentElevationY
        val isPostureCorrect = true
        val isReadyToStart = liftDelta < liftThreshold
        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (liftDelta >= liftThreshold) {
                isDownPhase = true
            } else if (liftDelta <= resetThreshold && isDownPhase) {
                if (now - lastRepTimestampMs >= minRepDurationMs) {
                    reps += 1
                    lastRepTimestampMs = now
                    isDownPhase = false
                    calfAnkleBaselineY = (baseline * 0.8f) + (currentElevationY * 0.2f)
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            } else if (currentElevationY > baseline + 15f) {
                calfAnkleBaselineY = currentElevationY
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Good heel lift! Now lower down slowly"
            else -> "Rise high onto your toes ($reps/$effectiveTargetReps)"
        }

        val anglesMap = mapOf("elevation" to liftDelta.toDouble())

        return PoseClassificationResult(
            reps, effectiveTargetReps, isDownPhase, 0, 0,
            feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart,
            activeAngles = anglesMap
        )
    }

    // -------------------------------------------------------------------------
    // 10. TREE POSE (Single-Leg Balance + High Foot)
    // -------------------------------------------------------------------------
    private fun classifyTreePose(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val hasLeft = (leftAnkle?.inFrameLikelihood ?: 0f) >= 0.5f && (leftKnee?.inFrameLikelihood ?: 0f) >= 0.5f
        val hasRight = (rightAnkle?.inFrameLikelihood ?: 0f) >= 0.5f && (rightKnee?.inFrameLikelihood ?: 0f) >= 0.5f

        if (!hasLeft || !hasRight) {
            return PoseClassificationResult(
                0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds,
                "🧍 Full standing body in frame", false,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val leftKneeAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)

        val isRightStanding = rightKneeAngle in 145.0..185.0 && (leftAnkle!!.position.y < rightAnkle!!.position.y - 30f) && (leftKneeAngle in 30.0..140.0)
        val isLeftStanding = leftKneeAngle in 145.0..185.0 && (rightAnkle!!.position.y < leftAnkle!!.position.y - 30f) && (rightKneeAngle in 30.0..140.0)

        val isValidTreePose = isRightStanding || isLeftStanding
        val isPostureCorrect = isValidTreePose
        val isReadyToStart = isValidTreePose
        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (isValidTreePose) {
                lastValidHoldTimeMs = now
                if (!isCurrentlyHolding) {
                    isCurrentlyHolding = true
                    holdStartTimeMs = now
                } else {
                    accumulatedHoldMs += (now - holdStartTimeMs)
                    holdStartTimeMs = now
                }
                val holdSec = (accumulatedHoldMs / 1000L).toInt()
                if (holdSec >= effectiveTargetHoldSeconds) isCompleted = true
            } else {
                if (now - lastValidHoldTimeMs > holdGraceWindowMs) {
                    isCurrentlyHolding = false
                }
                holdStartTimeMs = now
            }
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isValidTreePose) "🟢 Holding Tree Pose! (${totalSec}/${effectiveTargetHoldSeconds}s) 🌳" else "⚠️ Stand on 1 leg, place other foot on calf (timer paused)"
        return PoseClassificationResult(
            totalSec, effectiveTargetHoldSeconds, isValidTreePose,
            totalSec, effectiveTargetHoldSeconds, feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart
        )
    }

    // -------------------------------------------------------------------------
    // 11. MOUNTAIN POSE (Tadasana: Grounded Standing Alignment)
    // -------------------------------------------------------------------------
    private fun classifyMountainPose(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip

        if (shoulder == null || hip == null) {
            return PoseClassificationResult(
                0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds,
                "🧍 Full posture in frame", false,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val isUpright = (hip.position.y - shoulder.position.y) > 30f
        val isPostureCorrect = isUpright
        val isReadyToStart = isUpright
        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (isUpright) {
                lastValidHoldTimeMs = now
                if (!isCurrentlyHolding) {
                    isCurrentlyHolding = true
                    holdStartTimeMs = now
                } else {
                    accumulatedHoldMs += (now - holdStartTimeMs)
                    holdStartTimeMs = now
                }
                val holdSec = (accumulatedHoldMs / 1000L).toInt()
                if (holdSec >= effectiveTargetHoldSeconds) isCompleted = true
            } else {
                if (now - lastValidHoldTimeMs > holdGraceWindowMs) {
                    isCurrentlyHolding = false
                }
                holdStartTimeMs = now
            }
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isUpright) "🟢 Holding Mountain Pose (${totalSec}/${effectiveTargetHoldSeconds}s) 🏔️" else "Stand tall and steady with spine aligned"
        return PoseClassificationResult(
            totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding,
            totalSec, effectiveTargetHoldSeconds, feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart
        )
    }

    // -------------------------------------------------------------------------
    // 12. FORWARD FOLD
    // -------------------------------------------------------------------------
    private fun classifyForwardFold(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        if (shoulder == null || hip == null) {
            return PoseClassificationResult(
                0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds,
                "Step back so body is in frame", false,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val hipAngle = if (knee != null) PoseAngleCalculator.calculateAngle(shoulder, hip, knee) else -1.0
        val isFolded = (hipAngle in 20.0..115.0) || (abs(shoulder.position.y - hip.position.y) < 80f)
        val isPostureCorrect = isFolded
        val isReadyToStart = isFolded
        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (isFolded) {
                lastValidHoldTimeMs = now
                if (!isCurrentlyHolding) {
                    isCurrentlyHolding = true
                    holdStartTimeMs = now
                } else {
                    accumulatedHoldMs += (now - holdStartTimeMs)
                    holdStartTimeMs = now
                }
                val holdSec = (accumulatedHoldMs / 1000L).toInt()
                if (holdSec >= effectiveTargetHoldSeconds) isCompleted = true
            } else {
                if (now - lastValidHoldTimeMs > holdGraceWindowMs) {
                    isCurrentlyHolding = false
                }
                holdStartTimeMs = now
            }
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isFolded) "🟢 Holding Forward Fold! (${totalSec}/${effectiveTargetHoldSeconds}s)" else "Bend forward at your hips (timer paused)"
        return PoseClassificationResult(
            totalSec, effectiveTargetHoldSeconds, isFolded,
            totalSec, effectiveTargetHoldSeconds, feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart
        )
    }

    private fun classifySitToStand(pose: Pose, isCountingActive: Boolean): PoseClassificationResult = classifySquats(pose, isCountingActive)

    private fun classifyStandAndShake(pose: Pose, isCountingActive: Boolean): PoseClassificationResult {
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        var totalMotionDelta = 0f

        leftWrist?.let {
            val curr = Pair(it.position.x, it.position.y)
            prevLeftWristPos?.let { prev -> totalMotionDelta += abs(curr.first - prev.first) + abs(curr.second - prev.second) }
            prevLeftWristPos = curr
        }

        rightWrist?.let {
            val curr = Pair(it.position.x, it.position.y)
            prevRightWristPos?.let { prev -> totalMotionDelta += abs(curr.first - prev.first) + abs(curr.second - prev.second) }
            prevRightWristPos = curr
        }

        val isShaking = totalMotionDelta >= 15f
        val isPostureCorrect = isShaking || leftWrist != null || rightWrist != null
        val isReadyToStart = true
        val now = SystemClock.elapsedRealtime()

        if (isCountingActive) {
            if (isShaking) {
                lastValidHoldTimeMs = now
                if (!isCurrentlyHolding) {
                    isCurrentlyHolding = true
                    holdStartTimeMs = now
                } else {
                    accumulatedHoldMs += (now - holdStartTimeMs)
                    holdStartTimeMs = now
                }
                val holdSec = (accumulatedHoldMs / 1000L).toInt()
                if (holdSec >= effectiveTargetHoldSeconds) isCompleted = true
            } else {
                if (now - lastValidHoldTimeMs > holdGraceWindowMs) {
                    isCurrentlyHolding = false
                }
                holdStartTimeMs = now
            }
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isShaking) "🟢 Great energy! Keep shaking (${totalSec}/${effectiveTargetHoldSeconds}s) ✨" else "Stand up and shake your arms & legs!"
        return PoseClassificationResult(
            totalSec, effectiveTargetHoldSeconds, isShaking,
            totalSec, effectiveTargetHoldSeconds, feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart
        )
    }

    private fun classifyChildPose(pose: Pose, isCountingActive: Boolean): PoseClassificationResult = classifyForwardFold(pose, isCountingActive)
    private fun classifyCatCow(pose: Pose, isCountingActive: Boolean): PoseClassificationResult = classifyPlank(pose, isCountingActive)
    private fun classifyCobraStretch(pose: Pose, isCountingActive: Boolean): PoseClassificationResult = classifyPlank(pose, isCountingActive)
    private fun classifyShoulderStretch(pose: Pose, isCountingActive: Boolean): PoseClassificationResult = classifyMountainPose(pose, isCountingActive)
    private fun classifySeatedSpinalTwist(pose: Pose, isCountingActive: Boolean): PoseClassificationResult = classifyMountainPose(pose, isCountingActive)
    private fun classifyMiniSunSalutation(pose: Pose, isCountingActive: Boolean): PoseClassificationResult = classifyMountainPose(pose, isCountingActive)
    private fun classifyGeneralMovement(pose: Pose, isCountingActive: Boolean): PoseClassificationResult = classifyStandAndShake(pose, isCountingActive)
}

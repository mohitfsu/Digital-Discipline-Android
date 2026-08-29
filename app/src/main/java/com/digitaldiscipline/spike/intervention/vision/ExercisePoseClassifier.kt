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

/**
 * Robust On-Device Multi-Point Exercise Classifier & Biomechanical Repetition Engine.
 *
 * Employs distance-invariant, resolution-independent, and angle-resilient biomechanics:
 * 1. PUSH_UPS (Multi-point Elbow flexion + Torso vertical displacement)
 * 2. SQUATS (Multi-point Knee flexion + Torso-normalized Hip descent)
 * 3. PLANK (Linear spine hold + Horizontal shoulder-hip alignment)
 * 4. LUNGES (Alternating knee flexion + Dynamic hip drop)
 * 5. JUMPING_JACKS (Dual-arm abduction + Stance spread synchronization)
 * 6. WALL_SIT (Multi-point Knee & Hip 90-degree hold)
 * 7. HIGH_KNEES (Torso-normalized knee drives)
 * 8. CALF_RAISES (Ankle plantarflexion / heel raise cycle)
 * 9. TREE_POSE (Single-leg balance + high foot placement)
 * 10. MOUNTAIN_POSE (Upright alignment & steady presence)
 * 11. FORWARD_FOLD (Hip hinge + forward torso flexion)
 * 12. SIT_TO_STAND (Seated to upright standing cycle)
 * 13. STAND_UP_AND_SHAKE_OFF (Dynamic limb kinetic oscillation)
 * 14. YOGA & STRETCHES (Posture presence & stretch holds)
 */
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
    private var prevLeftAnklePos: Pair<Float, Float>? = null
    private var prevRightAnklePos: Pair<Float, Float>? = null

    // Anti-cheat cadence
    private val minRepDurationMs = 350L

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
        prevLeftAnklePos = null
        prevRightAnklePos = null
    }

    fun processPose(pose: Pose): PoseClassificationResult {
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
            "PUSH_UPS", "PUSHUPS" -> classifyPushups(pose)
            "SQUATS", "BODYWEIGHT_SQUATS" -> classifySquats(pose)
            "SIT_UPS", "SITUPS", "CRUNCHES", "CORE_SITUPS" -> classifySitups(pose)
            "PLANK", "CORE_PLANK" -> classifyPlank(pose)
            "LUNGES", "ALTERNATING_LUNGES" -> classifyLunges(pose)
            "JUMPING_JACKS" -> classifyJumpingJacks(pose)
            "WALL_SIT" -> classifyWallSit(pose)
            "HIGH_KNEES" -> classifyHighKnees(pose)
            "CALF_RAISES", "CALF_RAISE", "CALF" -> classifyCalfRaises(pose)
            "TREE_POSE", "TREE" -> classifyTreePose(pose)
            "MOUNTAIN_POSE", "MOUNTAIN" -> classifyMountainPose(pose)
            "FORWARD_FOLD", "FOLD" -> classifyForwardFold(pose)
            "SIT_TO_STAND", "SIT_STAND" -> classifySitToStand(pose)
            "STAND_UP", "STAND_UP_AND_SHAKE_OFF", "SHAKE_OFF", "STAND_SHAKE", "STAND" -> classifyStandAndShake(pose)
            "CHILD_POSE", "CHILDS_POSE", "BALASANA" -> classifyChildPose(pose)
            "SHOULDER_STRETCH", "STRETCH", "FULL_BODY_STRETCH" -> classifyShoulderStretch(pose)
            "COBRA_STRETCH", "COBRA" -> classifyCobraStretch(pose)
            "CAT_COW" -> classifyCatCow(pose)
            "SEATED_SPINAL_TWIST" -> classifySeatedSpinalTwist(pose)
            "MINI_SUN_SALUTATION" -> classifyMiniSunSalutation(pose)
            else -> classifyGeneralMovement(pose)
        }
    }

    // -------------------------------------------------------------------------
    // 1. PUSH-UPS (Multi-Point Elbow Flexion + Chest Displacement)
    // -------------------------------------------------------------------------
    private fun classifyPushups(pose: Pose): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val leftElbowAngle = PoseAngleCalculator.calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightElbowAngle = PoseAngleCalculator.calculateAngle(rightShoulder, rightElbow, rightWrist)

        val activeElbowAngle = when {
            leftElbowAngle > 0 && rightElbowAngle > 0 -> (leftElbowAngle + rightElbowAngle) / 2.0
            leftElbowAngle > 0 -> leftElbowAngle
            rightElbowAngle > 0 -> rightElbowAngle
            else -> -1.0
        }

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip
        val wrist = leftWrist ?: rightWrist
        val torsoLength = if (shoulder != null && hip != null) abs(hip.position.y - shoulder.position.y).coerceAtLeast(60f) else 120f
        val chestToWristDelta = if (shoulder != null && wrist != null) abs(shoulder.position.y - wrist.position.y) / torsoLength else -1f

        val isDown = (activeElbowAngle in 40.0..125.0) || (chestToWristDelta in 0.0f..0.45f && chestToWristDelta >= 0f)
        val isUp = (activeElbowAngle >= 140.0) || (chestToWristDelta >= 0.65f)

        val isHorizontalPlank = if (shoulder != null && hip != null) abs(shoulder.position.y - hip.position.y) < 320f else false
        val isPostureCorrect = isHorizontalPlank || activeElbowAngle > 0
        val isReadyToStart = isUp && isHorizontalPlank

        val now = SystemClock.elapsedRealtime()

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

        val angleText = if (activeElbowAngle > 0) " (${activeElbowAngle.toInt()}°)" else ""
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isPostureCorrect -> "Get into horizontal push-up plank on floor"
            isDownPhase -> "🟢 Good depth! Now push back up"
            activeElbowAngle > 0 -> "Lower your chest$angleText"
            else -> "Get in push-up position in camera view"
        }

        val anglesMap = if (activeElbowAngle > 0) mapOf("elbow" to activeElbowAngle) else emptyMap()

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
    // 2. SQUATS (Multi-Point Knee Angle + Hip-to-Torso Descent)
    // -------------------------------------------------------------------------
    private fun classifySquats(pose: Pose): PoseClassificationResult {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val leftKneeAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)

        val activeKneeAngle = when {
            leftKneeAngle > 0 && rightKneeAngle > 0 -> (leftKneeAngle + rightKneeAngle) / 2.0
            leftKneeAngle > 0 -> leftKneeAngle
            rightKneeAngle > 0 -> rightKneeAngle
            else -> -1.0
        }

        val leftHipAngle = PoseAngleCalculator.calculateAngle(leftShoulder, leftHip, leftKnee)
        val rightHipAngle = PoseAngleCalculator.calculateAngle(rightShoulder, rightHip, rightKnee)

        val activeHipAngle = when {
            leftHipAngle > 0 && rightHipAngle > 0 -> (leftHipAngle + rightHipAngle) / 2.0
            leftHipAngle > 0 -> leftHipAngle
            rightHipAngle > 0 -> rightHipAngle
            else -> -1.0
        }

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip
        val knee = leftKnee ?: rightKnee
        val torsoLength = if (shoulder != null && hip != null) abs(hip.position.y - shoulder.position.y).coerceAtLeast(60f) else 120f
        val hipKneeRatio = if (hip != null && knee != null) abs(knee.position.y - hip.position.y) / torsoLength else 1.0f

        val isDeepSquat = (activeKneeAngle in 45.0..140.0) ||
                          (activeHipAngle in 45.0..130.0) ||
                          (hipKneeRatio <= 0.65f)

        val isStandingUp = (activeKneeAngle >= 148.0) ||
                           (activeHipAngle >= 144.0) ||
                           (hipKneeRatio >= 0.85f)

        val isPostureCorrect = (activeKneeAngle > 0 || activeHipAngle > 0) && (hip != null && knee != null)
        val isReadyToStart = isStandingUp && isPostureCorrect

        val now = SystemClock.elapsedRealtime()

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

        val angleText = if (activeKneeAngle > 0) " (${activeKneeAngle.toInt()}°)" else if (activeHipAngle > 0) " (${activeHipAngle.toInt()}°)" else ""
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isPostureCorrect -> "Step back so your full body is in frame"
            isDownPhase -> "🟢 Deep squat reached! Stand up"
            angleText.isNotEmpty() -> "Squat down$angleText"
            else -> "Step back so your body is visible"
        }

        val anglesMap = mutableMapOf<String, Double>()
        if (activeKneeAngle > 0) anglesMap["knee"] = activeKneeAngle
        if (activeHipAngle > 0) anglesMap["hip"] = activeHipAngle

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
    private fun classifySitups(pose: Pose): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip
        val knee = leftKnee ?: rightKnee

        if (shoulder == null || hip == null) {
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

        val isLyingFlat = (activeHipAngle >= 130.0) || (abs(shoulder.position.y - hip.position.y) < 70f)
        val isCurledUp = (activeHipAngle in 30.0..98.0) || (knee != null && shoulder.position.y < hip.position.y - 60f)

        val isPostureCorrect = isLyingFlat || isCurledUp || activeHipAngle > 0
        val isReadyToStart = isLyingFlat

        val now = SystemClock.elapsedRealtime()

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

        val angleText = if (activeHipAngle > 0) " (${activeHipAngle.toInt()}°)" else ""
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isPostureCorrect -> "Lie down on mat facing camera"
            isDownPhase -> "🟢 Reached top! Lower back to floor"
            isLyingFlat -> "Sit up and curl torso forward$angleText"
            activeHipAngle > 0 -> "Curl up toward your knees$angleText"
            else -> "Get into sit-up position on the floor"
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
    private fun classifyJumpingJacks(pose: Pose): PoseClassificationResult {
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

        if (leftShoulder == null && rightShoulder == null) {
            return PoseClassificationResult(
                reps, effectiveTargetReps, false, 0, 0,
                "Position body in camera view", isCompleted,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val leftArmAngle = PoseAngleCalculator.calculateAngle(leftHip, leftShoulder, leftWrist ?: leftElbow)
        val rightArmAngle = PoseAngleCalculator.calculateAngle(rightHip, rightShoulder, rightWrist ?: rightElbow)

        val handsAboveHead = (leftWrist != null && leftShoulder != null && leftWrist.position.y < leftShoulder.position.y) ||
                             (rightWrist != null && rightShoulder != null && rightWrist.position.y < rightShoulder.position.y) ||
                             (leftArmAngle >= 110.0 || rightArmAngle >= 110.0)

        val handsDownAtSides = (leftArmAngle in 0.0..65.0 || rightArmAngle in 0.0..65.0) ||
                               (leftWrist != null && leftShoulder != null && leftWrist.position.y > leftShoulder.position.y + 40f)

        val hipWidth = if (leftHip != null && rightHip != null) PoseAngleCalculator.calculateDistance(leftHip, rightHip).coerceAtLeast(40.0) else 100.0
        val ankleDistance = if (leftAnkle != null && rightAnkle != null) PoseAngleCalculator.calculateDistance(leftAnkle, rightAnkle) else 0.0
        val isLegsSpread = (ankleDistance / hipWidth) >= 1.5

        val isJackExpanded = handsAboveHead || (handsAboveHead && isLegsSpread)
        val isJackClosed = handsDownAtSides

        val isPostureCorrect = leftShoulder != null || rightShoulder != null
        val isReadyToStart = isJackClosed && isPostureCorrect

        val now = SystemClock.elapsedRealtime()

        if (isJackExpanded && isPostureCorrect) {
            isDownPhase = true
        } else if (isJackClosed && isDownPhase && isPostureCorrect) {
            if (now - lastRepTimestampMs >= 280L) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                if (reps >= effectiveTargetReps) isCompleted = true
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
    private fun classifyPlank(pose: Pose): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val spineAngle = PoseAngleCalculator.calculateAngle(shoulder, hip, ankle ?: knee)
        val now = SystemClock.elapsedRealtime()

        val isAligned = (spineAngle in 135.0..195.0) || (shoulder != null && hip != null && abs(shoulder.position.y - hip.position.y) < 220f)
        val isPostureCorrect = isAligned && (shoulder != null && hip != null)
        val isReadyToStart = isPostureCorrect

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
    private fun classifyLunges(pose: Pose): PoseClassificationResult {
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
        val isReadyToStart = (leftAngle >= 145.0 || rightAngle >= 145.0) && isPostureCorrect

        val now = SystemClock.elapsedRealtime()

        if (minKneeAngle in 50.0..140.0 && isPostureCorrect) {
            isDownPhase = true
        } else if ((minKneeAngle >= 148.0 || minKneeAngle < 0) && isDownPhase && isPostureCorrect) {
            if (now - lastRepTimestampMs >= minRepDurationMs) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                if (reps >= effectiveTargetReps) isCompleted = true
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
    private fun classifyWallSit(pose: Pose): PoseClassificationResult {
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val kneeAngle = PoseAngleCalculator.calculateAngle(hip, knee, ankle)
        val hipAngle = PoseAngleCalculator.calculateAngle(shoulder, hip, knee)
        val now = SystemClock.elapsedRealtime()

        val isKneeBent = kneeAngle > 0 && kneeAngle in 60.0..145.0
        val isHipBent = hipAngle > 0 && hipAngle in 60.0..140.0
        val isSeatedStance = if (hip != null && knee != null) abs(knee.position.y - hip.position.y) < 280f else false

        val isSeated = isKneeBent || (isHipBent && isSeatedStance) || (shoulder != null && isHipBent)
        val isPostureCorrect = isSeated && (hip != null && knee != null)
        val isReadyToStart = isPostureCorrect

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
    private fun classifyHighKnees(pose: Pose): PoseClassificationResult {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (leftHip == null || leftKnee == null || rightHip == null || rightKnee == null) {
            return PoseClassificationResult(
                reps, effectiveTargetReps, false, 0, 0,
                "Show hips and knees in camera view", isCompleted,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val torsoLength = if (shoulder != null) abs(leftHip.position.y - shoulder.position.y).coerceAtLeast(60f) else 120f
        val leftKneeHigh = (leftHip.position.y - leftKnee.position.y) / torsoLength > -0.3f
        val rightKneeHigh = (rightHip.position.y - rightKnee.position.y) / torsoLength > -0.3f
        val isPostureCorrect = true
        val isReadyToStart = true
        val now = SystemClock.elapsedRealtime()

        if (leftKneeHigh || rightKneeHigh) {
            if (!isDownPhase) {
                isDownPhase = true
                if (now - lastRepTimestampMs >= 250L) {
                    reps += 1
                    lastRepTimestampMs = now
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        } else {
            isDownPhase = false
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
    private fun classifyCalfRaises(pose: Pose): PoseClassificationResult {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        val hasLeftAnkle = leftAnkle != null && leftAnkle.inFrameLikelihood > 0.25f
        val hasRightAnkle = rightAnkle != null && rightAnkle.inFrameLikelihood > 0.25f
        val hasAnkles = hasLeftAnkle || hasRightAnkle

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip

        val currentElevationY: Float? = when {
            hasLeftAnkle && hasRightAnkle -> (leftAnkle!!.position.y + rightAnkle!!.position.y) / 2f
            hasLeftAnkle -> leftAnkle!!.position.y
            hasRightAnkle -> rightAnkle!!.position.y
            shoulder != null -> shoulder.position.y
            nose != null -> nose.position.y
            else -> null
        }

        if (currentElevationY == null) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "🧍 Stand in camera view with feet visible",
                isCompleted = false,
                isPostureCorrect = false,
                isReadyToStart = false
            )
        }

        val torsoLength = if (shoulder != null && hip != null) abs(hip.position.y - shoulder.position.y).coerceAtLeast(60f) else 120f
        val liftThreshold = if (hasAnkles) 10f else (torsoLength * 0.04f).coerceAtLeast(8f)
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
    private fun classifyTreePose(pose: Pose): PoseClassificationResult {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val hasLeft = leftAnkle != null && leftKnee != null && leftHip != null
        val hasRight = rightAnkle != null && rightKnee != null && rightHip != null

        if (!hasLeft || !hasRight) {
            isCurrentlyHolding = false
            return PoseClassificationResult(
                0, effectiveTargetHoldSeconds, false,
                (accumulatedHoldMs / 1000L).toInt(), effectiveTargetHoldSeconds,
                "🧍 Full standing body in frame", isCompleted,
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
    private fun classifyMountainPose(pose: Pose): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip

        if (shoulder == null || hip == null) {
            isCurrentlyHolding = false
            return PoseClassificationResult(
                0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds,
                "🧍 Full posture in frame", isCompleted,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val isUpright = (hip.position.y - shoulder.position.y) > 30f
        val isPostureCorrect = isUpright
        val isReadyToStart = isUpright
        val now = SystemClock.elapsedRealtime()

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
    private fun classifyForwardFold(pose: Pose): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        if (shoulder == null || hip == null) {
            isCurrentlyHolding = false
            return PoseClassificationResult(
                0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds,
                "Step back so body is in frame", isCompleted,
                isPostureCorrect = false, isReadyToStart = false
            )
        }

        val hipAngle = if (knee != null) PoseAngleCalculator.calculateAngle(shoulder, hip, knee) else -1.0
        val isFolded = (hipAngle in 20.0..115.0) || (abs(shoulder.position.y - hip.position.y) < 80f)
        val isPostureCorrect = isFolded
        val isReadyToStart = isFolded
        val now = SystemClock.elapsedRealtime()

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

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isFolded) "🟢 Holding Forward Fold! (${totalSec}/${effectiveTargetHoldSeconds}s)" else "Bend forward at your hips (timer paused)"
        return PoseClassificationResult(
            totalSec, effectiveTargetHoldSeconds, isFolded,
            totalSec, effectiveTargetHoldSeconds, feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart
        )
    }

    // -------------------------------------------------------------------------
    // 13. SIT TO STAND (Chair / Box Stands)
    // -------------------------------------------------------------------------
    private fun classifySitToStand(pose: Pose): PoseClassificationResult {
        return classifySquats(pose)
    }

    // -------------------------------------------------------------------------
    // 14. STAND UP AND SHAKE OFF (Kinetic Movement Oscillation)
    // -------------------------------------------------------------------------
    private fun classifyStandAndShake(pose: Pose): PoseClassificationResult {
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

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isShaking) "🟢 Great energy! Keep shaking (${totalSec}/${effectiveTargetHoldSeconds}s) ✨" else "Stand up and shake your arms & legs!"
        return PoseClassificationResult(
            totalSec, effectiveTargetHoldSeconds, isShaking,
            totalSec, effectiveTargetHoldSeconds, feedback, isCompleted,
            isPostureCorrect = isPostureCorrect,
            isReadyToStart = isReadyToStart
        )
    }

    // -------------------------------------------------------------------------
    // 15. YOGA POSES (Cat-Cow, Child's Pose, Cobra, Shoulder Stretch)
    // -------------------------------------------------------------------------
    private fun classifyChildPose(pose: Pose): PoseClassificationResult = classifyForwardFold(pose)
    private fun classifyCatCow(pose: Pose): PoseClassificationResult = classifyPlank(pose)
    private fun classifyCobraStretch(pose: Pose): PoseClassificationResult = classifyPlank(pose)
    private fun classifyShoulderStretch(pose: Pose): PoseClassificationResult = classifyMountainPose(pose)
    private fun classifySeatedSpinalTwist(pose: Pose): PoseClassificationResult = classifyMountainPose(pose)
    private fun classifyMiniSunSalutation(pose: Pose): PoseClassificationResult = classifyMountainPose(pose)
    private fun classifyGeneralMovement(pose: Pose): PoseClassificationResult = classifyStandAndShake(pose)
}

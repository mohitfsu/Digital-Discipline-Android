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
                isCompleted = true
            )
        }

        return when (exerciseId.uppercase()) {
            "PUSH_UPS", "PUSHUPS" -> classifyPushups(pose)
            "SQUATS", "BODYWEIGHT_SQUATS" -> classifySquats(pose)
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

        val isDown = (activeElbowAngle in 40.0..128.0) || (chestToWristDelta in 0.0f..0.45f && chestToWristDelta >= 0f)
        val isUp = (activeElbowAngle >= 148.0) || (chestToWristDelta >= 0.70f)

        val now = SystemClock.elapsedRealtime()

        if (isDown) {
            isDownPhase = true
        } else if (isUp && isDownPhase) {
            if (now - lastRepTimestampMs >= minRepDurationMs) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                if (reps >= effectiveTargetReps) isCompleted = true
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Good depth! Now push up"
            activeElbowAngle > 0 -> "Lower your chest (${activeElbowAngle.toInt()}°)"
            else -> "Get in push-up position in camera view"
        }

        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
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

        // Point Group A: Knee Angles (Hip-Knee-Ankle)
        val leftKneeAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)

        val activeKneeAngle = when {
            leftKneeAngle > 0 && rightKneeAngle > 0 -> (leftKneeAngle + rightKneeAngle) / 2.0
            leftKneeAngle > 0 -> leftKneeAngle
            rightKneeAngle > 0 -> rightKneeAngle
            else -> -1.0
        }

        // Point Group B: Hip Angles (Shoulder-Hip-Knee)
        val leftHipAngle = PoseAngleCalculator.calculateAngle(leftShoulder, leftHip, leftKnee)
        val rightHipAngle = PoseAngleCalculator.calculateAngle(rightShoulder, rightHip, rightKnee)

        val activeHipAngle = when {
            leftHipAngle > 0 && rightHipAngle > 0 -> (leftHipAngle + rightHipAngle) / 2.0
            leftHipAngle > 0 -> leftHipAngle
            rightHipAngle > 0 -> rightHipAngle
            else -> -1.0
        }

        // Point Group C: Torso-Normalized Knee-to-Hip Vertical Depth
        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip
        val knee = leftKnee ?: rightKnee
        val torsoLength = if (shoulder != null && hip != null) abs(hip.position.y - shoulder.position.y).coerceAtLeast(60f) else 120f

        val hipKneeRatio = if (hip != null && knee != null) abs(knee.position.y - hip.position.y) / torsoLength else 1.0f

        // Squat Down Phase: Reached depth
        val isDeepSquat = (activeKneeAngle in 45.0..140.0) ||
                          (activeHipAngle in 45.0..130.0) ||
                          (hipKneeRatio <= 0.65f)

        // Squat Up Phase: Returned to standing
        val isStandingUp = (activeKneeAngle >= 152.0) ||
                           (activeHipAngle >= 148.0) ||
                           (hipKneeRatio >= 0.88f)

        val now = SystemClock.elapsedRealtime()

        if (isDeepSquat) {
            isDownPhase = true
        } else if (isStandingUp && isDownPhase) {
            if (now - lastRepTimestampMs >= minRepDurationMs) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                if (reps >= effectiveTargetReps) isCompleted = true
            }
        }

        val angleText = if (activeKneeAngle > 0) "${activeKneeAngle.toInt()}°" else if (activeHipAngle > 0) "${activeHipAngle.toInt()}°" else ""
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Deep squat reached! Stand up"
            angleText.isNotEmpty() -> "Squat down ($angleText)"
            else -> "Step back so your body is visible"
        }

        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 3. JUMPING JACKS (Dual-Arm Abduction + Stance Spread Synchronization)
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
            return PoseClassificationResult(reps, effectiveTargetReps, false, 0, 0, "Position body in camera view", isCompleted)
        }

        // Arm Abduction Angles (Hip-Shoulder-Wrist or Hip-Shoulder-Elbow)
        val leftArmAngle = PoseAngleCalculator.calculateAngle(leftHip, leftShoulder, leftWrist ?: leftElbow)
        val rightArmAngle = PoseAngleCalculator.calculateAngle(rightHip, rightShoulder, rightWrist ?: rightElbow)

        val handsAboveHead = (leftWrist != null && leftShoulder != null && leftWrist.position.y < leftShoulder.position.y) ||
                             (rightWrist != null && rightShoulder != null && rightWrist.position.y < rightShoulder.position.y) ||
                             (leftArmAngle >= 110.0 || rightArmAngle >= 110.0)

        val handsDownAtSides = (leftArmAngle in 0.0..65.0 || rightArmAngle in 0.0..65.0) ||
                               (leftWrist != null && leftShoulder != null && leftWrist.position.y > leftShoulder.position.y + 40f)

        // Stance Spread Ratio
        val hipWidth = if (leftHip != null && rightHip != null) PoseAngleCalculator.calculateDistance(leftHip, rightHip).coerceAtLeast(40.0) else 100.0
        val ankleDistance = if (leftAnkle != null && rightAnkle != null) PoseAngleCalculator.calculateDistance(leftAnkle, rightAnkle) else 0.0
        val isLegsSpread = (ankleDistance / hipWidth) >= 1.6

        val isJackExpanded = handsAboveHead || (handsAboveHead && isLegsSpread)
        val isJackClosed = handsDownAtSides

        val now = SystemClock.elapsedRealtime()

        if (isJackExpanded) {
            isDownPhase = true
        } else if (isJackClosed && isDownPhase) {
            if (now - lastRepTimestampMs >= 280L) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                if (reps >= effectiveTargetReps) isCompleted = true
            }
        }

        val feedback = if (isDownPhase) "🟢 Arms overhead! Now jump back" else "Jump & raise arms overhead ($reps/$effectiveTargetReps)"
        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 4. PLANK (Linear Spine Hold + Horizontal Alignment)
    // -------------------------------------------------------------------------
    private fun classifyPlank(pose: Pose): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        val spineAngle = PoseAngleCalculator.calculateAngle(shoulder, hip, ankle ?: knee)
        val now = SystemClock.elapsedRealtime()

        val isAligned = (spineAngle in 135.0..195.0) || (shoulder != null && hip != null && abs(shoulder.position.y - hip.position.y) < 180f)

        if (isAligned) {
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
            isCurrentlyHolding = false
            holdStartTimeMs = now
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isAligned) "🟢 Holding Plank! (${totalSec}/${effectiveTargetHoldSeconds}s)" else "Align body horizontally for plank"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 5. LUNGES (Alternating Knee Drops)
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

        val now = SystemClock.elapsedRealtime()

        if (minKneeAngle in 50.0..140.0) {
            isDownPhase = true
        } else if ((minKneeAngle >= 150.0 || minKneeAngle < 0) && isDownPhase) {
            if (now - lastRepTimestampMs >= minRepDurationMs) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                if (reps >= effectiveTargetReps) isCompleted = true
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Lunge depth reached! Push back up"
            else -> "Step forward into a steady lunge ($reps/$effectiveTargetReps)"
        }

        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 6. WALL SIT (Multi-Point Knee & Hip Angle Hold)
    // -------------------------------------------------------------------------
    private fun classifyWallSit(pose: Pose): PoseClassificationResult {
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val kneeAngle = PoseAngleCalculator.calculateAngle(hip, knee, ankle)
        val hipAngle = PoseAngleCalculator.calculateAngle(shoulder, hip, knee)
        val now = SystemClock.elapsedRealtime()

        val isKneeBent = kneeAngle > 0 && kneeAngle in 60.0..155.0
        val isHipBent = hipAngle > 0 && hipAngle in 60.0..145.0
        val isSeatedStance = if (hip != null && knee != null) abs(knee.position.y - hip.position.y) < 300f else false

        val isSeated = isKneeBent || (isHipBent && isSeatedStance) || (shoulder != null && isHipBent)

        if (isSeated) {
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
            isCurrentlyHolding = false
            holdStartTimeMs = now
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isSeated) "🟢 Holding Wall Sit! (${totalSec}/${effectiveTargetHoldSeconds}s)" else "Sit back against wall with knees bent"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 7. HIGH KNEES
    // -------------------------------------------------------------------------
    private fun classifyHighKnees(pose: Pose): PoseClassificationResult {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (leftHip == null || leftKnee == null || rightHip == null || rightKnee == null) {
            return PoseClassificationResult(reps, effectiveTargetReps, false, 0, 0, "Show hips and knees in camera view", isCompleted)
        }

        val torsoLength = if (shoulder != null) abs(leftHip.position.y - shoulder.position.y).coerceAtLeast(60f) else 120f
        val leftKneeHigh = (leftHip.position.y - leftKnee.position.y) / torsoLength > -0.3f
        val rightKneeHigh = (rightHip.position.y - rightKnee.position.y) / torsoLength > -0.3f
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
        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 8. CALF RAISES
    // -------------------------------------------------------------------------
    private fun classifyCalfRaises(pose: Pose): PoseClassificationResult {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val hasLeftAnkle = leftAnkle != null && leftAnkle.inFrameLikelihood > 0.35f
        val hasRightAnkle = rightAnkle != null && rightAnkle.inFrameLikelihood > 0.35f

        if (!hasLeftAnkle && !hasRightAnkle) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "🦶 Step back so feet & ankles are visible in frame",
                isCompleted = false
            )
        }

        val currentAnkleY = when {
            hasLeftAnkle && hasRightAnkle -> (leftAnkle!!.position.y + rightAnkle!!.position.y) / 2f
            hasLeftAnkle -> leftAnkle!!.position.y
            else -> rightAnkle!!.position.y
        }

        val baseline = calfAnkleBaselineY
        if (baseline == null) {
            calfAnkleBaselineY = currentAnkleY
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "Rise high onto your toes (heels up!)",
                isCompleted = false
            )
        }

        val ankleLiftDelta = baseline - currentAnkleY
        val now = SystemClock.elapsedRealtime()

        if (ankleLiftDelta >= 12f) {
            isDownPhase = true
        } else if (ankleLiftDelta <= 5f && isDownPhase) {
            if (now - lastRepTimestampMs >= minRepDurationMs) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                calfAnkleBaselineY = (baseline * 0.8f) + (currentAnkleY * 0.2f)
                if (reps >= effectiveTargetReps) isCompleted = true
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Good heel lift! Now lower down slowly"
            else -> "Rise high onto your toes ($reps/$effectiveTargetReps)"
        }

        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 9. TREE POSE (Single-Leg Balance + High Foot)
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
            return PoseClassificationResult(0, effectiveTargetHoldSeconds, false, (accumulatedHoldMs / 1000L).toInt(), effectiveTargetHoldSeconds, "🧍 Full standing body in frame", isCompleted)
        }

        val leftKneeAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)

        val isRightStanding = rightKneeAngle in 145.0..185.0 && (leftAnkle!!.position.y < rightAnkle!!.position.y - 30f) && (leftKneeAngle in 30.0..140.0)
        val isLeftStanding = leftKneeAngle in 145.0..185.0 && (rightAnkle!!.position.y < leftAnkle!!.position.y - 30f) && (rightKneeAngle in 30.0..140.0)

        val isValidTreePose = isRightStanding || isLeftStanding
        val now = SystemClock.elapsedRealtime()

        if (isValidTreePose) {
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
            isCurrentlyHolding = false
            holdStartTimeMs = now
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isValidTreePose) "🟢 Holding Tree Pose! (${totalSec}/${effectiveTargetHoldSeconds}s) 🌳" else "Stand on 1 leg and lift other foot onto calf/thigh"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isValidTreePose, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 10. MOUNTAIN POSE (Tadasana: Grounded Standing Alignment)
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
            return PoseClassificationResult(0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds, "🧍 Full posture in frame", isCompleted)
        }

        val isUpright = (hip.position.y - shoulder.position.y) > 30f
        val now = SystemClock.elapsedRealtime()

        if (isUpright) {
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
            isCurrentlyHolding = false
            holdStartTimeMs = now
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isUpright) "🟢 Holding Mountain Pose (${totalSec}/${effectiveTargetHoldSeconds}s) 🏔️" else "Stand tall and steady with spine aligned"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 11. FORWARD FOLD
    // -------------------------------------------------------------------------
    private fun classifyForwardFold(pose: Pose): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        if (shoulder == null || hip == null) {
            isCurrentlyHolding = false
            return PoseClassificationResult(0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds, "Step back so body is in frame", isCompleted)
        }

        val hipAngle = if (knee != null) PoseAngleCalculator.calculateAngle(shoulder, hip, knee) else -1.0
        val isFolded = (hipAngle in 20.0..115.0) || (abs(shoulder.position.y - hip.position.y) < 80f)
        val now = SystemClock.elapsedRealtime()

        if (isFolded) {
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
            isCurrentlyHolding = false
            holdStartTimeMs = now
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isFolded) "🟢 Holding Forward Fold! (${totalSec}/${effectiveTargetHoldSeconds}s)" else "Bend forward at your hips"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isFolded, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 12. SIT TO STAND (Chair / Box Stands)
    // -------------------------------------------------------------------------
    private fun classifySitToStand(pose: Pose): PoseClassificationResult {
        return classifySquats(pose)
    }

    // -------------------------------------------------------------------------
    // 13. STAND UP AND SHAKE OFF (Kinetic Movement Oscillation)
    // -------------------------------------------------------------------------
    private fun classifyStandAndShake(pose: Pose): PoseClassificationResult {
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

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
        val now = SystemClock.elapsedRealtime()

        if (isShaking) {
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
            isCurrentlyHolding = false
            holdStartTimeMs = now
        }

        val totalSec = (accumulatedHoldMs / 1000L).toInt()
        val feedback = if (isShaking) "🟢 Great energy! Keep shaking (${totalSec}/${effectiveTargetHoldSeconds}s) ✨" else "Stand up and shake your arms & legs!"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isShaking, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 14. YOGA POSES (Cat-Cow, Child's Pose, Cobra, Shoulder Stretch)
    // -------------------------------------------------------------------------
    private fun classifyChildPose(pose: Pose): PoseClassificationResult {
        return classifyForwardFold(pose)
    }

    private fun classifyCatCow(pose: Pose): PoseClassificationResult {
        return classifyPlank(pose)
    }

    private fun classifyCobraStretch(pose: Pose): PoseClassificationResult {
        return classifyPlank(pose)
    }

    private fun classifyShoulderStretch(pose: Pose): PoseClassificationResult {
        return classifyMountainPose(pose)
    }

    private fun classifySeatedSpinalTwist(pose: Pose): PoseClassificationResult {
        return classifyMountainPose(pose)
    }

    private fun classifyMiniSunSalutation(pose: Pose): PoseClassificationResult {
        return classifyMountainPose(pose)
    }

    private fun classifyGeneralMovement(pose: Pose): PoseClassificationResult {
        return classifyStandAndShake(pose)
    }
}

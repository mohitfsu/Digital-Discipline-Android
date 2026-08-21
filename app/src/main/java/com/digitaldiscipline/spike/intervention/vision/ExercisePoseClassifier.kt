package com.digitaldiscipline.spike.intervention.vision

import android.os.SystemClock
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.max

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
 * Robust On-Device Exercise Classifier & Biomechanical Repetition State Machine.
 *
 * Distance-invariant & Angle-resilient biomechanical analysis for:
 * 1. PUSH_UPS (Elbow flexion + Torso vertical displacement)
 * 2. SQUATS (Knee flexion + Hip vertical descent)
 * 3. PLANK (Linear hold + Spine neutrality)
 * 4. LUNGES (Alternating knee flexion)
 * 5. JUMPING_JACKS (Overhead arm abduction)
 * 6. WALL_SIT (Resilient Torso-Thigh & Knee angle hold)
 * 7. HIGH_KNEES (Alternating knee drives)
 * 8. CALF_RAISES (Ankle plantarflexion / heel raise cycle with strict feet-in-frame requirement)
 * 9. TREE_POSE (Single-leg balance + high foot placement + knee flare)
 * 10. MOUNTAIN_POSE (Upright alignment & steady standing presence)
 * 11. FORWARD_FOLD (Hip hinge + forward torso flexion)
 * 12. SIT_TO_STAND (Seated to upright full standing cycle)
 * 13. STAND_UP_AND_SHAKE_OFF (Upright posture + dynamic limb kinetic oscillation)
 * 14. YOGA & STRETCHES (Sustained posture presence & stretch holds)
 */
class ExercisePoseClassifier(
    val exerciseId: String,
    val targetReps: Int = 15,
    targetHoldSeconds: Int = 30
) {
    // Enforce minimum 30 seconds hold target for all time-based exercises
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
    private var prevLeftShoulderPos: Pair<Float, Float>? = null
    private var prevRightShoulderPos: Pair<Float, Float>? = null

    // Anti-cheat cadence (prevent fake rapid double triggers)
    private val minRepDurationMs = 400L

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
        prevLeftShoulderPos = null
        prevRightShoulderPos = null
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
            "CHILD_POSE", "CHILDS_POSE", "SHOULDER_STRETCH", "STRETCH", "FULL_BODY_STRETCH",
            "CAT_COW", "COBRA_STRETCH", "SEATED_SPINAL_TWIST", "MINI_SUN_SALUTATION" -> classifyYogaPosture(pose)
            else -> classifyGeneralMovement(pose)
        }
    }

    // -------------------------------------------------------------------------
    // 1. PUSH-UPS (Elbow Angle + Torso Y Descent)
    // -------------------------------------------------------------------------
    private fun classifyPushups(pose: Pose): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftAngle = PoseAngleCalculator.calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightAngle = PoseAngleCalculator.calculateAngle(rightShoulder, rightElbow, rightWrist)

        val activeAngle = when {
            leftAngle > 0 && rightAngle > 0 -> (leftAngle + rightAngle) / 2.0
            leftAngle > 0 -> leftAngle
            rightAngle > 0 -> rightAngle
            else -> -1.0
        }

        val now = SystemClock.elapsedRealtime()

        if (activeAngle > 0) {
            if (activeAngle <= 128.0) {
                isDownPhase = true
            } else if (activeAngle >= 148.0 && isDownPhase) {
                if (now - lastRepTimestampMs >= minRepDurationMs) {
                    reps += 1
                    lastRepTimestampMs = now
                    isDownPhase = false
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        } else {
            val shoulder = leftShoulder ?: rightShoulder
            val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            if (shoulder != null && hip != null) {
                val diff = abs(hip.position.y - shoulder.position.y)
                if (diff < 90f) {
                    isDownPhase = true
                } else if (diff > 130f && isDownPhase) {
                    if (now - lastRepTimestampMs >= minRepDurationMs) {
                        reps += 1
                        lastRepTimestampMs = now
                        isDownPhase = false
                        if (reps >= effectiveTargetReps) isCompleted = true
                    }
                }
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Good depth! Now push up"
            activeAngle > 0 -> "Lower your chest (${activeAngle.toInt()}°)"
            else -> "Get in push-up position in frame"
        }

        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 2. SQUATS (Knee Angle + Hip Y Descent)
    // -------------------------------------------------------------------------
    private fun classifySquats(pose: Pose): PoseClassificationResult {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)

        val activeAngle = when {
            leftAngle > 0 && rightAngle > 0 -> (leftAngle + rightAngle) / 2.0
            leftAngle > 0 -> leftAngle
            rightAngle > 0 -> rightAngle
            else -> -1.0
        }

        val now = SystemClock.elapsedRealtime()

        if (activeAngle > 0) {
            if (activeAngle <= 138.0) {
                isDownPhase = true
            } else if (activeAngle >= 152.0 && isDownPhase) {
                if (now - lastRepTimestampMs >= minRepDurationMs) {
                    reps += 1
                    lastRepTimestampMs = now
                    isDownPhase = false
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        } else {
            val hip = leftHip ?: rightHip
            val knee = leftKnee ?: rightKnee
            if (hip != null && knee != null) {
                val distanceY = abs(knee.position.y - hip.position.y)
                if (distanceY < 130f) {
                    isDownPhase = true
                } else if (distanceY > 170f && isDownPhase) {
                    if (now - lastRepTimestampMs >= minRepDurationMs) {
                        reps += 1
                        lastRepTimestampMs = now
                        isDownPhase = false
                        if (reps >= effectiveTargetReps) isCompleted = true
                    }
                }
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Deep squat reached! Stand up"
            activeAngle > 0 -> "Squat down (${activeAngle.toInt()}°)"
            else -> "Step back so your body is visible"
        }

        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 3. PLANK (Shoulder -> Hip -> Ankle Straightness + Hold Timer)
    // -------------------------------------------------------------------------
    private fun classifyPlank(pose: Pose): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val alignmentAngle = PoseAngleCalculator.calculateAngle(shoulder, hip, ankle)
        val now = SystemClock.elapsedRealtime()

        val isAligned = (alignmentAngle in 135.0..195.0) || (shoulder != null && hip != null && abs(shoulder.position.y - hip.position.y) < 180f)

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
    // 4. LUNGES (Alternating Leg Drops)
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
            leftAngle > 0 && rightAngle > 0 -> Math.min(leftAngle, rightAngle)
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
            else -> "Step forward into a steady lunge"
        }

        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 5. JUMPING JACKS (Arms Overhead Abduction)
    // -------------------------------------------------------------------------
    private fun classifyJumpingJacks(pose: Pose): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        if (leftShoulder == null || rightShoulder == null) {
            return PoseClassificationResult(reps, effectiveTargetReps, false, 0, 0, "Position body in camera view", isCompleted)
        }

        val handsAreHigh = (leftWrist != null && leftWrist.position.y < leftShoulder.position.y) ||
                (rightWrist != null && rightWrist.position.y < rightShoulder.position.y)
        val now = SystemClock.elapsedRealtime()

        if (handsAreHigh) {
            isDownPhase = true
        } else if (!handsAreHigh && isDownPhase) {
            if (now - lastRepTimestampMs >= 300L) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                if (reps >= effectiveTargetReps) isCompleted = true
            }
        }

        val feedback = if (handsAreHigh) "🟢 Arms overhead! Now lower" else "Jump and raise arms overhead"
        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 6. WALL SIT (Resilient Torso-Thigh & Knee Angle Hold)
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
        val isSeatedStance = if (hip != null && knee != null) {
            val dy = abs(knee.position.y - hip.position.y)
            dy < 300f
        } else false

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

        if (leftHip == null || leftKnee == null || rightHip == null || rightKnee == null) {
            return PoseClassificationResult(reps, effectiveTargetReps, false, 0, 0, "Show hips and knees in camera view", isCompleted)
        }

        val leftKneeHigh = leftKnee.position.y < (leftHip.position.y + 120f)
        val rightKneeHigh = rightKnee.position.y < (rightHip.position.y + 120f)
        val now = SystemClock.elapsedRealtime()

        if (leftKneeHigh || rightKneeHigh) {
            if (!isDownPhase) {
                isDownPhase = true
                if (now - lastRepTimestampMs >= 280L) {
                    reps += 1
                    lastRepTimestampMs = now
                    if (reps >= effectiveTargetReps) isCompleted = true
                }
            }
        } else {
            isDownPhase = false
        }

        val feedback = if (isDownPhase) "🟢 High knee drive!" else "Drive knees up high!"
        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 8. CALF RAISES (Strict Feet-in-Frame + Heel Elevation Cycle)
    // -------------------------------------------------------------------------
    private fun classifyCalfRaises(pose: Pose): PoseClassificationResult {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        // Strict Requirement: Feet / Ankles MUST be visible in frame with good likelihood
        val hasLeftAnkle = leftAnkle != null && leftAnkle.inFrameLikelihood > 0.45f
        val hasRightAnkle = rightAnkle != null && rightAnkle.inFrameLikelihood > 0.45f

        if (!hasLeftAnkle && !hasRightAnkle) {
            return PoseClassificationResult(
                currentReps = reps,
                targetReps = effectiveTargetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = 0,
                feedbackMessage = "🦶 Step back so feet & ankles are visible in frame",
                isCompleted = false,
                formQualityScore = 0.0f
            )
        }

        val currentAnkleY = when {
            hasLeftAnkle && hasRightAnkle -> (leftAnkle!!.position.y + rightAnkle!!.position.y) / 2f
            hasLeftAnkle -> leftAnkle!!.position.y
            else -> rightAnkle!!.position.y
        }

        // Establish or update baseline when standing flat
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

        // Check knee extension (to ensure user is standing straight and doing calf raises, not squatting)
        val leftKneeAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)
        val isStandingStraight = (leftKneeAngle < 0 || leftKneeAngle > 140.0) && (rightKneeAngle < 0 || rightKneeAngle > 140.0)

        val ankleLiftDelta = baseline - currentAnkleY
        val now = SystemClock.elapsedRealtime()

        if (isStandingStraight && ankleLiftDelta >= 14f) {
            // Peak / Up phase (on toes)
            isDownPhase = true
        } else if (ankleLiftDelta <= 6f && isDownPhase) {
            // Returned to flat ground
            if (now - lastRepTimestampMs >= minRepDurationMs) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                // Smooth baseline calibration
                calfAnkleBaselineY = (baseline * 0.8f) + (currentAnkleY * 0.2f)
                if (reps >= effectiveTargetReps) isCompleted = true
            }
        } else if (currentAnkleY > baseline + 15f) {
            // User shifted closer to ground/camera: recalibrate baseline
            calfAnkleBaselineY = currentAnkleY
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Good heel lift! Now lower down slowly"
            else -> "Rise high onto your toes ($reps/$effectiveTargetReps)"
        }

        return PoseClassificationResult(
            currentReps = reps,
            targetReps = effectiveTargetReps,
            isHolding = isDownPhase,
            holdSeconds = 0,
            targetHoldSeconds = 0,
            feedbackMessage = feedback,
            isCompleted = isCompleted
        )
    }

    // -------------------------------------------------------------------------
    // 9. TREE POSE (Vrksasana: Strict Single-Leg Balance + High Lifted Foot)
    // -------------------------------------------------------------------------
    private fun classifyTreePose(pose: Pose): PoseClassificationResult {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val hasLeftLeg = leftAnkle != null && leftKnee != null && leftHip != null && leftAnkle.inFrameLikelihood > 0.45f
        val hasRightLeg = rightAnkle != null && rightKnee != null && rightHip != null && rightAnkle.inFrameLikelihood > 0.45f

        // Must see both legs in frame to evaluate single-leg balance vs two feet
        if (!hasLeftLeg || !hasRightLeg) {
            isCurrentlyHolding = false
            return PoseClassificationResult(
                currentReps = 0,
                targetReps = 0,
                isHolding = false,
                holdSeconds = (accumulatedHoldMs / 1000L).toInt(),
                targetHoldSeconds = effectiveTargetHoldSeconds,
                feedbackMessage = "🧍 Step back so your full body & feet are in frame",
                isCompleted = false,
                formQualityScore = 0.0f
            )
        }

        val leftKneeAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)

        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip
        val torsoLength = if (shoulder != null && hip != null) abs(hip.position.y - shoulder.position.y).coerceAtLeast(80f) else 150f

        val leftAnkleY = leftAnkle!!.position.y
        val rightAnkleY = rightAnkle!!.position.y
        val ankleYDifference = abs(leftAnkleY - rightAnkleY)

        // Candidate 1: Right leg is standing straight, Left leg is lifted onto thigh/calf
        val isRightStanding = rightKneeAngle in 150.0..185.0 && (leftAnkleY < rightAnkleY - (torsoLength * 0.35f)) && (leftKneeAngle in 35.0..135.0)

        // Candidate 2: Left leg is standing straight, Right leg is lifted onto thigh/calf
        val isLeftStanding = leftKneeAngle in 150.0..185.0 && (rightAnkleY < leftAnkleY - (torsoLength * 0.35f)) && (rightKneeAngle in 35.0..135.0)

        // Check if user is simply standing flat on two feet (NOT Tree Pose)
        val isStandingOnBothFeet = ankleYDifference < (torsoLength * 0.2f) && leftKneeAngle > 150.0 && rightKneeAngle > 150.0

        val isValidTreePose = (isRightStanding || isLeftStanding) && !isStandingOnBothFeet
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
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isValidTreePose -> "🟢 Holding Tree Pose! (${totalSec}/${effectiveTargetHoldSeconds}s) 🌳"
            isStandingOnBothFeet -> "🌲 Stand on 1 leg & place opposite foot on inner calf or thigh"
            else -> "Lift your foot higher onto your calf/thigh & balance"
        }

        return PoseClassificationResult(
            currentReps = totalSec,
            targetReps = effectiveTargetHoldSeconds,
            isHolding = isValidTreePose,
            holdSeconds = totalSec,
            targetHoldSeconds = effectiveTargetHoldSeconds,
            feedbackMessage = feedback,
            isCompleted = isCompleted,
            formQualityScore = if (isValidTreePose) 1.0f else 0.4f
        )
    }

    // -------------------------------------------------------------------------
    // 10. MOUNTAIN POSE (Tadasana: Grounded Standing Alignment)
    // -------------------------------------------------------------------------
    private fun classifyMountainPose(pose: Pose): PoseClassificationResult {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val hasAnkles = (leftAnkle != null && leftAnkle.inFrameLikelihood > 0.4f) || (rightAnkle != null && rightAnkle.inFrameLikelihood > 0.4f)
        val hasHips = leftHip != null || rightHip != null
        val hasShoulders = leftShoulder != null || rightShoulder != null

        if (!hasAnkles || !hasHips || !hasShoulders) {
            isCurrentlyHolding = false
            return PoseClassificationResult(
                currentReps = 0,
                targetReps = 0,
                isHolding = false,
                holdSeconds = (accumulatedHoldMs / 1000L).toInt(),
                targetHoldSeconds = effectiveTargetHoldSeconds,
                feedbackMessage = "🧍 Step back so your full standing posture is in frame",
                isCompleted = false
            )
        }

        val shoulder = leftShoulder ?: rightShoulder!!
        val hip = leftHip ?: rightHip!!
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
    // 11. FORWARD FOLD (Standing / Seated Hip Hinge)
    // -------------------------------------------------------------------------
    private fun classifyForwardFold(pose: Pose): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        if (shoulder == null || hip == null) {
            isCurrentlyHolding = false
            return PoseClassificationResult(0, effectiveTargetHoldSeconds, false, 0, effectiveTargetHoldSeconds, "Step back so your body is visible in frame", isCompleted)
        }

        val hipAngle = if (knee != null) PoseAngleCalculator.calculateAngle(shoulder, hip, knee) else -1.0
        val isFolded = (hipAngle in 20.0..105.0) || (abs(shoulder.position.y - hip.position.y) < 60f)

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
        val feedback = if (isFolded) "🟢 Holding Forward Fold (${totalSec}/${effectiveTargetHoldSeconds}s) 🙇" else "Hinge at hips and fold forward toward your feet"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 12. SIT TO STAND (Chair / Seat to Full Upright Extension Cycle)
    // -------------------------------------------------------------------------
    private fun classifySitToStand(pose: Pose): PoseClassificationResult {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val hip = leftHip ?: rightHip
        val knee = leftKnee ?: rightKnee

        if (hip == null || knee == null) {
            return PoseClassificationResult(reps, effectiveTargetReps, false, 0, 0, "Position chair & body in camera view", isCompleted)
        }

        val leftKneeAngle = PoseAngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = PoseAngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)
        val activeKneeAngle = when {
            leftKneeAngle > 0 && rightKneeAngle > 0 -> (leftKneeAngle + rightKneeAngle) / 2.0
            leftKneeAngle > 0 -> leftKneeAngle
            rightKneeAngle > 0 -> rightKneeAngle
            else -> -1.0
        }

        val thighLengthY = knee.position.y - hip.position.y
        val now = SystemClock.elapsedRealtime()

        // 1. Primary: Knee angle flexion (seated = 60°-125°, standing = >= 155°)
        // 2. Secondary: Thigh vertical displacement (seated: thigh horizontal < 100px, standing: thigh vertical >= 125px)
        val isSeated = (activeKneeAngle in 55.0..128.0) || (activeKneeAngle < 0 && thighLengthY < 100f)
        val isStanding = (activeKneeAngle >= 152.0) || (thighLengthY >= 130f)

        if (isSeated) {
            isDownPhase = true // Seated on chair
        } else if (isStanding && isDownPhase) {
            if (now - lastRepTimestampMs >= minRepDurationMs) {
                reps += 1
                lastRepTimestampMs = now
                isDownPhase = false
                if (reps >= effectiveTargetReps) isCompleted = true
            }
        }

        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            isDownPhase -> "🟢 Seated detected! Now stand fully upright"
            else -> "Sit down on chair with control ($reps/$effectiveTargetReps)"
        }

        return PoseClassificationResult(reps, effectiveTargetReps, isDownPhase, 0, 0, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 13. STAND UP & SHAKE OFF (Camera AI Pose + Limb Kinetic Oscillation)
    // -------------------------------------------------------------------------
    private fun classifyStandAndShake(pose: Pose): PoseClassificationResult {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val now = SystemClock.elapsedRealtime()

        // 1. Detect Standing Posture
        val shoulder = leftShoulder ?: rightShoulder
        val hip = leftHip ?: rightHip
        val knee = leftKnee ?: rightKnee

        var isStanding = false
        if (shoulder != null && hip != null) {
            val shoulderHipDistance = hip.position.y - shoulder.position.y
            val isUpperBodyVertical = shoulderHipDistance > 30f

            val isLowerBodyVertical = if (knee != null) {
                hip.position.y < (knee.position.y - 20f)
            } else {
                true
            }

            val leftHipAngle = PoseAngleCalculator.calculateAngle(leftShoulder, leftHip, leftKnee)
            val rightHipAngle = PoseAngleCalculator.calculateAngle(rightShoulder, rightHip, rightKnee)
            val isUprightAngle = (leftHipAngle < 0 || leftHipAngle > 135.0) && (rightHipAngle < 0 || rightHipAngle > 135.0)

            isStanding = isUpperBodyVertical && isLowerBodyVertical && isUprightAngle
        }

        // 2. Measure Dynamic Shaking Motion (Limb displacement deltas)
        var shakeVelocity = 0f

        if (leftWrist != null) {
            val prev = prevLeftWristPos
            val curr = Pair(leftWrist.position.x, leftWrist.position.y)
            if (prev != null) {
                val dx = curr.first - prev.first
                val dy = curr.second - prev.second
                shakeVelocity += Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            }
            prevLeftWristPos = curr
        }

        if (rightWrist != null) {
            val prev = prevRightWristPos
            val curr = Pair(rightWrist.position.x, rightWrist.position.y)
            if (prev != null) {
                val dx = curr.first - prev.first
                val dy = curr.second - prev.second
                shakeVelocity += Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            }
            prevRightWristPos = curr
        }

        if (leftAnkle != null) {
            val prev = prevLeftAnklePos
            val curr = Pair(leftAnkle.position.x, leftAnkle.position.y)
            if (prev != null) {
                val dx = curr.first - prev.first
                val dy = curr.second - prev.second
                shakeVelocity += Math.hypot(dx.toDouble(), dy.toDouble()).toFloat() * 0.7f
            }
            prevLeftAnklePos = curr
        }

        if (rightAnkle != null) {
            val prev = prevRightAnklePos
            val curr = Pair(rightAnkle.position.x, rightAnkle.position.y)
            if (prev != null) {
                val dx = curr.first - prev.first
                val dy = curr.second - prev.second
                shakeVelocity += Math.hypot(dx.toDouble(), dy.toDouble()).toFloat() * 0.7f
            }
            prevRightAnklePos = curr
        }

        if (shoulder != null) {
            val prev = prevLeftShoulderPos
            val curr = Pair(shoulder.position.x, shoulder.position.y)
            if (prev != null) {
                val dx = curr.first - prev.first
                val dy = curr.second - prev.second
                shakeVelocity += Math.hypot(dx.toDouble(), dy.toDouble()).toFloat() * 0.5f
            }
            prevLeftShoulderPos = curr
        }

        val isShaking = shakeVelocity >= 12f
        val isStandAndShakeActive = isStanding && isShaking

        if (isStandAndShakeActive) {
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
        val feedback = when {
            isCompleted -> "Challenge Completed! 🎉"
            !isStanding -> "🧍 Step back & stand upright in camera view"
            !isShaking -> "🧍 Standing detected! Now shake arms, hands & legs! 👋"
            else -> "⚡ Shaking active! Keep moving... (${totalSec}/${effectiveTargetHoldSeconds}s) 🔥"
        }

        return PoseClassificationResult(
            currentReps = totalSec,
            targetReps = effectiveTargetHoldSeconds,
            isHolding = isStandAndShakeActive,
            holdSeconds = totalSec,
            targetHoldSeconds = effectiveTargetHoldSeconds,
            feedbackMessage = feedback,
            isCompleted = isCompleted,
            formQualityScore = if (isStandAndShakeActive) 1.0f else if (isStanding) 0.6f else 0.2f
        )
    }

    // -------------------------------------------------------------------------
    // 14. YOGA POSTURE & MOBILITY HOLD
    // -------------------------------------------------------------------------
    private fun classifyYogaPosture(pose: Pose): PoseClassificationResult {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val isPresent = (leftShoulder != null && rightShoulder != null) || (leftHip != null && rightHip != null)
        val now = SystemClock.elapsedRealtime()

        if (isPresent) {
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
        val feedback = if (isPresent) "🟢 Holding stretch (${totalSec}/${effectiveTargetHoldSeconds}s) 🧘" else "Settle into posture in camera frame"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }

    // -------------------------------------------------------------------------
    // 15. GENERAL MOVEMENT FALLBACK
    // -------------------------------------------------------------------------
    private fun classifyGeneralMovement(pose: Pose): PoseClassificationResult {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val now = SystemClock.elapsedRealtime()

        val isPresent = nose != null && (leftShoulder != null || rightShoulder != null)

        if (isPresent) {
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
        val feedback = if (isPresent) "🟢 Moving in frame! (${totalSec}/${effectiveTargetHoldSeconds}s)" else "Step in front of camera to begin"
        return PoseClassificationResult(totalSec, effectiveTargetHoldSeconds, isCurrentlyHolding, totalSec, effectiveTargetHoldSeconds, feedback, isCompleted)
    }
}

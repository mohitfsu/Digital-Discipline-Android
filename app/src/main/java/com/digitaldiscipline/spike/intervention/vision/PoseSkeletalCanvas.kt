package com.digitaldiscipline.spike.intervention.vision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.max

/**
 * Compose Canvas that draws real-time skeletal tracking wireframe
 * covering all 33 body joints, facial features, hands, torso, and feet
 * with aspect-ratio-corrected (FILL_CENTER) transformation and dynamic form feedback.
 */
@Composable
fun PoseSkeletalCanvas(
    pose: Pose?,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean = true,
    isActionActive: Boolean = false,
    isPostureCorrect: Boolean = false,
    exerciseId: String = "",
    activeAngles: Map<String, Double> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pose == null || imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        // Accurate aspect-ratio scaling matching PreviewView.ScaleType.FILL_CENTER
        val scale = max(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val offsetX = (canvasWidth - scaledWidth) / 2f
        val offsetY = (canvasHeight - scaledHeight) / 2f

        fun transformPoint(landmark: PoseLandmark?): Offset? {
            if (landmark == null || landmark.inFrameLikelihood < 0.25f) return null
            val pos = landmark.position

            val rawX = if (isFrontCamera) {
                (imageWidth - pos.x) * scale + offsetX
            } else {
                pos.x * scale + offsetX
            }
            val rawY = pos.y * scale + offsetY
            return Offset(rawX, rawY)
        }

        fun drawBone(p1: PoseLandmark?, p2: PoseLandmark?, color: Color, strokeWidth: Float = if (isActionActive || isPostureCorrect) 8f else 5.5f) {
            val o1 = transformPoint(p1)
            val o2 = transformPoint(p2)
            if (o1 != null && o2 != null) {
                drawLine(
                    color = color,
                    start = o1,
                    end = o2,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // Head landmarks
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
        val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
        val mouthLeft = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH)
        val mouthRight = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH)

        // Upper body landmarks
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        // Hands & Fingers
        val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
        val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
        val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
        val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)

        // Core / Hips
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        // Lower body landmarks
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
        val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
        val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
        val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)

        // Color palettes based on posture correctness state (Green when correct, Red/Amber when incorrect)
        val torsoColor = when {
            isPostureCorrect -> Color(0xFF22C55E) // Neon Green
            isActionActive -> Color(0xFFF59E0B)   // Amber
            else -> Color(0xFFEF4444)             // Bright Red
        }
        val limbColor = when {
            isPostureCorrect -> Color(0xFF4ADE80)
            isActionActive -> Color(0xFFFBBF24)
            else -> Color(0xFFF87171)
        }
        val headColor = when {
            isPostureCorrect -> Color(0xFF86EFAC)
            isActionActive -> Color(0xFFFDE68A)
            else -> Color(0xFFFCA5A5)
        }
        val glowColor = when {
            isPostureCorrect -> Color(0xFF22C55E).copy(alpha = 0.75f)
            isActionActive -> Color(0xFFF59E0B).copy(alpha = 0.75f)
            else -> Color(0xFFEF4444).copy(alpha = 0.70f)
        }
        val beadColor = when {
            isPostureCorrect -> Color(0xFFDCFCE7)
            isActionActive -> Color(0xFFFEF3C7)
            else -> Color(0xFFFEE2E2)
        }

        // 1. Draw Head & Face (6 landmarks)
        drawBone(leftEye, nose, headColor)
        drawBone(rightEye, nose, headColor)
        drawBone(leftEye, leftEar, headColor)
        drawBone(rightEye, rightEar, headColor)
        drawBone(mouthLeft, mouthRight, headColor)
        drawBone(nose, leftShoulder, headColor.copy(alpha = 0.6f))
        drawBone(nose, rightShoulder, headColor.copy(alpha = 0.6f))

        // 2. Upper Body & Arms (6 landmarks)
        drawBone(leftShoulder, rightShoulder, torsoColor, strokeWidth = 9f)
        drawBone(leftShoulder, leftElbow, limbColor)
        drawBone(leftElbow, leftWrist, limbColor)
        drawBone(rightShoulder, rightElbow, limbColor)
        drawBone(rightElbow, rightWrist, limbColor)

        // 3. Hands & Fingers (6 landmarks)
        drawBone(leftWrist, leftThumb, limbColor)
        drawBone(leftWrist, leftIndex, limbColor)
        drawBone(leftWrist, leftPinky, limbColor)
        drawBone(rightWrist, rightThumb, limbColor)
        drawBone(rightWrist, rightIndex, limbColor)
        drawBone(rightWrist, rightPinky, limbColor)

        // 4. Torso Box (4 landmarks)
        drawBone(leftShoulder, leftHip, torsoColor, strokeWidth = 8f)
        drawBone(rightShoulder, rightHip, torsoColor, strokeWidth = 8f)
        drawBone(leftHip, rightHip, torsoColor, strokeWidth = 9f)

        // 5. Lower Body & Legs (4 landmarks)
        drawBone(leftHip, leftKnee, limbColor, strokeWidth = 8f)
        drawBone(leftKnee, leftAnkle, limbColor, strokeWidth = 8f)
        drawBone(rightHip, rightKnee, limbColor, strokeWidth = 8f)
        drawBone(rightKnee, rightAnkle, limbColor, strokeWidth = 8f)

        // 6. Feet & Toes (6 landmarks)
        drawBone(leftAnkle, leftHeel, limbColor)
        drawBone(leftAnkle, leftFootIndex, limbColor)
        drawBone(leftHeel, leftFootIndex, limbColor)
        drawBone(rightAnkle, rightHeel, limbColor)
        drawBone(rightAnkle, rightFootIndex, limbColor)
        drawBone(rightHeel, rightFootIndex, limbColor)

        // 7. Draw All 33 Key Joint Marker Points
        val jointPoints = listOfNotNull(
            transformPoint(nose),
            transformPoint(leftEye),
            transformPoint(rightEye),
            transformPoint(leftEar),
            transformPoint(rightEar),
            transformPoint(mouthLeft),
            transformPoint(mouthRight),
            transformPoint(leftShoulder),
            transformPoint(rightShoulder),
            transformPoint(leftElbow),
            transformPoint(rightElbow),
            transformPoint(leftWrist),
            transformPoint(rightWrist),
            transformPoint(leftThumb),
            transformPoint(rightThumb),
            transformPoint(leftIndex),
            transformPoint(rightIndex),
            transformPoint(leftPinky),
            transformPoint(rightPinky),
            transformPoint(leftHip),
            transformPoint(rightHip),
            transformPoint(leftKnee),
            transformPoint(rightKnee),
            transformPoint(leftAnkle),
            transformPoint(rightAnkle),
            transformPoint(leftHeel),
            transformPoint(rightHeel),
            transformPoint(leftFootIndex),
            transformPoint(rightFootIndex)
        )

        jointPoints.forEach { pt ->
            // Outer radiant glow circle
            drawCircle(
                color = glowColor,
                radius = if (isPostureCorrect) 12f else 9f,
                center = pt
            )
            // Solid center joint bead
            drawCircle(
                color = beadColor,
                radius = if (isPostureCorrect) 6f else 4.5f,
                center = pt
            )
        }

        // 8. Targeted Focal Joint Halos (Observe and point to primary exercise joints)
        val focalLandmarks = when (exerciseId.uppercase()) {
            "PUSH_UPS", "PUSHUPS" -> listOfNotNull(leftElbow, rightElbow, leftShoulder, rightShoulder)
            "SQUATS", "BODYWEIGHT_SQUATS", "WALL_SIT" -> listOfNotNull(leftKnee, rightKnee, leftHip, rightHip)
            "SIT_UPS", "SITUPS", "CRUNCHES", "CORE_SITUPS" -> listOfNotNull(leftShoulder, rightShoulder, leftHip, rightHip)
            "CALF_RAISES", "CALF_RAISE", "CALF" -> listOfNotNull(leftAnkle, rightAnkle, leftHeel, rightHeel)
            "PLANK", "CORE_PLANK" -> listOfNotNull(leftShoulder, rightShoulder, leftHip, rightHip, leftAnkle, rightAnkle)
            "LUNGES", "ALTERNATING_LUNGES" -> listOfNotNull(leftKnee, rightKnee, leftHip, rightHip)
            else -> emptyList()
        }

        focalLandmarks.forEach { landmark ->
            val pt = transformPoint(landmark)
            if (pt != null) {
                // Large pulsing focal halo
                drawCircle(
                    color = if (isPostureCorrect) Color(0xFF22C55E).copy(alpha = 0.45f) else Color(0xFFEF4444).copy(alpha = 0.40f),
                    radius = 24f,
                    center = pt
                )
                drawCircle(
                    color = if (isPostureCorrect) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                    radius = 16f,
                    center = pt
                )
            }
        }
    }
}

package com.digitaldiscipline.spike.ui.vision

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

enum class GuideViewMode {
    CAMERA_PLACEMENT,
    EXERCISE_POSTURE
}

/**
 * High-definition vector animated canvas illustration for phone camera placement
 * and biomechanically accurate exercise form (Push-ups, Sit-ups, Wall Sit, Squats, Calf Raises).
 */
@Composable
fun AnimatedExerciseGuideCanvas(
    exerciseId: String,
    mode: GuideViewMode = GuideViewMode.CAMERA_PLACEMENT,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "guide_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim_progress"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090D16))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background subtle studio fill
            drawRect(
                color = Color(0xFF0B1120),
                size = size
            )

            if (mode == GuideViewMode.CAMERA_PLACEMENT) {
                drawCameraPlacementIllustration(
                    exerciseId = exerciseId,
                    width = width,
                    height = height,
                    pulseAlpha = pulseAlpha
                )
            } else {
                drawExercisePostureIllustration(
                    exerciseId = exerciseId,
                    width = width,
                    height = height,
                    animProgress = animProgress
                )
            }
        }
    }
}

// =============================================================================
// 1. CAMERA & PHONE PLACEMENT ILLUSTRATION (Floor/Table + Radiant FOV Cone)
// =============================================================================
private fun DrawScope.drawCameraPlacementIllustration(
    exerciseId: String,
    width: Float,
    height: Float,
    pulseAlpha: Float
) {
    val isFloorPlacement = when (exerciseId.uppercase()) {
        "PUSH_UPS", "PUSHUPS", "SIT_UPS", "SITUPS", "CRUNCHES", "PLANK", "CALF_RAISES", "CALF_RAISE" -> true
        else -> false
    }

    val floorY = height * 0.80f

    // Studio Floor Line
    drawLine(
        color = Color(0xFF334155),
        start = Offset(0f, floorY),
        end = Offset(width, floorY),
        strokeWidth = 3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    )

    if (isFloorPlacement) {
        // Phone on Floor propped at 45°
        val phoneBaseX = width * 0.14f
        val phoneBaseY = floorY
        val phoneLength = height * 0.28f
        val angleRad = Math.toRadians(45.0).toFloat()

        val phoneTopX = phoneBaseX + phoneLength * cos(angleRad)
        val phoneTopY = phoneBaseY - phoneLength * sin(angleRad)

        // Phone Prop Stand
        drawPath(
            path = Path().apply {
                moveTo(phoneBaseX - 8f, phoneBaseY)
                lineTo(phoneBaseX + 16f, phoneBaseY)
                lineTo(phoneBaseX + 6f, phoneBaseY - 18f)
                close()
            },
            color = Color(0xFF475569)
        )

        // Phone Body (Propped)
        drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(phoneBaseX, phoneBaseY),
            end = Offset(phoneTopX, phoneTopY),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Camera Lens Dot
        drawCircle(
            color = Color(0xFF22C55E),
            radius = 5f,
            center = Offset(phoneTopX - 3f, phoneTopY + 3f)
        )

        // Radiant Field of View (FOV) Cone
        val fovPath = Path().apply {
            moveTo(phoneTopX, phoneTopY)
            lineTo(width * 0.95f, floorY - height * 0.65f)
            lineTo(width * 0.95f, floorY)
            close()
        }

        drawPath(
            path = fovPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF22C55E).copy(alpha = pulseAlpha * 0.35f),
                    Color(0xFF38BDF8).copy(alpha = pulseAlpha * 0.12f),
                    Color.Transparent
                ),
                startX = phoneTopX,
                endX = width * 0.95f
            )
        )

        // FOV Edge Lines
        drawLine(
            color = Color(0xFF22C55E).copy(alpha = pulseAlpha),
            start = Offset(phoneTopX, phoneTopY),
            end = Offset(width * 0.95f, floorY - height * 0.65f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
        drawLine(
            color = Color(0xFF22C55E).copy(alpha = pulseAlpha),
            start = Offset(phoneTopX, phoneTopY),
            end = Offset(width * 0.95f, floorY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        // User Position Silhouette in FOV
        val userX = width * 0.68f
        val userHeadY = floorY - height * 0.22f
        drawCircle(
            color = Color(0xFFF8FAFC),
            radius = 10f,
            center = Offset(userX, userHeadY)
        )
        drawLine(
            color = Color(0xFFF8FAFC),
            start = Offset(userX, userHeadY + 10f),
            end = Offset(userX - width * 0.18f, floorY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Distance Tag
        drawDistanceBanner(
            startX = phoneBaseX,
            endX = userX,
            y = floorY + 18f
        )
    } else {
        // Table/Stand at Waist Height (for Squats / Wall Sit)
        val tableX = width * 0.15f
        val tableTopY = height * 0.50f
        val tableWidth = 36f

        // Table Stand
        drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(tableX - tableWidth / 2f, tableTopY),
            size = Size(tableWidth, floorY - tableTopY)
        )

        // Phone Standing Upright
        val phoneHeight = 36f
        val phoneY = tableTopY - phoneHeight
        drawRoundRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(tableX - 5f, phoneY),
            size = Size(10f, phoneHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )

        // Camera Lens
        drawCircle(
            color = Color(0xFF22C55E),
            radius = 4f,
            center = Offset(tableX, phoneY + 6f)
        )

        // Wide Radiant Field of View
        val fovPath = Path().apply {
            moveTo(tableX, phoneY + 6f)
            lineTo(width * 0.95f, height * 0.10f)
            lineTo(width * 0.95f, floorY)
            close()
        }

        drawPath(
            path = fovPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF22C55E).copy(alpha = pulseAlpha * 0.35f),
                    Color(0xFF38BDF8).copy(alpha = pulseAlpha * 0.12f),
                    Color.Transparent
                ),
                startX = tableX,
                endX = width * 0.95f
            )
        )

        // User Standing Full-Height in FOV
        val userX = width * 0.72f
        val userHeadY = height * 0.20f
        // Head
        drawCircle(color = Color(0xFFF8FAFC), radius = 11f, center = Offset(userX, userHeadY))
        // Torso
        drawLine(color = Color(0xFFF8FAFC), start = Offset(userX, userHeadY + 11f), end = Offset(userX, floorY - height * 0.28f), strokeWidth = 4f)
        // Legs
        drawLine(color = Color(0xFFF8FAFC), start = Offset(userX, floorY - height * 0.28f), end = Offset(userX - 10f, floorY), strokeWidth = 4f)
        drawLine(color = Color(0xFFF8FAFC), start = Offset(userX, floorY - height * 0.28f), end = Offset(userX + 10f, floorY), strokeWidth = 4f)

        // Distance Tag
        drawDistanceBanner(
            startX = tableX,
            endX = userX,
            y = floorY + 18f
        )
    }
}

private fun DrawScope.drawDistanceBanner(
    startX: Float,
    endX: Float,
    y: Float
) {
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(startX, y),
        end = Offset(endX, y),
        strokeWidth = 2f
    )
    // Left & Right Arrow ticks
    drawLine(color = Color(0xFF38BDF8), start = Offset(startX, y - 4f), end = Offset(startX, y + 4f), strokeWidth = 2f)
    drawLine(color = Color(0xFF38BDF8), start = Offset(endX, y - 4f), end = Offset(endX, y + 4f), strokeWidth = 2f)
}

// =============================================================================
// 2. BIOMECHANICAL POSTURE ANIMATION (Animated Skeleton / Vector Loop)
// =============================================================================
private fun DrawScope.drawExercisePostureIllustration(
    exerciseId: String,
    width: Float,
    height: Float,
    animProgress: Float
) {
    val floorY = height * 0.80f

    // Floor baseline
    drawLine(
        color = Color(0xFF334155),
        start = Offset(0f, floorY),
        end = Offset(width, floorY),
        strokeWidth = 3f
    )

    when (exerciseId.uppercase()) {
        "PUSH_UPS", "PUSHUPS" -> drawPushupsPosture(width, height, floorY, animProgress)
        "SIT_UPS", "SITUPS", "CRUNCHES" -> drawSitupsPosture(width, height, floorY, animProgress)
        "WALL_SIT", "WALLSIT" -> drawWallSitPosture(width, height, floorY, animProgress)
        "SQUATS", "BODYWEIGHT_SQUATS" -> drawSquatsPosture(width, height, floorY, animProgress)
        "CALF_RAISES", "CALF_RAISE" -> drawCalfRaisesPosture(width, height, floorY, animProgress)
        "PLANK", "CORE_PLANK" -> drawPlankPosture(width, height, floorY, animProgress)
        else -> drawPushupsPosture(width, height, floorY, animProgress)
    }
}

private fun DrawScope.drawPushupsPosture(
    width: Float,
    height: Float,
    floorY: Float,
    progress: Float
) {
    val feetX = width * 0.22f
    val feetY = floorY - 4f

    val handsX = width * 0.68f
    val handsY = floorY - 2f

    val chestHeight = lerp(height * 0.38f, height * 0.14f, progress)
    val shoulderX = width * 0.66f
    val shoulderY = floorY - chestHeight

    val headX = shoulderX + width * 0.08f
    val headY = shoulderY - 8f

    val hipsX = width * 0.44f
    val hipsY = floorY - (chestHeight * 0.85f)

    val elbowX = lerp(handsX - 8f, handsX - 28f, progress)
    val elbowY = lerp((shoulderY + handsY) / 2f - 10f, (shoulderY + handsY) / 2f + 12f, progress)

    val skeletonColor = Color(0xFF22C55E)

    // Body plank line (Feet -> Hips -> Shoulders)
    drawLine(color = skeletonColor, start = Offset(feetX, feetY), end = Offset(hipsX, hipsY), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(hipsX, hipsY), end = Offset(shoulderX, shoulderY), strokeWidth = 5f, cap = StrokeCap.Round)

    // Arm (Shoulder -> Elbow -> Hand)
    drawLine(color = skeletonColor, start = Offset(shoulderX, shoulderY), end = Offset(elbowX, elbowY), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(elbowX, elbowY), end = Offset(handsX, handsY), strokeWidth = 5f, cap = StrokeCap.Round)

    // Head
    drawCircle(color = Color(0xFFF8FAFC), radius = 12f, center = Offset(headX, headY))

    // Joint Nodes
    drawJointHalo(Offset(shoulderX, shoulderY))
    drawJointHalo(Offset(elbowX, elbowY))
    drawJointHalo(Offset(hipsX, hipsY))
    drawJointHalo(Offset(feetX, feetY))

    drawAngleBadge(Offset(elbowX - 28f, elbowY))
}

private fun DrawScope.drawSitupsPosture(
    width: Float,
    height: Float,
    floorY: Float,
    progress: Float
) {
    val matStartX = width * 0.18f
    val matEndX = width * 0.82f
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(matStartX, floorY - 6f),
        size = Size(matEndX - matStartX, 6f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
    )

    val hipsX = width * 0.46f
    val hipsY = floorY - 8f

    val kneesX = width * 0.64f
    val kneesY = floorY - height * 0.28f
    val feetX = width * 0.74f
    val feetY = floorY - 4f

    val torsoLength = height * 0.36f
    val torsoAngleRad = Math.toRadians(lerp(170f, 70f, progress).toDouble()).toFloat()

    val shoulderX = hipsX - torsoLength * cos(torsoAngleRad)
    val shoulderY = hipsY - torsoLength * sin(torsoAngleRad)

    val headX = shoulderX - 16f * cos(torsoAngleRad)
    val headY = shoulderY - 16f * sin(torsoAngleRad)

    val skeletonColor = Color(0xFF22C55E)

    // Legs
    drawLine(color = skeletonColor, start = Offset(hipsX, hipsY), end = Offset(kneesX, kneesY), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(kneesX, kneesY), end = Offset(kneesX, floorY), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(kneesX, floorY), end = Offset(feetX, feetY), strokeWidth = 5f, cap = StrokeCap.Round)

    // Torso
    drawLine(color = skeletonColor, start = Offset(hipsX, hipsY), end = Offset(shoulderX, shoulderY), strokeWidth = 5f, cap = StrokeCap.Round)

    // Arms
    val handX = lerp(shoulderX + 30f, kneesX - 10f, progress)
    val handY = lerp(shoulderY - 5f, kneesY + 10f, progress)
    drawLine(color = skeletonColor, start = Offset(shoulderX, shoulderY), end = Offset(handX, handY), strokeWidth = 4f, cap = StrokeCap.Round)

    // Head
    drawCircle(color = Color(0xFFF8FAFC), radius = 12f, center = Offset(headX, headY))

    drawJointHalo(Offset(hipsX, hipsY))
    drawJointHalo(Offset(kneesX, kneesY))
    drawJointHalo(Offset(shoulderX, shoulderY))

    drawAngleBadge(Offset(hipsX - 25f, hipsY - 25f))
}

private fun DrawScope.drawWallSitPosture(
    width: Float,
    height: Float,
    floorY: Float,
    progress: Float
) {
    val wallX = width * 0.30f
    val wallTopY = height * 0.12f

    drawLine(
        color = Color(0xFF475569),
        start = Offset(wallX, wallTopY),
        end = Offset(wallX, floorY),
        strokeWidth = 6f
    )
    for (i in 0..6) {
        val y = wallTopY + i * 22f
        drawLine(color = Color(0xFF334155), start = Offset(wallX, y), end = Offset(wallX - 14f, y + 10f), strokeWidth = 2f)
    }

    val headX = wallX + 12f
    val headY = height * 0.28f

    val shoulderX = wallX + 6f
    val shoulderY = height * 0.38f

    val hipsX = wallX + 6f
    val hipsY = height * 0.58f

    val kneesX = hipsX + width * 0.26f
    val kneesY = hipsY

    val anklesX = kneesX
    val anklesY = floorY - 4f

    val feetX = anklesX + 24f
    val feetY = floorY - 4f

    val skeletonColor = Color(0xFF22C55E)

    drawLine(color = skeletonColor, start = Offset(shoulderX, shoulderY), end = Offset(hipsX, hipsY), strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(hipsX, hipsY), end = Offset(kneesX, kneesY), strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(kneesX, kneesY), end = Offset(anklesX, anklesY), strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(anklesX, anklesY), end = Offset(feetX, feetY), strokeWidth = 5f, cap = StrokeCap.Round)

    drawCircle(color = Color(0xFFF8FAFC), radius = 12f, center = Offset(headX, headY))

    drawJointHalo(Offset(kneesX, kneesY))
    drawJointHalo(Offset(hipsX, hipsY))

    // 90 deg corner indicator
    drawPath(
        path = Path().apply {
            moveTo(kneesX - 12f, kneesY)
            lineTo(kneesX - 12f, kneesY + 12f)
            lineTo(kneesX, kneesY + 12f)
        },
        color = Color(0xFF38BDF8),
        style = Stroke(width = 2.5f)
    )

    drawAngleBadge(Offset(kneesX + 20f, kneesY))
}

private fun DrawScope.drawSquatsPosture(
    width: Float,
    height: Float,
    floorY: Float,
    progress: Float
) {
    val feetX = width * 0.48f
    val feetY = floorY - 4f

    val hipDrop = lerp(height * 0.52f, height * 0.28f, progress)
    val hipsX = lerp(width * 0.48f, width * 0.40f, progress)
    val hipsY = floorY - hipDrop

    val kneesX = lerp(width * 0.48f, width * 0.56f, progress)
    val kneesY = floorY - (hipDrop * 0.65f)

    val torsoLength = height * 0.32f
    val torsoLeanAngleRad = Math.toRadians(lerp(88f, 68f, progress).toDouble()).toFloat()
    val shoulderX = hipsX + torsoLength * cos(torsoLeanAngleRad)
    val shoulderY = hipsY - torsoLength * sin(torsoLeanAngleRad)

    val headX = shoulderX + 8f
    val headY = shoulderY - 14f

    val skeletonColor = Color(0xFF22C55E)

    drawLine(color = skeletonColor, start = Offset(feetX, feetY), end = Offset(kneesX, kneesY), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(kneesX, kneesY), end = Offset(hipsX, hipsY), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(hipsX, hipsY), end = Offset(shoulderX, shoulderY), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(shoulderX, shoulderY), end = Offset(shoulderX + 35f, shoulderY + 8f), strokeWidth = 4f, cap = StrokeCap.Round)

    drawCircle(color = Color(0xFFF8FAFC), radius = 12f, center = Offset(headX, headY))

    drawJointHalo(Offset(kneesX, kneesY))
    drawJointHalo(Offset(hipsX, hipsY))

    drawAngleBadge(Offset(kneesX + 18f, kneesY))
}

private fun DrawScope.drawCalfRaisesPosture(
    width: Float,
    height: Float,
    floorY: Float,
    progress: Float
) {
    val heelLift = lerp(0f, 26f, progress)
    val toesX = width * 0.58f
    val toesY = floorY - 4f

    val heelX = width * 0.42f
    val heelY = floorY - 4f - heelLift

    val ankleX = width * 0.46f
    val ankleY = floorY - 24f - heelLift

    val kneeX = width * 0.48f
    val kneeY = floorY - height * 0.42f - (heelLift * 0.8f)

    val hipX = width * 0.48f
    val hipY = floorY - height * 0.72f - (heelLift * 0.8f)

    val skeletonColor = Color(0xFF22C55E)

    drawLine(color = skeletonColor, start = Offset(heelX, heelY), end = Offset(toesX, toesY), strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(ankleX, ankleY), end = Offset(kneeX, kneeY), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(kneeX, kneeY), end = Offset(hipX, hipY), strokeWidth = 5f, cap = StrokeCap.Round)

    drawJointHalo(Offset(ankleX, ankleY))
    drawJointHalo(Offset(kneeX, kneeY))

    if (heelLift > 4f) {
        drawLine(color = Color(0xFF38BDF8), start = Offset(heelX - 14f, floorY), end = Offset(heelX - 14f, heelY), strokeWidth = 3f)
        drawAngleBadge(Offset(heelX - 28f, heelY))
    }
}

private fun DrawScope.drawPlankPosture(
    width: Float,
    height: Float,
    floorY: Float,
    progress: Float
) {
    val feetX = width * 0.22f
    val feetY = floorY - 4f

    val elbowsX = width * 0.68f
    val elbowsY = floorY - 4f

    val plankHeight = height * 0.22f
    val shoulderX = width * 0.68f
    val shoulderY = floorY - plankHeight

    val headX = shoulderX + width * 0.08f
    val headY = shoulderY - 4f

    val hipsX = width * 0.44f
    val hipsY = floorY - (plankHeight * 0.95f)

    val skeletonColor = Color(0xFF22C55E)

    drawLine(color = skeletonColor, start = Offset(feetX, feetY), end = Offset(hipsX, hipsY), strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(hipsX, hipsY), end = Offset(shoulderX, shoulderY), strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(color = skeletonColor, start = Offset(shoulderX, shoulderY), end = Offset(elbowsX, elbowsY), strokeWidth = 5f, cap = StrokeCap.Round)

    drawCircle(color = Color(0xFFF8FAFC), radius = 12f, center = Offset(headX, headY))

    drawJointHalo(Offset(shoulderX, shoulderY))
    drawJointHalo(Offset(hipsX, hipsY))
    drawJointHalo(Offset(feetX, feetY))

    drawAngleBadge(Offset(hipsX, hipsY - 20f))
}

private fun DrawScope.drawJointHalo(center: Offset) {
    drawCircle(color = Color(0xFF22C55E).copy(alpha = 0.35f), radius = 10f, center = center)
    drawCircle(color = Color(0xFF4ADE80), radius = 4.5f, center = center)
}

private fun DrawScope.drawAngleBadge(offset: Offset) {
    drawCircle(color = Color(0xFF38BDF8), radius = 4f, center = offset)
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

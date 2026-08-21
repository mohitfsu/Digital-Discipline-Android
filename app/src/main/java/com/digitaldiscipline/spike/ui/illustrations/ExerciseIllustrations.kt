package com.digitaldiscipline.spike.ui.illustrations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Design Colors for Illustrations
// ─────────────────────────────────────────────────────────────────────────────
private val PrimaryCyan = Color(0xFF38BDF8)
private val SecondaryTeal = Color(0xFF2DD4BF)
private val AccentEmerald = Color(0xFF34D399)
private val HighlightAmber = Color(0xFFFBBF24)
private val DarkOutline = Color(0xFF1E293B)
private val BoneWhite = Color(0xFFF8FAFC)

/**
 * Universal Exercise Illustration Dispatcher
 */
@Composable
fun ExerciseIllustration(
    exerciseId: String,
    modifier: Modifier = Modifier.size(140.dp, 100.dp)
) {
    when (exerciseId) {
        "PUSH_UPS", "PULL_UPS" -> PushUpIllustration(modifier)
        "SQUATS", "SIT_TO_STAND" -> SquatsIllustration(modifier)
        "LUNGES" -> LungesIllustration(modifier)
        "PLANK" -> PlankIllustration(modifier)
        "WALL_SIT" -> WallSitIllustration(modifier)
        "JUMPING_JACKS", "HIGH_KNEES", "CALF_RAISES" -> JumpingJacksIllustration(modifier)
        "TREE_POSE", "MOUNTAIN_POSE" -> TreePoseIllustration(modifier)
        "FORWARD_FOLD", "CHILD_POSE" -> ForwardFoldIllustration(modifier)
        "SHOULDER_STRETCH", "STRETCH", "MINI_SUN_SALUTATION" -> StretchIllustration(modifier)
        "BOX_BREATHING", "FOUR_TWO_SIX_BREATHING", "ONE_MINUTE_BREATHING_RESET", "THREE_BREATH_RESET" -> BreathingIllustration(modifier)
        "LOOK_AWAY_FROM_SCREEN" -> EyeReliefIllustration(modifier)
        "DRINK_WATER" -> HydrationIllustration(modifier)
        "POSTURE_RESET", "STAND_UP" -> PostureIllustration(modifier)
        else -> BreathingIllustration(modifier)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. PUSH-UP ILLUSTRATION (Animated Motion)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PushUpIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pushup")
    val animY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pushupY"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.85f

        // Ground line
        drawLine(
            color = DarkOutline,
            start = Offset(w * 0.1f, groundY),
            end = Offset(w * 0.9f, groundY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        val bodyOffset = animY.dp.toPx()

        // Feet pivot
        val footX = w * 0.22f
        val footY = groundY - 6.dp.toPx()

        // Hands on ground
        val handX = w * 0.72f
        val handY = groundY - 4.dp.toPx()

        // Shoulders (moves down & up)
        val shoulderX = w * 0.70f
        val shoulderY = h * 0.42f + bodyOffset

        // Head
        val headX = w * 0.82f
        val headY = shoulderY - 8.dp.toPx()

        // Arms (Shoulder to Hands)
        drawLine(
            color = PrimaryCyan,
            start = Offset(shoulderX, shoulderY),
            end = Offset(handX, handY),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Torso & Legs (Plank line)
        drawLine(
            color = BoneWhite,
            start = Offset(footX, footY),
            end = Offset(shoulderX, shoulderY),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Head circle
        drawCircle(
            color = SecondaryTeal,
            radius = 9.dp.toPx(),
            center = Offset(headX, headY)
        )

        // Motion guidance arrow
        drawArc(
            color = AccentEmerald.copy(alpha = 0.7f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.64f, h * 0.22f),
            size = Size(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. SQUATS ILLUSTRATION (Animated Motion)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SquatsIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "squats")
    val squatDepth by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "squatDepth"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.90f
        val depthPx = squatDepth.dp.toPx()

        // Ground line
        drawLine(
            color = DarkOutline,
            start = Offset(w * 0.2f, groundY),
            end = Offset(w * 0.8f, groundY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        val feetX = w * 0.45f
        val feetY = groundY - 4.dp.toPx()

        // Knees bend forward
        val kneeX = w * 0.58f + (depthPx * 0.2f)
        val kneeY = h * 0.65f + (depthPx * 0.3f)

        // Hips sink back
        val hipX = w * 0.38f - (depthPx * 0.3f)
        val hipY = h * 0.50f + depthPx

        // Torso / Shoulder
        val shoulderX = w * 0.48f
        val shoulderY = h * 0.28f + (depthPx * 0.9f)

        // Head
        val headX = w * 0.50f
        val headY = shoulderY - 14.dp.toPx()

        // Extended Arms
        val handX = w * 0.76f
        val handY = shoulderY + 2.dp.toPx()

        // Lower leg (Foot to Knee)
        drawLine(
            color = BoneWhite,
            start = Offset(feetX, feetY),
            end = Offset(kneeX, kneeY),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Upper leg (Knee to Hip)
        drawLine(
            color = BoneWhite,
            start = Offset(kneeX, kneeY),
            end = Offset(hipX, hipY),
            strokeWidth = 5.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Torso (Hip to Shoulder)
        drawLine(
            color = PrimaryCyan,
            start = Offset(hipX, hipY),
            end = Offset(shoulderX, shoulderY),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Arms
        drawLine(
            color = AccentEmerald,
            start = Offset(shoulderX, shoulderY),
            end = Offset(handX, handY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Head
        drawCircle(
            color = SecondaryTeal,
            radius = 10.dp.toPx(),
            center = Offset(headX, headY)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. PLANK / ISOMETRIC ILLUSTRATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlankIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "plank")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plankGlow"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.85f

        // Ground
        drawLine(
            color = DarkOutline,
            start = Offset(w * 0.1f, groundY),
            end = Offset(w * 0.9f, groundY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        val feetX = w * 0.20f
        val feetY = groundY - 4.dp.toPx()

        val elbowX = w * 0.72f
        val elbowY = groundY - 2.dp.toPx()

        val shoulderX = w * 0.70f
        val shoulderY = h * 0.52f

        val headX = w * 0.82f
        val headY = shoulderY - 6.dp.toPx()

        // Forearm on ground
        drawLine(
            color = PrimaryCyan,
            start = Offset(w * 0.80f, elbowY),
            end = Offset(elbowX, elbowY),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Upper arm (vertical)
        drawLine(
            color = PrimaryCyan,
            start = Offset(elbowX, elbowY),
            end = Offset(shoulderX, shoulderY),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Straight Body Line (Gold Standard Form)
        drawLine(
            color = BoneWhite,
            start = Offset(feetX, feetY),
            end = Offset(shoulderX, shoulderY),
            strokeWidth = 6.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Core activation aura
        drawCircle(
            color = AccentEmerald.copy(alpha = 0.25f * pulseGlow),
            radius = 16.dp.toPx(),
            center = Offset(w * 0.48f, h * 0.58f)
        )

        // Head
        drawCircle(
            color = SecondaryTeal,
            radius = 9.dp.toPx(),
            center = Offset(headX, headY)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. LUNGES ILLUSTRATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LungesIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.88f

        // Ground
        drawLine(color = DarkOutline, start = Offset(w * 0.1f, groundY), end = Offset(w * 0.9f, groundY), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)

        // Front foot & knee (90 degree bend)
        val frontFootX = w * 0.68f
        val frontKneeX = w * 0.68f
        val frontKneeY = h * 0.60f

        // Back foot & knee (90 degree bend)
        val backFootX = w * 0.25f
        val backKneeX = w * 0.38f
        val backKneeY = groundY - 8.dp.toPx()

        // Hips & Torso (vertical)
        val hipX = w * 0.48f
        val hipY = h * 0.52f
        val shoulderX = w * 0.48f
        val shoulderY = h * 0.28f
        val headX = w * 0.48f
        val headY = shoulderY - 12.dp.toPx()

        // Front leg lines
        drawLine(color = BoneWhite, start = Offset(frontFootX, groundY - 4.dp.toPx()), end = Offset(frontKneeX, frontKneeY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = BoneWhite, start = Offset(frontKneeX, frontKneeY), end = Offset(hipX, hipY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)

        // Back leg lines
        drawLine(color = PrimaryCyan, start = Offset(backFootX, groundY - 4.dp.toPx()), end = Offset(backKneeX, backKneeY), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = PrimaryCyan, start = Offset(backKneeX, backKneeY), end = Offset(hipX, hipY), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)

        // Upright Torso
        drawLine(color = BoneWhite, start = Offset(hipX, hipY), end = Offset(shoulderX, shoulderY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)

        // Head
        drawCircle(color = SecondaryTeal, radius = 9.dp.toPx(), center = Offset(headX, headY))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. WALL SIT ILLUSTRATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WallSitIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.88f
        val wallX = w * 0.35f

        // Wall & Ground
        drawLine(color = DarkOutline, start = Offset(wallX, h * 0.15f), end = Offset(wallX, groundY), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Square)
        drawLine(color = DarkOutline, start = Offset(wallX - 10.dp.toPx(), groundY), end = Offset(w * 0.85f, groundY), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)

        // Back flat against wall
        val shoulderY = h * 0.32f
        val hipY = h * 0.58f

        // Thigh horizontal (90 deg)
        val kneeX = wallX + 38.dp.toPx()
        val kneeY = hipY

        // Shin vertical
        val footY = groundY - 4.dp.toPx()

        // Legs
        drawLine(color = BoneWhite, start = Offset(kneeX, footY), end = Offset(kneeX, kneeY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = BoneWhite, start = Offset(wallX + 4.dp.toPx(), hipY), end = Offset(kneeX, kneeY), strokeWidth = 5.5.dp.toPx(), cap = StrokeCap.Round)

        // Torso against wall
        drawLine(color = PrimaryCyan, start = Offset(wallX + 4.dp.toPx(), hipY), end = Offset(wallX + 4.dp.toPx(), shoulderY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)

        // Head
        drawCircle(color = SecondaryTeal, radius = 9.dp.toPx(), center = Offset(wallX + 4.dp.toPx(), shoulderY - 12.dp.toPx()))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. JUMPING JACKS ILLUSTRATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun JumpingJacksIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "jj")
    val spread by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jjSpread"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.90f
        val centerX = w * 0.5f

        val hipY = h * 0.56f
        val shoulderY = h * 0.32f
        val headY = shoulderY - 13.dp.toPx()

        val legSpreadPx = 28.dp.toPx() * spread
        val armSpreadPx = 32.dp.toPx() * spread

        // Torso
        drawLine(color = BoneWhite, start = Offset(centerX, hipY), end = Offset(centerX, shoulderY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)

        // Legs
        drawLine(color = PrimaryCyan, start = Offset(centerX, hipY), end = Offset(centerX - legSpreadPx, groundY - 4.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = PrimaryCyan, start = Offset(centerX, hipY), end = Offset(centerX + legSpreadPx, groundY - 4.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)

        // Arms overhead / down
        val armAngleY = shoulderY - (22.dp.toPx() * spread) + (14.dp.toPx() * (1f - spread))
        drawLine(color = AccentEmerald, start = Offset(centerX, shoulderY), end = Offset(centerX - armSpreadPx, armAngleY), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = AccentEmerald, start = Offset(centerX, shoulderY), end = Offset(centerX + armSpreadPx, armAngleY), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)

        // Head
        drawCircle(color = SecondaryTeal, radius = 10.dp.toPx(), center = Offset(centerX, headY))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. TREE POSE (Balance / Yoga)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TreePoseIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.90f
        val centerX = w * 0.5f

        val hipY = h * 0.56f
        val shoulderY = h * 0.32f
        val headY = shoulderY - 13.dp.toPx()

        // Standing leg (straight down)
        drawLine(color = BoneWhite, start = Offset(centerX, hipY), end = Offset(centerX, groundY - 4.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)

        // Bent leg (foot on inner thigh)
        val kneeX = centerX - 24.dp.toPx()
        val kneeY = h * 0.68f
        drawLine(color = PrimaryCyan, start = Offset(centerX, hipY), end = Offset(kneeX, kneeY), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = PrimaryCyan, start = Offset(kneeX, kneeY), end = Offset(centerX, h * 0.72f), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)

        // Torso
        drawLine(color = BoneWhite, start = Offset(centerX, hipY), end = Offset(centerX, shoulderY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)

        // Hands in prayer (Anjali Mudra at chest)
        drawLine(color = AccentEmerald, start = Offset(centerX, shoulderY), end = Offset(centerX - 10.dp.toPx(), shoulderY + 12.dp.toPx()), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = AccentEmerald, start = Offset(centerX, shoulderY), end = Offset(centerX + 10.dp.toPx(), shoulderY + 12.dp.toPx()), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = AccentEmerald, start = Offset(centerX - 10.dp.toPx(), shoulderY + 12.dp.toPx()), end = Offset(centerX, shoulderY + 8.dp.toPx()), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = AccentEmerald, start = Offset(centerX + 10.dp.toPx(), shoulderY + 12.dp.toPx()), end = Offset(centerX, shoulderY + 8.dp.toPx()), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)

        // Head
        drawCircle(color = SecondaryTeal, radius = 9.5.dp.toPx(), center = Offset(centerX, headY))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. FORWARD FOLD / MOBILITY
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ForwardFoldIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.90f
        val footX = w * 0.42f

        // Legs upright
        val hipX = w * 0.42f
        val hipY = h * 0.48f
        drawLine(color = BoneWhite, start = Offset(footX, groundY - 4.dp.toPx()), end = Offset(hipX, hipY), strokeWidth = 5.5.dp.toPx(), cap = StrokeCap.Round)

        // Torso folded downward towards feet
        val shoulderX = w * 0.50f
        val shoulderY = h * 0.72f
        drawLine(color = PrimaryCyan, start = Offset(hipX, hipY), end = Offset(shoulderX, shoulderY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)

        // Arms reaching to floor
        val handX = w * 0.52f
        val handY = groundY - 6.dp.toPx()
        drawLine(color = AccentEmerald, start = Offset(shoulderX, shoulderY), end = Offset(handX, handY), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)

        // Head hanging down
        drawCircle(color = SecondaryTeal, radius = 9.dp.toPx(), center = Offset(shoulderX, shoulderY + 11.dp.toPx()))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. STRETCH / FULL BODY
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StretchIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.90f
        val centerX = w * 0.5f

        val hipY = h * 0.58f
        val shoulderY = h * 0.32f
        val headY = shoulderY - 13.dp.toPx()

        // Legs grounded
        drawLine(color = BoneWhite, start = Offset(centerX - 10.dp.toPx(), groundY - 4.dp.toPx()), end = Offset(centerX, hipY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = BoneWhite, start = Offset(centerX + 10.dp.toPx(), groundY - 4.dp.toPx()), end = Offset(centerX, hipY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)

        // Torso
        drawLine(color = BoneWhite, start = Offset(centerX, hipY), end = Offset(centerX, shoulderY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)

        // Arms reaching high overhead in a V
        drawLine(color = PrimaryCyan, start = Offset(centerX, shoulderY), end = Offset(centerX - 24.dp.toPx(), h * 0.12f), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = PrimaryCyan, start = Offset(centerX, shoulderY), end = Offset(centerX + 24.dp.toPx(), h * 0.12f), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)

        // Head
        drawCircle(color = SecondaryTeal, radius = 9.5.dp.toPx(), center = Offset(centerX, headY))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. BREATHING & LUNGS VISUAL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BreathingIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxR = size.minDimension * 0.38f * scale

        // Outermost ripple
        drawCircle(
            color = PrimaryCyan.copy(alpha = 0.15f),
            radius = maxR * 1.25f,
            center = center
        )

        // Middle aura
        drawCircle(
            color = SecondaryTeal.copy(alpha = 0.35f),
            radius = maxR,
            center = center
        )

        // Inner glowing core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                center = center,
                radius = maxR * 0.65f
            ),
            radius = maxR * 0.65f,
            center = center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 11. EYE RELIEF (20-20-20 Rule)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EyeReliefIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2, h / 2)

        val eyePath = Path().apply {
            moveTo(center.x - 36.dp.toPx(), center.y)
            quadraticTo(center.x, center.y - 24.dp.toPx(), center.x + 36.dp.toPx(), center.y)
            quadraticTo(center.x, center.y + 24.dp.toPx(), center.x - 36.dp.toPx(), center.y)
            close()
        }

        drawPath(
            path = eyePath,
            color = DarkOutline,
            style = Stroke(width = 3.dp.toPx())
        )

        // Iris
        drawCircle(color = PrimaryCyan, radius = 12.dp.toPx(), center = center)
        // Pupil
        drawCircle(color = BoneWhite, radius = 5.dp.toPx(), center = center)

        // Distant horizon lines
        drawLine(color = SecondaryTeal.copy(alpha = 0.6f), start = Offset(w * 0.15f, h * 0.20f), end = Offset(w * 0.85f, h * 0.20f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = SecondaryTeal.copy(alpha = 0.3f), start = Offset(w * 0.25f, h * 0.12f), end = Offset(w * 0.75f, h * 0.12f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 12. HYDRATION (Glass of water)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HydrationIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2, h / 2)

        val glassWidth = 32.dp.toPx()
        val glassHeight = 44.dp.toPx()

        // Glass Outline
        val glassPath = Path().apply {
            moveTo(center.x - glassWidth, center.y - glassHeight / 2)
            lineTo(center.x - (glassWidth * 0.75f), center.y + glassHeight / 2)
            lineTo(center.x + (glassWidth * 0.75f), center.y + glassHeight / 2)
            lineTo(center.x + glassWidth, center.y - glassHeight / 2)
        }

        drawPath(
            path = glassPath,
            color = BoneWhite.copy(alpha = 0.8f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Water fill
        val waterPath = Path().apply {
            moveTo(center.x - (glassWidth * 0.85f), center.y - 4.dp.toPx())
            lineTo(center.x - (glassWidth * 0.72f), center.y + glassHeight / 2 - 2.dp.toPx())
            lineTo(center.x + (glassWidth * 0.72f), center.y + glassHeight / 2 - 2.dp.toPx())
            lineTo(center.x + (glassWidth * 0.85f), center.y - 4.dp.toPx())
            close()
        }

        drawPath(
            path = waterPath,
            brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
        )

        // Water droplet
        drawCircle(color = AccentEmerald, radius = 4.dp.toPx(), center = Offset(center.x, center.y - glassHeight / 2 - 10.dp.toPx()))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 13. POSTURE ALIGNMENT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PostureIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f

        // Straight plumb line (neutral spine guide)
        drawLine(
            color = AccentEmerald.copy(alpha = 0.4f),
            start = Offset(centerX, h * 0.15f),
            end = Offset(centerX, h * 0.85f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Spine vertebrae dots
        for (i in 0..5) {
            val y = h * 0.32f + (i * 8.dp.toPx())
            drawCircle(color = PrimaryCyan, radius = 3.5.dp.toPx(), center = Offset(centerX, y))
        }

        // Head aligned atop plumb line
        drawCircle(color = SecondaryTeal, radius = 10.dp.toPx(), center = Offset(centerX, h * 0.22f))
    }
}

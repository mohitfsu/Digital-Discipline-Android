package com.digitaldiscipline.spike.ui.vision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.digitaldiscipline.spike.intervention.session.InterventionSession
import com.digitaldiscipline.spike.intervention.validation.CameraPoseValidator
import com.digitaldiscipline.spike.intervention.vision.CameraPoseAnalyzer
import com.digitaldiscipline.spike.intervention.vision.PoseClassificationResult
import com.digitaldiscipline.spike.intervention.vision.PoseSkeletalCanvas
import com.google.mlkit.vision.pose.Pose
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

enum class WorkoutStage {
    POSITIONING,
    COUNTDOWN_3,
    COUNTDOWN_2,
    COUNTDOWN_1,
    COUNTDOWN_GO,
    ACTIVE,
    COMPLETED
}

/**
 * Enhanced Real-Time Camera Computer Vision Pose Workout Screen.
 *
 * Features:
 * - Animated Camera Placement & Posture Illustrations: Dedicated animated sketches of phone angle, distance, and looping skeleton exercises.
 * - 3-2-1-GO Sequence: Uninterrupted countdown once user is in valid starting stance.
 * - Dynamic Color Feedback: Wireframe & joints turn 🟢 Neon Green when posture is correct and 🔴 Bright Red when broken.
 * - Instant Pause & Resume: Counter/timer freezes immediately if posture breaks, resuming only when form is restored.
 * - Targeted Joint Pointing: Observes & points to key joints with angles (Elbows, Knees, Hips, Ankles).
 * - Instant Running Challenge Switcher: Allows seamless switching to another challenge mid-workout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPoseWorkoutScreen(
    exerciseId: String = "PUSH_UPS",
    exerciseTitle: String = "Push-ups",
    targetReps: Int = 10,
    targetHoldSeconds: Int = 30,
    session: InterventionSession? = null,
    rewardMinutes: Int = (session?.rewardSeconds ?: 600) / 60,
    onComplete: (earnedSeconds: Int) -> Unit,
    onSwitchToMotionSensor: (() -> Unit)? = null,
    onSwitchChallenge: ((challengeId: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var activeExerciseId by remember(exerciseId) { mutableStateOf(exerciseId) }
    var activeExerciseTitle by remember(exerciseTitle) { mutableStateOf(exerciseTitle) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var showSwitchChallengeSheet by remember { mutableStateOf(false) }
    var showPlacementGuideDialog by remember { mutableStateOf(false) }
    var guideViewMode by remember { mutableStateOf(GuideViewMode.CAMERA_PLACEMENT) }

    val requestCameraPermission = {
        CameraPermissionActivity.launch(context)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            requestCameraPermission()
        }
    }

    // Periodically re-check permission if not yet granted
    LaunchedEffect(Unit) {
        while (!hasCameraPermission) {
            delay(1000L)
            hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }
    }

    val isHoldExercise = remember(activeExerciseId) {
        when (activeExerciseId.uppercase()) {
            "PUSH_UPS", "PUSHUPS",
            "SQUATS", "BODYWEIGHT_SQUATS",
            "SIT_UPS", "SITUPS", "CRUNCHES",
            "LUNGES", "ALTERNATING_LUNGES",
            "JUMPING_JACKS",
            "HIGH_KNEES",
            "CALF_RAISES", "CALF_RAISE",
            "SIT_TO_STAND", "SIT_STAND", "CHAIR_STAND" -> false
            else -> true
        }
    }

    // Active Validator instance
    var cameraPoseValidator by remember(activeExerciseId) {
        mutableStateOf(
            CameraPoseValidator(
                exerciseId = activeExerciseId,
                targetReps = targetReps,
                targetHoldSeconds = kotlin.math.max(30, targetHoldSeconds)
            )
        )
    }

    LaunchedEffect(session, activeExerciseId) {
        if (session != null) {
            cameraPoseValidator.startValidation(session) { result ->
                if (result is com.digitaldiscipline.spike.intervention.validation.ValidationResult.Completed) {
                    onComplete(session.rewardSeconds)
                }
            }
        }
    }

    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }

    var workoutStage by remember(activeExerciseId) { mutableStateOf(WorkoutStage.POSITIONING) }

    var classificationResult by remember(activeExerciseId) {
        mutableStateOf(
            PoseClassificationResult(
                currentReps = 0,
                targetReps = targetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = targetHoldSeconds,
                feedbackMessage = "Get into starting position",
                isCompleted = false,
                isPostureCorrect = false,
                isReadyToStart = false
            )
        )
    }

    // Robust 1-2-3-GO Sequence
    var triggerCountdown by remember(activeExerciseId) { mutableStateOf(false) }
    LaunchedEffect(classificationResult.isReadyToStart, workoutStage) {
        if (workoutStage == WorkoutStage.POSITIONING && classificationResult.isReadyToStart && !triggerCountdown) {
            triggerCountdown = true
        }
    }

    LaunchedEffect(triggerCountdown) {
        if (triggerCountdown && workoutStage == WorkoutStage.POSITIONING) {
            workoutStage = WorkoutStage.COUNTDOWN_3
            delay(1000L)
            workoutStage = WorkoutStage.COUNTDOWN_2
            delay(1000L)
            workoutStage = WorkoutStage.COUNTDOWN_1
            delay(1000L)
            workoutStage = WorkoutStage.COUNTDOWN_GO
            delay(600L)
            workoutStage = WorkoutStage.ACTIVE
            triggerCountdown = false
        }
    }

    // Timer state
    var elapsedSeconds by remember(activeExerciseId) { mutableIntStateOf(0) }
    LaunchedEffect(workoutStage, classificationResult.isPostureCorrect) {
        while (workoutStage == WorkoutStage.ACTIVE && classificationResult.isPostureCorrect) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var activeAnalyzer by remember { mutableStateOf<CameraPoseAnalyzer?>(null) }

    // Strict Lifecycle Shutdown Observer
    DisposableEffect(lifecycleOwner, activeExerciseId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                cameraProviderInstance?.unbindAll()
                activeAnalyzer?.close()
                cameraPoseValidator.stopValidation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraProviderInstance?.unbindAll()
            activeAnalyzer?.close()
            cameraPoseValidator.stopValidation()
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // 1. Live Camera Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderInstance = cameraProvider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val analyzer = CameraPoseAnalyzer { pose, w, h, rotation ->
                            currentPose = pose
                            if (rotation == 90 || rotation == 270) {
                                imageWidth = h
                                imageHeight = w
                            } else {
                                imageWidth = w
                                imageHeight = h
                            }

                            // Pass isCountingActive: Reps & Timers only increment AFTER "GO!"
                            val isCountingActive = (workoutStage == WorkoutStage.ACTIVE)
                            val result = cameraPoseValidator.onPoseReceived(pose, isCountingActive)
                            if (result != null) {
                                classificationResult = result
                                if (result.isCompleted) {
                                    workoutStage = WorkoutStage.COMPLETED
                                    if (session == null) {
                                        onComplete(rewardMinutes * 60)
                                    }
                                }
                            }
                        }
                        activeAnalyzer = analyzer

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, analyzer)
                            }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )
                        } catch (_: Exception) {
                            try {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalyzer
                                )
                            } catch (_: Exception) {}
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = { previewView ->
                    val cameraProvider = cameraProviderInstance ?: return@AndroidView
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also {
                            activeAnalyzer?.let { analyzer -> it.setAnalyzer(cameraExecutor, analyzer) }
                        }

                    val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Real-Time Skeletal Wireframe Overlay
            if (imageWidth > 0 && imageHeight > 0) {
                PoseSkeletalCanvas(
                    pose = currentPose,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT,
                    isActionActive = workoutStage == WorkoutStage.ACTIVE || workoutStage == WorkoutStage.COMPLETED,
                    isPostureCorrect = classificationResult.isPostureCorrect,
                    exerciseId = activeExerciseId,
                    activeAngles = classificationResult.activeAngles,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Camera Permission Needed State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📷", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Digital Discipline processes all camera frames on-device for pose validation. Zero images or video ever leave your device.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { requestCameraPermission() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enable Camera", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (onSwitchToMotionSensor != null) {
                        OutlinedButton(
                            onClick = onSwitchToMotionSensor,
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Use Sensor", color = Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        val isPostureGood = classificationResult.isPostureCorrect
        val isInActivePhase = workoutStage == WorkoutStage.ACTIVE

        // =========================================================================
        // 3. TOP OVERLAY: HEADER, TIMER, FLIP CAMERA & SWITCH CHALLENGE BUTTONS
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .size(38.dp)
                    .clickable {
                        cameraPoseValidator.onUserCancelled("User dismissed workout")
                        onDismiss()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Exercise Title & Status Subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeExerciseTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = when (workoutStage) {
                        WorkoutStage.POSITIONING -> "Get Ready"
                        WorkoutStage.COUNTDOWN_3, WorkoutStage.COUNTDOWN_2, WorkoutStage.COUNTDOWN_1, WorkoutStage.COUNTDOWN_GO -> "Starting..."
                        WorkoutStage.ACTIVE -> if (isHoldExercise) "${classificationResult.holdSeconds}s / ${targetHoldSeconds}s HOLD" else "${classificationResult.currentReps} / ${targetReps} REPS"
                        WorkoutStage.COMPLETED -> "🎉 Completed!"
                    },
                    color = when {
                        workoutStage == WorkoutStage.COMPLETED || isPostureGood -> Color(0xFF22C55E)
                        isInActivePhase && !isPostureGood -> Color(0xFFEF4444)
                        else -> Color(0xFF38BDF8)
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Right Actions: Switch Challenge & Flip Camera
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Switch Challenge Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .clickable { showSwitchChallengeSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔄", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Switch", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Flip Camera Button
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📷", fontSize = 14.sp)
                    }
                }
            }
        }

        // =========================================================================
        // 4. LIVE COACHING & COLOR FEEDBACK BANNER
        // =========================================================================
        val bannerBgColor = when {
            workoutStage == WorkoutStage.COMPLETED -> Color(0xFF064E3B).copy(alpha = 0.95f)
            isInActivePhase && isPostureGood -> Color(0xFF064E3B).copy(alpha = 0.95f)
            isInActivePhase && !isPostureGood -> Color(0xFF7F1D1D).copy(alpha = 0.95f)
            classificationResult.isReadyToStart -> Color(0xFF064E3B).copy(alpha = 0.90f)
            else -> Color(0xFF0F172A).copy(alpha = 0.90f)
        }
        val bannerBorderColor = when {
            workoutStage == WorkoutStage.COMPLETED || isPostureGood -> Color(0xFF22C55E)
            isInActivePhase && !isPostureGood -> Color(0xFFEF4444)
            else -> Color(0xFF38BDF8).copy(alpha = 0.7f)
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bannerBgColor,
            border = BorderStroke(1.5.dp, bannerBorderColor),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 66.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        workoutStage == WorkoutStage.COMPLETED -> "🎉"
                        isInActivePhase && isPostureGood -> "🟢"
                        isInActivePhase && !isPostureGood -> "⚠️"
                        classificationResult.isReadyToStart -> "🟢"
                        else -> "💡"
                    },
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        workoutStage == WorkoutStage.COMPLETED -> "Challenge Completed! 🎉"
                        isInActivePhase && !isPostureGood -> "Posture broken! Counter Paused (return to position)"
                        isInActivePhase && isPostureGood -> classificationResult.feedbackMessage
                        workoutStage == WorkoutStage.POSITIONING && classificationResult.isReadyToStart -> "🟢 Stance Detected! Hold still for countdown..."
                        else -> classificationResult.feedbackMessage
                    },
                    color = when {
                        workoutStage == WorkoutStage.COMPLETED || isPostureGood -> Color(0xFF4ADE80)
                        isInActivePhase && !isPostureGood -> Color(0xFFFCA5A5)
                        else -> Color(0xFF38BDF8)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // =========================================================================
        // 4B. 3... 2... 1... GO! FLASHING COUNTDOWN OVERLAY
        // =========================================================================
        val isCountdownActive = workoutStage in listOf(
            WorkoutStage.COUNTDOWN_3,
            WorkoutStage.COUNTDOWN_2,
            WorkoutStage.COUNTDOWN_1,
            WorkoutStage.COUNTDOWN_GO
        )

        AnimatedVisibility(
            visible = isCountdownActive,
            enter = fadeIn(tween(150)) + scaleIn(tween(200)),
            exit = fadeOut(tween(150)) + scaleOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val countdownText = when (workoutStage) {
                WorkoutStage.COUNTDOWN_3 -> "3"
                WorkoutStage.COUNTDOWN_2 -> "2"
                WorkoutStage.COUNTDOWN_1 -> "1"
                WorkoutStage.COUNTDOWN_GO -> "GO! 🚀"
                else -> ""
            }

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(4.dp, if (workoutStage == WorkoutStage.COUNTDOWN_GO) Color(0xFF22C55E) else Color(0xFF38BDF8)),
                shadowElevation = 24.dp,
                modifier = Modifier.size(160.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = countdownText,
                        color = if (workoutStage == WorkoutStage.COUNTDOWN_GO) Color(0xFF4ADE80) else Color.White,
                        fontSize = if (workoutStage == WorkoutStage.COUNTDOWN_GO) 36.sp else 64.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // =========================================================================
        // 4C. PHONE SETUP & POSTURE ANIMATED GUIDE BUTTON
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 114.dp, end = 16.dp)
                .clickable { showPlacementGuideDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📱 Setup & Form Guide", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // =========================================================================
        // 5. BOTTOM OVERLAY: LARGE METRIC BADGE & FINISH / SWITCH BUTTON
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Glowing Circular Metric Counter
            val isPaused = isInActivePhase && !isPostureGood
            val badgeBgColor = when {
                workoutStage == WorkoutStage.COMPLETED || (isInActivePhase && isPostureGood) -> Color(0xFF059669)
                isPaused -> Color(0xFF991B1B)
                else -> Color(0xFF0284C7)
            }
            val badgeBorderColor = when {
                workoutStage == WorkoutStage.COMPLETED || (isInActivePhase && isPostureGood) -> Color(0xFF4ADE80)
                isPaused -> Color(0xFFEF4444)
                else -> Color(0xFF38BDF8)
            }

            Surface(
                shape = CircleShape,
                color = badgeBgColor,
                border = BorderStroke(4.dp, badgeBorderColor),
                shadowElevation = 16.dp,
                modifier = Modifier.size(92.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isHoldExercise) "${classificationResult.holdSeconds}s" else "${classificationResult.currentReps}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = when {
                            isPaused -> "PAUSED"
                            workoutStage == WorkoutStage.COMPLETED -> "DONE"
                            isInActivePhase && isHoldExercise -> "HOLDING"
                            isInActivePhase -> "COUNTING"
                            else -> if (isHoldExercise) "HOLD" else "REPS"
                        },
                        color = when {
                            isPaused -> Color(0xFFFECACA)
                            isInActivePhase -> Color(0xFFDCFCE7)
                            else -> Color(0xFFBAE6FD)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Switch Challenge + Claim Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary Switch Challenge Button
                OutlinedButton(
                    onClick = { showSwitchChallengeSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("🔄 Switch", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Primary Finish / Claim Time Button
                val isReadyToClaim = classificationResult.isCompleted ||
                        (!isHoldExercise && classificationResult.currentReps >= targetReps) ||
                        (isHoldExercise && classificationResult.holdSeconds >= targetHoldSeconds)

                Button(
                    onClick = {
                        onComplete(rewardMinutes * 60)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReadyToClaim) Color(0xFF059669) else Color(0xFF0284C7)
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Text(
                        text = if (isReadyToClaim) "🎉 Claim ${rewardMinutes}m" else "Claim (+${rewardMinutes}m)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================================================================
        // 6. ANIMATED SETUP & FORM ILLUSTRATION MODAL
        // =========================================================================
        if (showPlacementGuideDialog) {
            val guide = getPhonePlacementGuide(activeExerciseId)
            AlertDialog(
                onDismissRequest = { showPlacementGuideDialog = false },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${guide.iconEmoji} ${guide.title}",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { showPlacementGuideDialog = false }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✕", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Toggle Tabs: Camera Placement vs Posture Form
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (guideViewMode == GuideViewMode.CAMERA_PLACEMENT) Color(0xFF0284C7) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { guideViewMode = GuideViewMode.CAMERA_PLACEMENT }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📹 Phone Setup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (guideViewMode == GuideViewMode.EXERCISE_POSTURE) Color(0xFF059669) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { guideViewMode = GuideViewMode.EXERCISE_POSTURE }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏃 Posture Form", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // High-Definition Animated Illustration Canvas
                        AnimatedExerciseGuideCanvas(
                            exerciseId = activeExerciseId,
                            mode = guideViewMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bulleted Instructions
                        if (guideViewMode == GuideViewMode.CAMERA_PLACEMENT) {
                            Text("📍 Placement: ${guide.phonePlacement}", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("📐 Angle: ${guide.cameraAngle}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("📏 Distance: ${guide.distance}", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("👁️ Alignment: ${guide.whatMustBeVisible}", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("🟢 Skeleton wireframe turns green when stance is held correctly.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showPlacementGuideDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got It • Start Challenge ➔", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // =========================================================================
        // 7. SWITCH CHALLENGE MODAL BOTTOM SHEET
        // =========================================================================
        if (showSwitchChallengeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSwitchChallengeSheet = false },
                containerColor = Color(0xFF090D16),
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Choose Another Challenge",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switch freely without losing your session progress.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val challenges = listOf(
                        Triple("PUSH_UPS", "💪 Push-ups", "10 Reps • Camera Vision"),
                        Triple("SQUATS", "🏋️ Bodyweight Squats", "10 Reps • Camera Vision"),
                        Triple("SIT_UPS", "🤸 Core Sit-ups", "10 Reps • Camera Vision"),
                        Triple("WALL_SIT", "🧱 Wall Sit Hold", "30s • Isometric Camera"),
                        Triple("PLANK", "🧘 Core Plank Hold", "30s • Isometric Camera"),
                        Triple("CALF_RAISES", "🦶 Calf Raises", "15 Reps • Camera Vision"),
                        Triple("JUMPING_JACKS", "⚡ Jumping Jacks", "15 Reps • Dynamic Vision"),
                        Triple("MINDFUL_HANGMAN", "🔤 Mindful Hangman", "Classic Word Game • Mental Reset"),
                        Triple("PICTURE_PUZZLE", "🧩 3x3 Picture Puzzle", "Visual Spatial Reset"),
                        Triple("BOX_BREATHING", "🫁 Box Breathing", "4-4-4-4 Calming Flow"),
                        Triple("STEP_AWAY", "💧 Step Away Walk", "Get water & reset eyes")
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(challenges) { (id, title, desc) ->
                            val isSelected = id.equals(activeExerciseId, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFF0F172A),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showSwitchChallengeSheet = false
                                        // Update active exercise state immediately
                                        activeExerciseId = id
                                        activeExerciseTitle = title.split(" ", limit = 2).getOrElse(1) { title }
                                        workoutStage = WorkoutStage.POSITIONING
                                        triggerCountdown = false
                                        cameraPoseValidator = CameraPoseValidator(
                                            exerciseId = id,
                                            targetReps = targetReps,
                                            targetHoldSeconds = kotlin.math.max(30, targetHoldSeconds)
                                        )
                                        // Inform parent router if available
                                        onSwitchChallenge?.invoke(id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    if (isSelected) {
                                        Text("Active", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Start ➔", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PhonePlacementGuide(
    val title: String,
    val phonePlacement: String,
    val cameraAngle: String,
    val distance: String,
    val whatMustBeVisible: String,
    val iconEmoji: String
)

fun getPhonePlacementGuide(exerciseId: String): PhonePlacementGuide {
    return when (exerciseId.uppercase()) {
        "CALF_RAISES", "CALF_RAISE" -> PhonePlacementGuide(
            title = "Calf Raises Setup",
            phonePlacement = "Floor (propped against wall)",
            cameraAngle = "Tilted up towards lower legs",
            distance = "3 to 5 ft away",
            whatMustBeVisible = "Feet, ankles, and calves clearly in frame",
            iconEmoji = "🦶"
        )
        "PUSH_UPS", "PUSHUPS" -> PhonePlacementGuide(
            title = "Push-ups Setup",
            phonePlacement = "Floor (propped at 45° angle)",
            cameraAngle = "Side profile view across floor",
            distance = "4 to 6 ft away",
            whatMustBeVisible = "Full horizontal body (head, chest, hips) in frame",
            iconEmoji = "💪"
        )
        "SIT_UPS", "SITUPS", "CRUNCHES" -> PhonePlacementGuide(
            title = "Core Sit-ups Setup",
            phonePlacement = "Floor or low stand facing mat",
            cameraAngle = "Side horizontal angle",
            distance = "4 to 6 ft away",
            whatMustBeVisible = "Upper body, hips, and knees visible while lying & curling",
            iconEmoji = "🤸"
        )
        "SQUATS", "BODYWEIGHT_SQUATS", "LUNGES", "ALTERNATING_LUNGES" -> PhonePlacementGuide(
            title = "Squats / Lunges Setup",
            phonePlacement = "Table or chair (waist height)",
            cameraAngle = "Straight forward angle",
            distance = "6 to 8 ft away",
            whatMustBeVisible = "Full body (head to shoes) while standing & descending",
            iconEmoji = "🏋️"
        )
        "WALL_SIT", "WALLSIT" -> PhonePlacementGuide(
            title = "Wall Sit Setup",
            phonePlacement = "Chair or table facing wall",
            cameraAngle = "Side angle showing back-to-wall contact",
            distance = "5 to 7 ft away",
            whatMustBeVisible = "Back flat against wall and 90° bent knees",
            iconEmoji = "🧱"
        )
        "PLANK", "CORE_PLANK" -> PhonePlacementGuide(
            title = "Plank Hold Setup",
            phonePlacement = "Floor (propped on mat)",
            cameraAngle = "Side horizontal view",
            distance = "4 to 6 ft away",
            whatMustBeVisible = "Straight horizontal line from head to heels",
            iconEmoji = "🧘"
        )
        "JUMPING_JACKS", "HIGH_KNEES", "BURPEES" -> PhonePlacementGuide(
            title = "Cardio Sprint Setup",
            phonePlacement = "Table or shelf (chest height)",
            cameraAngle = "Level horizontal",
            distance = "7 to 9 ft away",
            whatMustBeVisible = "Full body from head to feet with overhead clearance",
            iconEmoji = "⚡"
        )
        else -> PhonePlacementGuide(
            title = "Camera Alignment Setup",
            phonePlacement = "Table or level surface",
            cameraAngle = "Straight facing you",
            distance = "6 to 8 ft away",
            whatMustBeVisible = "Full body clearly framed in camera view",
            iconEmoji = "🧍"
        )
    }
}

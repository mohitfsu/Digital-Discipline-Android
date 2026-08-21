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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.digitaldiscipline.spike.intervention.validation.ValidationResult
import com.digitaldiscipline.spike.intervention.vision.CameraPoseAnalyzer
import com.digitaldiscipline.spike.intervention.vision.PoseClassificationResult
import com.digitaldiscipline.spike.intervention.vision.PoseSkeletalCanvas
import com.google.mlkit.vision.pose.Pose
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/**
 * Real-Time Camera Computer Vision Pose Workout Screen.
 *
 * Provides:
 * - Real-time CameraX preview with TextureView (COMPATIBLE mode for floating window overlays).
 * - Live skeletal wireframe overlay.
 * - Accurate rep counting and form coaching.
 * - Clear distinction between Count-Based (Push-ups/Squats) and Time-Based Hold (Wall Sit/Plank).
 * - Front/Back camera toggle.
 */
@Composable
fun CameraPoseWorkoutScreen(
    exerciseId: String = "PUSH_UPS",
    exerciseTitle: String = "Push-ups",
    targetReps: Int = 15,
    targetHoldSeconds: Int = 30,
    session: InterventionSession? = null,
    rewardMinutes: Int = (session?.rewardSeconds ?: 600) / 60,
    onComplete: (earnedSeconds: Int) -> Unit,
    onSwitchToMotionSensor: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }

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

    val isHoldExercise = remember(exerciseId) {
        when (exerciseId.uppercase()) {
            "PUSH_UPS", "PUSHUPS",
            "SQUATS", "BODYWEIGHT_SQUATS",
            "LUNGES", "ALTERNATING_LUNGES",
            "JUMPING_JACKS",
            "HIGH_KNEES",
            "CALF_RAISES", "CALF_RAISE",
            "SIT_TO_STAND", "SIT_STAND", "CHAIR_STAND" -> false
            else -> true
        }
    }

    // Active Validator instance
    val cameraPoseValidator = remember(exerciseId) {
        CameraPoseValidator(
            exerciseId = exerciseId,
            targetReps = targetReps,
            targetHoldSeconds = kotlin.math.max(30, targetHoldSeconds)
        )
    }

    LaunchedEffect(session) {
        if (session != null) {
            cameraPoseValidator.startValidation(session) { result ->
                if (result is ValidationResult.Completed) {
                    onComplete(session.rewardSeconds)
                }
            }
        }
    }

    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }

    var classificationResult by remember {
        mutableStateOf(
            PoseClassificationResult(
                currentReps = 0,
                targetReps = targetReps,
                isHolding = false,
                holdSeconds = 0,
                targetHoldSeconds = targetHoldSeconds,
                feedbackMessage = if (isHoldExercise) "Hold position in camera view" else "Ready! Start your reps",
                isCompleted = false
            )
        )
    }

    // Timer state
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val start = SystemClock.elapsedRealtime()
        while (true) {
            delay(1000L)
            elapsedSeconds = ((SystemClock.elapsedRealtime() - start) / 1000L).toInt()
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var activeAnalyzer by remember { mutableStateOf<CameraPoseAnalyzer?>(null) }

    // Strict Lifecycle Shutdown Observer
    DisposableEffect(lifecycleOwner) {
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
            // 1. Live Camera Preview with COMPATIBLE (TextureView) for floating overlay support
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

                            val result = cameraPoseValidator.onPoseReceived(pose)
                            if (result != null) {
                                classificationResult = result
                                if (result.isCompleted && session == null) {
                                    onComplete(rewardMinutes * 60)
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
                    // Re-bind when user toggles camera flip
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

            // 2. Real-Time Skeletal Wireframe Canvas Overlay
            if (imageWidth > 0 && imageHeight > 0) {
                PoseSkeletalCanvas(
                    pose = currentPose,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT,
                    isActionActive = classificationResult.isHolding || classificationResult.currentReps > 0,
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

        val isActiveNow = classificationResult.isHolding || classificationResult.currentReps > 0

        // =========================================================================
        // 3. TOP OVERLAY: HEADER, TIMER, FLIP CAMERA & CLOSE BUTTON
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        cameraPoseValidator.onUserCancelled()
                        onDismiss()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Exercise Title + High-Visibility Green Metric Subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = exerciseTitle,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isHoldExercise) "${classificationResult.holdSeconds}s / ${targetHoldSeconds}s HOLD" else "${classificationResult.currentReps} / ${targetReps} REPS",
                    color = if (isActiveNow) Color(0xFF22C55E) else Color(0xFF38BDF8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Camera Flip & Timer Badge Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Flip Camera Button
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🔄", fontSize = 15.sp)
                    }
                }

                // Timer Badge
                val mins = elapsedSeconds / 60
                val secs = elapsedSeconds % 60
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "${mins}:${if (secs < 10) "0$secs" else secs}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // =========================================================================
        // 4. LIVE COACHING & FEEDBACK BANNER
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isActiveNow) Color(0xFF064E3B).copy(alpha = 0.92f) else Color(0xFF0F172A).copy(alpha = 0.88f),
            border = BorderStroke(1.5.dp, if (isActiveNow) Color(0xFF22C55E) else Color(0xFF0284C7).copy(alpha = 0.7f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (classificationResult.isCompleted) "🎉" else if (isActiveNow) "🟢" else "💡", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = classificationResult.feedbackMessage,
                    color = if (classificationResult.isCompleted || isActiveNow) Color(0xFF4ADE80) else Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // =========================================================================
        // 5. BOTTOM OVERLAY: LARGE GLOWING GREEN METRIC BADGE & FINISH BUTTON
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Glowing Circular Metric Counter (Reps or Hold Seconds)
            Surface(
                shape = CircleShape,
                color = if (isActiveNow) Color(0xFF059669) else Color(0xFF0284C7),
                border = BorderStroke(4.dp, if (isActiveNow) Color(0xFF4ADE80) else Color(0xFF38BDF8)),
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
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isHoldExercise) (if (isActiveNow) "HOLDING" else "HOLD") else (if (isActiveNow) "COUNTING" else "REPS"),
                        color = if (isActiveNow) Color(0xFFDCFCE7) else Color(0xFFBAE6FD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Button: Finish & Claim Time
            val isReadyToClaim = classificationResult.isCompleted ||
                    (!isHoldExercise && classificationResult.currentReps >= targetReps) ||
                    (isHoldExercise && classificationResult.holdSeconds >= targetHoldSeconds)

            Button(
                onClick = {
                    onComplete(rewardMinutes * 60)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isReadyToClaim) Color(0xFF059669) else Color(0xFF0284C7)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isReadyToClaim) "🎉 Claim ${rewardMinutes}m Screen Time" else "Claim Screen Time (+${rewardMinutes}m)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

package com.digitaldiscipline.spike.overlay

import android.content.Context
import android.os.CountDownTimer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.behaviour.TriggerReflection
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.intervention.adaptive.HelpfulnessFeedback
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.intervention.model.ValidationType
import com.digitaldiscipline.spike.intervention.audio.MeditationSoundPlayer
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.security.ParentPinManager
import com.digitaldiscipline.spike.security.PinVerificationResult
import com.digitaldiscipline.spike.ui.vision.CameraPoseWorkoutScreen
import com.digitaldiscipline.spike.ui.illustrations.ExerciseIllustration
import com.digitaldiscipline.spike.ui.challenges.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ActiveInterventionTab {
    INTENTIONAL_PAUSE,
    STRICT_BLOCK,
    PAUSE_10S,
    BREATHING_30S,
    CAMERA_POSE_WORKOUT,
    COGNITIVE_CHALLENGE,
    CHALLENGE_COMPLETED,
    PARENT_PIN_OVERRIDE
}

@Composable
fun InterventionOverlayContent(
    context: Context,
    targetPackage: String,
    targetAppName: String,
    unlockDurationSeconds: Int,
    attemptNumber: Int = 1,
    ruleMode: RuleMode = RuleMode.EARN,
    pauseDurationSeconds: Int = 10,
    breathingDurationSeconds: Int = 30,
    squatsTargetCount: Int = 10,
    pinManager: ParentPinManager,
    shouldSampleFeedback: Boolean = false,
    onFeedbackSubmitted: ((HelpfulnessFeedback) -> Unit)? = null,
    onComplete: (durationSeconds: Int) -> Unit,
    onExitHome: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Initial Tab
    val initialTab = when (ruleMode) {
        RuleMode.BLOCK -> ActiveInterventionTab.STRICT_BLOCK
        RuleMode.DELAY -> ActiveInterventionTab.INTENTIONAL_PAUSE
        RuleMode.EARN -> ActiveInterventionTab.INTENTIONAL_PAUSE
        RuleMode.ALLOW -> ActiveInterventionTab.PAUSE_10S
    }

    var activeTab by remember { mutableStateOf(initialTab) }
    var selectedReflection by remember { mutableStateOf<TriggerReflection?>(null) }

    // Intervention Catalog & Card Swiping State (filtered by user enabled preferences)
    val preferencesManager = remember {
        try {
            com.digitaldiscipline.spike.DigitalDisciplineApp.instance.preferencesManager
        } catch (_: Exception) {
            null
        }
    }
    val enabledCategories by preferencesManager?.enabledCategoriesFlow?.collectAsState(initial = emptySet())
        ?: remember { mutableStateOf(emptySet<String>()) }
    val enabledInterventions by preferencesManager?.enabledInterventionsFlow?.collectAsState(initial = emptySet())
        ?: remember { mutableStateOf(emptySet<String>()) }

    val catalogList = remember(enabledCategories, enabledInterventions) {
        val all = InterventionCatalog.getAllInterventions()
        val filtered = all.filter { def ->
            (enabledInterventions.isEmpty() || enabledInterventions.contains(def.id)) &&
            (enabledCategories.isEmpty() || enabledCategories.contains(def.category.name))
        }
        if (filtered.isNotEmpty()) filtered else all
    }
    var currentCardIndex by remember(catalogList) {
        mutableIntStateOf(if (catalogList.isNotEmpty()) (0 until catalogList.size).random() else 0)
    }
    val currentIntervention = if (catalogList.isNotEmpty()) catalogList[currentCardIndex % catalogList.size] else InterventionCatalog.getDefaultIntervention()
    var selectedIntervention by remember { mutableStateOf<InterventionDefinition?>(null) }

    // Swipe Animation
    val swipeOffsetX = remember { Animatable(0f) }

    // Timer States
    var currentPauseSeconds by remember { mutableIntStateOf(pauseDurationSeconds) }
    var pauseSecondsRemaining by remember { mutableIntStateOf(pauseDurationSeconds) }
    var currentBreathingSeconds by remember { mutableIntStateOf(breathingDurationSeconds) }
    var breathingPhase by remember { mutableStateOf("Inhale (4s)") }
    var breathingSecondsLeft by remember { mutableIntStateOf(breathingDurationSeconds) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var earnedSecondsTotal by remember { mutableIntStateOf(unlockDurationSeconds) }
    var submittedFeedback by remember { mutableStateOf<HelpfulnessFeedback?>(null) }

    // Breathing Animation Scale
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    var isMeditationAudioEnabled by remember { mutableStateOf(true) }

    DisposableEffect(activeTab, isMeditationAudioEnabled) {
        if (isMeditationAudioEnabled && (activeTab == ActiveInterventionTab.BREATHING_30S || activeTab == ActiveInterventionTab.PAUSE_10S)) {
            MeditationSoundPlayer.play()
        } else {
            MeditationSoundPlayer.stop()
        }
        onDispose {
            MeditationSoundPlayer.stop()
        }
    }

    // Helper to start an intervention
    fun executeIntervention(def: InterventionDefinition, is10sTest: Boolean = false) {
        selectedIntervention = def
        if (is10sTest) {
            currentPauseSeconds = 10
            activeTab = ActiveInterventionTab.PAUSE_10S
            return
        }

        val isCameraEligible = def.category == InterventionCategory.MOVEMENT ||
                def.category == InterventionCategory.UPPER_BODY ||
                def.category == InterventionCategory.YOGA_MOBILITY ||
                def.id.contains("PUSH") || def.id.contains("SQUAT") ||
                def.id.contains("LUNGE") || def.id.contains("PLANK") ||
                def.id == "STAND_UP" || def.id.contains("SHAKE") ||
                def.id.contains("TREE") || def.id.contains("MOUNTAIN") ||
                def.id.contains("CALF") || def.id.contains("JUMPING_JACKS") ||
                def.id.contains("HIGH_KNEES") || def.id.contains("WALL_SIT")

        val isCognitiveOrInteractive = def.category == InterventionCategory.COGNITIVE ||
                def.category == InterventionCategory.CREATIVE_FLOW ||
                def.category == InterventionCategory.MINDFUL_PERSPECTIVE ||
                def.category == InterventionCategory.PHYSICAL_RESET ||
                def.validationType == ValidationType.INTERACTION_VALIDATED ||
                def.id == "SCAVENGER_HUNT" || def.id == "IMAGE_PUZZLE_3X3" ||
                def.id == "ZEN_ENSO_CANVAS" || def.id == "HAND_MUDRA_DEXTERITY" ||
                def.id == "DIVERGENT_THINKING" || def.id == "HAIKU_CRAFTER" ||
                def.id == "BINAURAL_SOUNDSCAPE" || def.id == "FUTURE_SELF_CAPSULE" ||
                def.id == "STOIC_TAROT_DECIDER" || def.id.contains("STROOP") ||
                def.id.contains("MATH") || def.id.contains("MEMORY") ||
                def.id.contains("PATTERN") || def.id.contains("TAP") ||
                def.id.contains("REACTION") || def.id.contains("RECALL") ||
                def.id.contains("READING") || def.id.contains("WRITING") ||
                def.id.contains("PUZZLE") || def.id.contains("SCAVENGER")

        if (isCameraEligible) {
            activeTab = ActiveInterventionTab.CAMERA_POSE_WORKOUT
        } else if (isCognitiveOrInteractive) {
            activeTab = ActiveInterventionTab.COGNITIVE_CHALLENGE
        } else if (def.category == InterventionCategory.BREATHING || def.id.contains("BREATH")) {
            currentBreathingSeconds = if (def.defaultDurationSeconds > 0) kotlin.math.max(30, def.defaultDurationSeconds) else 30
            activeTab = ActiveInterventionTab.BREATHING_30S
        } else {
            currentPauseSeconds = if (def.defaultDurationSeconds > 0) kotlin.math.max(30, def.defaultDurationSeconds) else 30
            activeTab = ActiveInterventionTab.PAUSE_10S
        }
    }

    // Handle Timers when entering PAUSE or BREATHING tab
    LaunchedEffect(activeTab) {
        if (activeTab == ActiveInterventionTab.PAUSE_10S) {
            pauseSecondsRemaining = currentPauseSeconds
            val timer = object : CountDownTimer(currentPauseSeconds * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    pauseSecondsRemaining = ((millisUntilFinished / 1000) + 1).toInt()
                }

                override fun onFinish() {
                    pauseSecondsRemaining = 0
                    EventLogger.log("OVERLAY", targetPackage, "INTERVENTION_COMPLETED", details = "Type: PAUSE | Mode: $ruleMode | Intervention: ${selectedIntervention?.id}")
                    earnedSecondsTotal = if (unlockDurationSeconds >= 300) unlockDurationSeconds else 300
                    activeTab = ActiveInterventionTab.CHALLENGE_COMPLETED
                }
            }
            timer.start()
        } else if (activeTab == ActiveInterventionTab.BREATHING_30S) {
            breathingSecondsLeft = currentBreathingSeconds
            val timer = object : CountDownTimer(currentBreathingSeconds * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    val sec = (millisUntilFinished / 1000).toInt()
                    breathingSecondsLeft = sec
                    val phaseIndex = (currentBreathingSeconds - sec) % 16
                    breathingPhase = when (phaseIndex) {
                        in 0..3 -> "🌬️ Inhale deeply (4s)"
                        in 4..7 -> "⏸️ Hold breath (4s)"
                        in 8..11 -> "💨 Exhale slowly (4s)"
                        else -> "⏸️ Hold empty (4s)"
                    }
                }

                override fun onFinish() {
                    EventLogger.log("OVERLAY", targetPackage, "INTERVENTION_COMPLETED", details = "Type: BREATHING | Mode: $ruleMode | Intervention: ${selectedIntervention?.id}")
                    earnedSecondsTotal = if (unlockDurationSeconds >= 300) unlockDurationSeconds else 300
                    activeTab = ActiveInterventionTab.CHALLENGE_COMPLETED
                }
            }
            timer.start()
        }
    }

    // Full-screen Camera Pose Workout Mode
    if (activeTab == ActiveInterventionTab.CAMERA_POSE_WORKOUT) {
        val exerciseDef = selectedIntervention ?: currentIntervention
        CameraPoseWorkoutScreen(
            exerciseId = exerciseDef.id,
            exerciseTitle = exerciseDef.title,
            targetReps = exerciseDef.defaultReps.takeIf { it > 0 } ?: 15,
            targetHoldSeconds = exerciseDef.defaultDurationSeconds.takeIf { it > 0 } ?: 30,
            rewardMinutes = kotlin.math.max(5, unlockDurationSeconds / 60),
            onComplete = { earnedSecs ->
                val finalEarned = if (earnedSecs >= 300) earnedSecs else 300
                EventLogger.log("OVERLAY", targetPackage, "INTERVENTION_COMPLETED", details = "Type: CAMERA_POSE | Exercise: ${exerciseDef.id} | Earned: $finalEarned")
                earnedSecondsTotal = finalEarned
                activeTab = ActiveInterventionTab.CHALLENGE_COMPLETED
            },
            onSwitchToMotionSensor = {
                currentPauseSeconds = 30
                activeTab = ActiveInterventionTab.PAUSE_10S
            },
            onDismiss = {
                activeTab = ActiveInterventionTab.INTENTIONAL_PAUSE
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF80A0F1D)) // 97% opacity dark slate
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (ruleMode == RuleMode.BLOCK) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (ruleMode == RuleMode.BLOCK) "⛔" else "🌱", fontSize = 17.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (ruleMode == RuleMode.BLOCK) "PARENT PROTECTION" else "DIGITAL DISCIPLINE",
                    color = if (ruleMode == RuleMode.BLOCK) Color(0xFFEF4444) else Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeTab) {
                ActiveInterventionTab.INTENTIONAL_PAUSE -> {
                    // 1. Optional Reflection Row
                    val reflections = listOf(
                        TriggerReflection.INTENTIONAL_USE,
                        TriggerReflection.BOREDOM,
                        TriggerReflection.AVOIDANCE,
                        TriggerReflection.HABIT
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reflections.forEach { reflection ->
                            val isSelected = selectedReflection == reflection
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedReflection = if (isSelected) null else reflection
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (reflection) {
                                            TriggerReflection.INTENTIONAL_USE -> "Intentional"
                                            TriggerReflection.BOREDOM -> "Bored"
                                            TriggerReflection.AVOIDANCE -> "Avoidance"
                                            TriggerReflection.HABIT -> "Habit"
                                            TriggerReflection.SKIPPED -> "Skip"
                                            else -> "Other"
                                        },
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // =========================================================================
                    // 2. TINDER-STYLE SINGLE INTERVENTION SWIPE CARD
                    // =========================================================================
                    val isCameraCard = currentIntervention.category == InterventionCategory.MOVEMENT ||
                            currentIntervention.category == InterventionCategory.UPPER_BODY ||
                            currentIntervention.category == InterventionCategory.YOGA_MOBILITY ||
                            currentIntervention.id.contains("PUSH") || currentIntervention.id.contains("SQUAT") ||
                            currentIntervention.id == "STAND_UP" || currentIntervention.id.contains("SHAKE")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 380.dp, max = 450.dp)
                            .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                            .graphicsLayer {
                                rotationZ = (swipeOffsetX.value / 400f) * 10f
                            }
                            .pointerInput(currentCardIndex) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            swipeOffsetX.snapTo(swipeOffsetX.value + dragAmount.x)
                                        }
                                    },
                                    onDragEnd = {
                                        if (swipeOffsetX.value > 80f) {
                                            // Swipe Right -> PREVIOUS Card
                                            coroutineScope.launch {
                                                swipeOffsetX.animateTo(400f, tween(160))
                                                currentCardIndex = if (currentCardIndex > 0) currentCardIndex - 1 else catalogList.size - 1
                                                swipeOffsetX.snapTo(0f)
                                            }
                                        } else if (swipeOffsetX.value < -80f) {
                                            // Swipe Left -> NEXT Card
                                            coroutineScope.launch {
                                                swipeOffsetX.animateTo(-400f, tween(160))
                                                currentCardIndex = (currentCardIndex + 1) % catalogList.size
                                                swipeOffsetX.snapTo(0f)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                swipeOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                            }
                                        }
                                    }
                                )
                            },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.8f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Swipe indicator watermark overlays
                            if (swipeOffsetX.value > 30f) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "◀ PREVIOUS",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            } else if (swipeOffsetX.value < -30f) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "NEXT ▶",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E293B)
                                    ) {
                                        Text(
                                            text = if (isCameraCard) "📹 AI CAMERA POSE" else currentIntervention.category.name.replace("_", " "),
                                            color = if (isCameraCard) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.size(28.dp).clickable {
                                                currentCardIndex = if (currentCardIndex > 0) currentCardIndex - 1 else catalogList.size - 1
                                            }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("◀", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Text(
                                            text = "${(currentCardIndex % catalogList.size) + 1} / ${catalogList.size}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.size(28.dp).clickable {
                                                currentCardIndex = (currentCardIndex + 1) % catalogList.size
                                            }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("▶", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Center Content with Animated Vector Illustration
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    ExerciseIllustration(
                                        exerciseId = currentIntervention.id,
                                        modifier = Modifier.size(175.dp, 96.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "${currentIntervention.iconEmoji} ${currentIntervention.title}",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentIntervention.description,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp,
                                        maxLines = 3
                                    )
                                }

                                // Bottom Target Pill
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val targetPillText = when {
                                            currentIntervention.defaultReps > 0 -> "🎯 ${currentIntervention.defaultReps} Reps (Camera AI)"
                                            isCameraCard -> "⏳ ${currentIntervention.defaultDurationSeconds.takeIf { it > 0 } ?: 30}s Hold (Camera AI)"
                                            currentIntervention.category == InterventionCategory.COGNITIVE || currentIntervention.validationType == ValidationType.INTERACTION_VALIDATED -> "🧠 Mind Challenge"
                                            currentIntervention.category == InterventionCategory.BREATHING -> "🫁 ${currentIntervention.defaultDurationSeconds.takeIf { it > 0 } ?: 30}s Breath Reset"
                                            currentIntervention.category == InterventionCategory.MEDITATION -> "🧘 ${currentIntervention.defaultDurationSeconds.takeIf { it > 0 } ?: 30}s Meditation"
                                            else -> "⏱️ ${currentIntervention.defaultDurationSeconds.takeIf { it > 0 } ?: 30}s Mindful Reset"
                                        }
                                        Text(
                                            text = targetPillText,
                                            color = Color(0xFF34D399),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("•", color = Color(0xFF64748B))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("+${unlockDurationSeconds / 60}m", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Controls: Prev, Next, Start Challenge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button 1: Previous Challenge
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    swipeOffsetX.animateTo(300f, tween(120))
                                    currentCardIndex = if (currentCardIndex > 0) currentCardIndex - 1 else catalogList.size - 1
                                    swipeOffsetX.snapTo(0f)
                                }
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("◀ Prev", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // Button 2: Next Challenge
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    swipeOffsetX.animateTo(-300f, tween(120))
                                    currentCardIndex = (currentCardIndex + 1) % catalogList.size
                                    swipeOffsetX.snapTo(0f)
                                }
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Next ▶", color = Color(0xFFCBD5E1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // Button 3: Start Challenge
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    swipeOffsetX.animateTo(400f, tween(150))
                                    executeIntervention(currentIntervention)
                                    swipeOffsetX.snapTo(0f)
                                }
                            },
                            modifier = Modifier.weight(1.5f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isCameraCard) Color(0xFF2563EB) else Color(0xFF059669)),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(if (isCameraCard) "📹 Start AI" else "▶ Start", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Swipe left / right or tap Prev / Next to browse challenges",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                ActiveInterventionTab.STRICT_BLOCK -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDC2626), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C131D)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔒 Strict Block Active",
                                color = Color(0xFFF87171),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF450A0A))
                                    .border(2.dp, Color(0xFFEF4444), CircleShape)
                            ) {
                                Text("⛔", fontSize = 36.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "$targetAppName is locked by discipline schedule or parent policy.",
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    activeTab = ActiveInterventionTab.PARENT_PIN_OVERRIDE
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Text("🔑 PARENT PIN OVERRIDE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                ActiveInterventionTab.PAUSE_10S -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$targetAppName can wait.",
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mindful Pause (${currentPauseSeconds}s)",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, Color(0xFF38BDF8), CircleShape)
                            ) {
                                Text(
                                    text = "$pauseSecondsRemaining",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Notice your urge to scroll before continuing.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                ActiveInterventionTab.BREATHING_30S -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$targetAppName can wait.",
                                color = Color(0xFF5EEAD4),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Box Breathing (${currentBreathingSeconds}s)",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(breathScale)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF0D9488), Color(0xFF115E59))
                                        )
                                    )
                            ) {
                                Text(
                                    text = "$breathingSecondsLeft",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = breathingPhase,
                                color = Color(0xFF5EEAD4),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable {
                                    isMeditationAudioEnabled = !isMeditationAudioEnabled
                                }
                            ) {
                                Text(
                                    text = if (isMeditationAudioEnabled) "🎵 Music: ON" else "🔇 Music: OFF",
                                    color = if (isMeditationAudioEnabled) Color(0xFF5EEAD4) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                ActiveInterventionTab.COGNITIVE_CHALLENGE -> {
                    val def = selectedIntervention ?: currentIntervention
                    CognitiveChallengeCard(
                        intervention = def,
                        targetAppName = targetAppName,
                        onSuccess = {
                            EventLogger.log("OVERLAY", targetPackage, "INTERVENTION_COMPLETED", details = "Type: COGNITIVE | Intervention: ${def.id}")
                            earnedSecondsTotal = if (unlockDurationSeconds >= 300) unlockDurationSeconds else 300
                            activeTab = ActiveInterventionTab.CHALLENGE_COMPLETED
                        }
                    )
                }

                ActiveInterventionTab.CHALLENGE_COMPLETED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Done. You beat the impulse.",
                                color = Color(0xFF34D399),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val earnedMins = earnedSecondsTotal / 60
                            val earnedText = if (earnedMins > 0) {
                                "+$earnedMins minute${if (earnedMins != 1) "s" else ""} deposited to your wallet"
                            } else {
                                "+$earnedSecondsTotal seconds deposited to your wallet"
                            }
                            Text(
                                text = earnedText,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (shouldSampleFeedback) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (submittedFeedback != null) "Thanks for the feedback." else "Did that help?",
                                    color = if (submittedFeedback != null) Color(0xFF34D399) else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (submittedFeedback == null) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            onClick = {
                                                submittedFeedback = HelpfulnessFeedback.HELPED
                                                onFeedbackSubmitted?.invoke(HelpfulnessFeedback.HELPED)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("YES", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Surface(
                                            onClick = {
                                                submittedFeedback = HelpfulnessFeedback.NEUTRAL
                                                onFeedbackSubmitted?.invoke(HelpfulnessFeedback.NEUTRAL)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("A LITTLE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Surface(
                                            onClick = {
                                                submittedFeedback = HelpfulnessFeedback.DID_NOT_HELP
                                                onFeedbackSubmitted?.invoke(HelpfulnessFeedback.DID_NOT_HELP)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("NOT REALLY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    onComplete(earnedSecondsTotal)
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("CONTINUE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    onComplete(earnedSecondsTotal)
                                    onExitHome()
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text("DONE FOR NOW (SAVE TIME)", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                ActiveInterventionTab.PARENT_PIN_OVERRIDE -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔑 Admin PIN Override",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Enter 4-Digit Admin PIN (Default: 1234)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // PIN Dots Display
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                (0..3).forEach { index ->
                                    val isFilled = index < enteredPin.length
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (isFilled) Color(0xFF38BDF8) else Color(0xFF334155))
                                            .border(1.dp, Color(0xFF64748B), CircleShape)
                                    )
                                }
                            }

                            if (pinError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val keypad = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("Cancel", "0", "⌫")
                            )

                            keypad.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    row.forEach { digit ->
                                        Surface(
                                            onClick = {
                                                when (digit) {
                                                    "Cancel" -> {
                                                        enteredPin = ""
                                                        pinError = null
                                                        activeTab = initialTab
                                                    }
                                                    "⌫" -> {
                                                        if (enteredPin.isNotEmpty()) {
                                                            enteredPin = enteredPin.dropLast(1)
                                                            pinError = null
                                                        }
                                                    }
                                                    else -> {
                                                        if (enteredPin.length < 4) {
                                                            val newPin = enteredPin + digit
                                                            enteredPin = newPin
                                                            pinError = null

                                                            if (newPin.length == 4) {
                                                                val result = pinManager.verifyPin(newPin)
                                                                when (result) {
                                                                    is PinVerificationResult.Success -> {
                                                                        EventLogger.log("SECURITY", targetPackage, "PARENT_OVERRIDE_SUCCESS")
                                                                        onComplete(900)
                                                                    }
                                                                    is PinVerificationResult.IncorrectPin -> {
                                                                        pinError = "Incorrect PIN (${result.attemptsRemaining} left)"
                                                                        enteredPin = ""
                                                                    }
                                                                    is PinVerificationResult.LockedOut -> {
                                                                        pinError = "Locked for ${result.remainingLockoutSeconds}s"
                                                                        enteredPin = ""
                                                                    }
                                                                    else -> {
                                                                        pinError = "PIN not set or unavailable"
                                                                        enteredPin = ""
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.size(width = 68.dp, height = 44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = digit,
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Options: Parent Override & Exit to Home
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ruleMode != RuleMode.BLOCK && activeTab != ActiveInterventionTab.PARENT_PIN_OVERRIDE) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.clickable {
                            activeTab = ActiveInterventionTab.PARENT_PIN_OVERRIDE
                        }
                    ) {
                        Text(
                            text = "🔑 Parent PIN",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable {
                        EventLogger.log("OVERLAY", targetPackage, "EXIT_TO_HOME")
                        onExitHome()
                    }
                ) {
                    Text(
                        text = "Exit to Home ⌂",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COGNITIVE MICRO-CHALLENGES
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun CognitiveChallengeCard(
    intervention: InterventionDefinition,
    targetAppName: String,
    onSuccess: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$targetAppName can wait.",
                color = Color(0xFF38BDF8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            when (intervention.id) {
                "IMAGE_PUZZLE_3X3" -> ImageTilePuzzleGame(onSuccess = onSuccess)
                "HANGMAN_CLASSIC" -> HangmanWordGame(onSuccess = onSuccess)
                "ZEN_ENSO_CANVAS" -> ZenCanvasEnsoGame(onSuccess = onSuccess)
                "SCAVENGER_HUNT" -> RealWorldScavengerGame(onSuccess = onSuccess)
                "HAND_MUDRA_DEXTERITY" -> HandMudraDexterityGame(onSuccess = onSuccess)
                "DIVERGENT_THINKING" -> DivergentThinkingGame(onSuccess = onSuccess)
                "HAIKU_CRAFTER" -> HaikuCrafterGame(onSuccess = onSuccess)
                "BINAURAL_SOUNDSCAPE" -> BinauralSoundscapeGame(onSuccess = onSuccess)
                "FUTURE_SELF_CAPSULE" -> FutureSelfCapsuleGame(onSuccess = onSuccess)
                "STOIC_TAROT_DECIDER" -> StoicTarotDeciderGame(onSuccess = onSuccess)
                "STROOP_TEST" -> StroopChallengeGame(onSuccess = onSuccess)
                "MATH_SPRINT", "SIMPLE_MATH" -> MathSprintGame(onSuccess = onSuccess)
                "MEMORY_MATRIX", "MEMORY_SEQUENCE" -> MemoryMatrixGame(onSuccess = onSuccess)
                "MINDFUL_READING" -> MindfulReadingGame(onSuccess = onSuccess)
                "INTENTIONAL_WRITING" -> IntentionalityFrictionGame(targetAppName = targetAppName, onSuccess = onSuccess)
                "PATTERN_MATCH" -> PatternMatchGame(onSuccess = onSuccess)
                "TAP_SEQUENCE" -> TapSequenceGame(onSuccess = onSuccess)
                "REACTION_TEST" -> ReactionTestGame(onSuccess = onSuccess)
                "QUICK_RECALL" -> QuickRecallGame(onSuccess = onSuccess)
                else -> ImageTilePuzzleGame(onSuccess = onSuccess)
            }
        }
    }
}

@Composable
private fun PatternMatchGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val symbols = remember { listOf("🦊", "⚡", "💎", "🎯", "🚀", "🌟", "🧩", "🦁", "🔮", "🎨", "🔥", "🍀") }
    val target = remember { symbols.random() }
    val choices = remember {
        val distractors = symbols.filter { it != target }.shuffled().take(3)
        (distractors + target).shuffled()
    }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pattern Match", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Find and tap the matching symbol", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Target Symbol
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(2.dp, if (isSuccess) Color(0xFF34D399) else Color(0xFF38BDF8)),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(target, fontSize = 38.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isError) {
            Text("❌ Incorrect! Tap the exact matching symbol.", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        } else if (isSuccess) {
            Text("✓ Match Confirmed! Unlocking...", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2x2 Choice Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                choices.take(2).forEach { choice ->
                    val isThisCorrect = choice == target
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccess && isThisCorrect) Color(0xFF065F46) else Color(0xFF1E293B),
                        border = BorderStroke(1.5.dp, if (isSuccess && isThisCorrect) Color(0xFF34D399) else Color(0xFF334155)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable(enabled = !isSuccess) {
                                if (isThisCorrect) {
                                    isSuccess = true
                                    isError = false
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(400)
                                        onSuccess()
                                    }
                                } else {
                                    isError = true
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(choice, fontSize = 24.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                choices.drop(2).take(2).forEach { choice ->
                    val isThisCorrect = choice == target
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccess && isThisCorrect) Color(0xFF065F46) else Color(0xFF1E293B),
                        border = BorderStroke(1.5.dp, if (isSuccess && isThisCorrect) Color(0xFF34D399) else Color(0xFF334155)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable(enabled = !isSuccess) {
                                if (isThisCorrect) {
                                    isSuccess = true
                                    isError = false
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(400)
                                        onSuccess()
                                    }
                                } else {
                                    isError = true
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(choice, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MathChallengeGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val a = remember { (3..12).random() }
    val b = remember { (3..12).random() }
    val isAdd = remember { listOf(true, false).random() }
    val correct = remember { if (isAdd) a + b else a + b }
    val questionText = remember { "$a + $b = ?" }
    val choices = remember {
        val set = mutableSetOf(correct)
        while (set.size < 4) {
            val offset = (-4..4).random()
            if (offset != 0) set.add(correct + offset)
        }
        set.toList().shuffled()
    }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Quick Math", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Awaken your rational brain", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.5.dp, if (isSuccess) Color(0xFF34D399) else Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                Text(questionText, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (isError) {
            Text("❌ Incorrect! Try again.", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        } else if (isSuccess) {
            Text("✓ Correct! Unlocking...", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                choices.take(2).forEach { ans ->
                    val isThisCorrect = ans == correct
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccess && isThisCorrect) Color(0xFF065F46) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSuccess && isThisCorrect) Color(0xFF34D399) else Color(0xFF334155)),
                        modifier = Modifier.weight(1f).height(48.dp).clickable(enabled = !isSuccess) {
                            if (isThisCorrect) {
                                isSuccess = true
                                isError = false
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(400)
                                    onSuccess()
                                }
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$ans", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                choices.drop(2).take(2).forEach { ans ->
                    val isThisCorrect = ans == correct
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccess && isThisCorrect) Color(0xFF065F46) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSuccess && isThisCorrect) Color(0xFF34D399) else Color(0xFF334155)),
                        modifier = Modifier.weight(1f).height(48.dp).clickable(enabled = !isSuccess) {
                            if (isThisCorrect) {
                                isSuccess = true
                                isError = false
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(400)
                                    onSuccess()
                                }
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$ans", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TapSequenceGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val numbers = remember { (1..5).toList().shuffled() }
    var nextExpected by remember { mutableIntStateOf(1) }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Speed Tap Sequence", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (isSuccess) "Completed! Unlocking..." else if (nextExpected <= 5) "Tap number $nextExpected in order" else "Completed! ✓",
            color = if (isSuccess) Color(0xFF34D399) else Color(0xFF38BDF8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (isError) {
            Text("❌ Out of order! Resetting to 1.", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            numbers.forEach { num ->
                val isDone = num < nextExpected
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDone) Color(0xFF065F46) else Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, if (isDone) Color(0xFF34D399) else Color(0xFF334155)),
                    modifier = Modifier.weight(1f).height(54.dp).clickable(enabled = !isSuccess) {
                        if (num == nextExpected) {
                            isError = false
                            val next = nextExpected + 1
                            nextExpected = next
                            if (next > 5) {
                                isSuccess = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(400)
                                    onSuccess()
                                }
                            }
                        } else if (!isDone) {
                            isError = true
                            nextExpected = 1
                        }
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isDone) "✓" else "$num",
                            color = if (isDone) Color(0xFF34D399) else Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemorySequenceGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val colors = remember { listOf("🔴", "🔵", "🟢", "🟡") }
    val sequence = remember { listOf(colors.random(), colors.random(), colors.random(), colors.random()) }
    var isMemorizing by remember { mutableStateOf(true) }
    var enteredIndex by remember { mutableIntStateOf(0) }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        isMemorizing = false
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Memory Sequence", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (isSuccess) "✓ Sequence Complete! Unlocking..." else if (isMemorizing) "Memorize the sequence..." else "Tap the sequence in order (${enteredIndex}/4)",
            color = if (isSuccess) Color(0xFF34D399) else Color(0xFF38BDF8),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isMemorizing) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                sequence.forEach { c ->
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF1E293B), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(c, fontSize = 24.sp) }
                    }
                }
            }
        } else {
            if (isError) {
                Text("❌ Incorrect! Sequence reset.", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                colors.forEach { c ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.weight(1f).height(50.dp).clickable(enabled = !isSuccess) {
                            if (c == sequence[enteredIndex]) {
                                isError = false
                                val next = enteredIndex + 1
                                enteredIndex = next
                                if (next == 4) {
                                    isSuccess = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(400)
                                        onSuccess()
                                    }
                                }
                            } else {
                                isError = true
                                enteredIndex = 0
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(c, fontSize = 22.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionTestGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isGreen by remember { mutableStateOf(false) }
    var isTooEarly by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isTooEarly) {
        isGreen = false
        val delayTime = (1500..3200).random().toLong()
        kotlinx.coroutines.delay(delayTime)
        isGreen = true
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Reaction Test", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (isSuccess) "✓ Great Reflexes! Unlocking..." else if (isGreen) "TAP NOW!" else "Wait for Green...",
            color = if (isSuccess || isGreen) Color(0xFF34D399) else Color(0xFFF87171),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSuccess || isGreen) Color(0xFF059669) else Color(0xFF991B1B),
            border = BorderStroke(2.dp, if (isSuccess || isGreen) Color(0xFF34D399) else Color(0xFFEF4444)),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clickable(enabled = !isSuccess) {
                    if (isGreen) {
                        isSuccess = true
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(400)
                            onSuccess()
                        }
                    } else {
                        isTooEarly = !isTooEarly
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isSuccess) "✓ SUCCESS!" else if (isGreen) "⚡ TAP NOW ⚡" else "⏳ WAIT...",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        if (isTooEarly && !isSuccess) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Too early! Wait for green.", color = Color(0xFFF87171), fontSize = 11.sp)
        }
    }
}

@Composable
private fun QuickRecallGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val wordPool = remember { listOf("DISCIPLINE", "FOCUS", "CALM", "BREATHE", "MOMENT", "PURPOSE", "CLARITY", "PAUSE") }
    val shownWords = remember { wordPool.shuffled().take(3) }
    val targetWord = remember { shownWords.random() }
    val distractors = remember { wordPool.filter { !shownWords.contains(it) }.shuffled().take(3) }
    val choices = remember { (distractors + targetWord).shuffled() }
    var isMemorizing by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        isMemorizing = false
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Word Recall", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (isSuccess) "✓ Correct Word Recalled! Unlocking..." else if (isMemorizing) "Memorize these 3 words (3s)..." else "Which word was shown?",
            color = if (isSuccess) Color(0xFF34D399) else Color(0xFF38BDF8),
            fontSize = 12.sp,
            fontWeight = if (isSuccess) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (isMemorizing) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                shownWords.forEach { w ->
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1E293B), border = BorderStroke(1.dp, Color(0xFF38BDF8))) {
                        Text(w, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                }
            }
        } else {
            if (isError) {
                Text("❌ Incorrect word! Look closely next time.", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                choices.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { choice ->
                            val isThisCorrect = choice == targetWord
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSuccess && isThisCorrect) Color(0xFF065F46) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSuccess && isThisCorrect) Color(0xFF34D399) else Color(0xFF334155)),
                                modifier = Modifier.weight(1f).height(44.dp).clickable(enabled = !isSuccess) {
                                    if (isThisCorrect) {
                                        isSuccess = true
                                        isError = false
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(400)
                                            onSuccess()
                                        }
                                    } else {
                                        isError = true
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isSuccess && isThisCorrect) "✓ $choice" else choice,
                                        color = if (isSuccess && isThisCorrect) Color(0xFF34D399) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

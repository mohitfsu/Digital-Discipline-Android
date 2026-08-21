package com.digitaldiscipline.spike.ui.dashboard

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.firstwin.FirstWinStateManager
import com.digitaldiscipline.spike.behaviour.planner.DailyActionItem
import com.digitaldiscipline.spike.data.local.entities.BehaviourType
import com.digitaldiscipline.spike.data.local.entities.GoalCategory
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun DailyActionScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    actionItem: DailyActionItem,
    goalId: String,
    behaviourRepository: BehaviourRepository,
    walletService: EarnedTimeWalletService,
    preferencesManager: PreferencesManager? = null,
    onCompleteAction: (earnedSeconds: Int, startSessionImmediately: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var currentProgress by remember { mutableIntStateOf(0) }
    var secondsElapsed by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var showCameraPoseWorkout by remember { mutableStateOf(false) }

    if (showCameraPoseWorkout) {
        com.digitaldiscipline.spike.ui.vision.CameraPoseWorkoutScreen(
            exerciseId = actionItem.type,
            exerciseTitle = actionItem.title,
            targetReps = actionItem.targetCount,
            rewardMinutes = actionItem.rewardSeconds / 60,
            onComplete = { earnedSec ->
                isCompleted = true
                showCameraPoseWorkout = false
            },
            onDismiss = {
                showCameraPoseWorkout = false
            }
        )
        return
    }

    // Breathing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Timer loop for time-based actions
    LaunchedEffect(isRunning, isCompleted) {
        if (isRunning && !isCompleted) {
            while (true) {
                delay(1000L)
                secondsElapsed++
                if (actionItem.type == BehaviourType.BOX_BREATHING.name || actionItem.type == BehaviourType.MINDFUL_PAUSE.name) {
                    currentProgress = secondsElapsed
                    if (secondsElapsed >= actionItem.estimatedDurationSeconds) {
                        isCompleted = true
                        isRunning = false
                        break
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentAlignment = Alignment.Center
    ) {
        if (!isCompleted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY HABIT ACTION",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    IconButton(onClick = onCancel) {
                        Text("✕", color = Color(0xFF94A3B8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Main Action Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (actionItem.category) {
                                GoalCategory.FITNESS.name -> "💪"
                                GoalCategory.STUDY.name -> "📚"
                                GoalCategory.MINDFULNESS.name -> "🧘"
                                GoalCategory.READING.name -> "📖"
                                else -> "🎯"
                            },
                            fontSize = 36.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = actionItem.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = actionItem.description,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Interactive Component based on action type
                        when (actionItem.type) {
                            BehaviourType.BOX_BREATHING.name, BehaviourType.MINDFUL_PAUSE.name -> {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                    border = BorderStroke(2.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier
                                        .size(140.dp)
                                        .scale(if (isRunning) breathingScale else 1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${(actionItem.estimatedDurationSeconds - secondsElapsed).coerceAtLeast(0)}s",
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                            BehaviourType.STUDY_TIMER.name -> {
                                val remainingM = (actionItem.estimatedDurationSeconds - secondsElapsed).coerceAtLeast(0) / 60
                                val remainingS = (actionItem.estimatedDurationSeconds - secondsElapsed).coerceAtLeast(0) % 60
                                Text(
                                    text = "${if (remainingM < 10) "0$remainingM" else "$remainingM"}:${if (remainingS < 10) "0$remainingS" else "$remainingS"}",
                                    color = Color.White,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            else -> {
                                // Repetition / Count based (e.g. Squats, Pushups, Reading)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$currentProgress",
                                        color = Color(0xFF34D399),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = " / ${actionItem.targetCount} ${actionItem.unit}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Controls
                        when (actionItem.type) {
                            BehaviourType.BOX_BREATHING.name, BehaviourType.MINDFUL_PAUSE.name, BehaviourType.STUDY_TIMER.name -> {
                                Button(
                                    onClick = { isRunning = !isRunning },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(if (isRunning) "PAUSE" else "START TIMER", fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { showCameraPoseWorkout = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📹", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("START CAMERA POSE TRACKING", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                if (currentProgress < actionItem.targetCount) {
                                                    currentProgress++
                                                    if (currentProgress >= actionItem.targetCount) {
                                                        isCompleted = true
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                            modifier = Modifier.weight(1f).height(44.dp)
                                        ) {
                                            Text("+1 ${actionItem.unit.uppercase()}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                currentProgress = actionItem.targetCount
                                                isCompleted = true
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.weight(1f).height(44.dp)
                                        ) {
                                            Text("MARK DONE", color = Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Reward preview note
                Text(
                    text = "🎁 Completing this step earns +${actionItem.rewardSeconds / 60}m Screen Time",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // COMPLETION SCREEN (EARNED REWARD & EXPLICIT CHOICE)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFF10B981))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Step Completed!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "+${actionItem.rewardSeconds / 60} Minutes Screen Time Earned",
                        color = Color(0xFF34D399),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("What would you like to do with your earned time?", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Explicit Choice Buttons: USE NOW vs SAVE FOR LATER
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                // 1. Record Goal Progress
                                behaviourRepository.recordGoalCompletion(
                                    goalId = goalId,
                                    durationSec = actionItem.estimatedDurationSeconds
                                )
                                // 2. Credit Wallet Ledger
                                walletService.earnTime(
                                    amountSeconds = actionItem.rewardSeconds,
                                    source = actionItem.type,
                                    idempotencyKey = "daily_action_${UUID.randomUUID()}"
                                )
                                // 3. Record First-Win State
                                preferencesManager?.let {
                                    FirstWinStateManager.onInterventionCompleted(goalId, actionItem.rewardSeconds, actionItem.title, it)
                                    FirstWinStateManager.onTimeUsed(goalId, actionItem.rewardSeconds, it)
                                }
                            }
                            onCompleteAction(actionItem.rewardSeconds, true)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("USE NOW (Start Session)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                // 1. Record Goal Progress
                                behaviourRepository.recordGoalCompletion(
                                    goalId = goalId,
                                    durationSec = actionItem.estimatedDurationSeconds
                                )
                                // 2. Credit Wallet Ledger
                                walletService.earnTime(
                                    amountSeconds = actionItem.rewardSeconds,
                                    source = actionItem.type,
                                    idempotencyKey = "daily_action_${UUID.randomUUID()}"
                                )
                                // 3. Record First-Win State
                                preferencesManager?.let {
                                    FirstWinStateManager.onInterventionCompleted(goalId, actionItem.rewardSeconds, actionItem.title, it)
                                    FirstWinStateManager.onTimeSaved(goalId, actionItem.rewardSeconds, it)
                                }
                            }
                            onCompleteAction(actionItem.rewardSeconds, false)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("SAVE FOR LATER", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

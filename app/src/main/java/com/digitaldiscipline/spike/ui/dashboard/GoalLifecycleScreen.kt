package com.digitaldiscipline.spike.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleEngine
import com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleSnapshot
import com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState
import com.digitaldiscipline.spike.behaviour.lifecycle.GoalTransitionType

@Composable
fun GoalLifecycleScreen(
    snapshot: GoalLifecycleSnapshot,
    onPauseGoal: () -> Unit,
    onResumeGoal: () -> Unit,
    onCompleteGoal: () -> Unit,
    onChangeGoal: () -> Unit,
    onStartFresh: () -> Unit,
    onViewHistory: () -> Unit,
    onNavigateBack: () -> Unit
) {
    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }

    val scrollState = rememberScrollState()
    var pendingTransition by remember { mutableStateOf<GoalTransitionType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 1. Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "GOAL LIFECYCLE",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Evolve & Manage",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Goal Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, when (snapshot.lifecycleState) {
                GoalLifecycleState.ACTIVE -> Color(0xFF0284C7).copy(alpha = 0.6f)
                GoalLifecycleState.PAUSED -> Color(0xFFEAB308).copy(alpha = 0.6f)
                GoalLifecycleState.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.6f)
                else -> Color(0xFF64748B).copy(alpha = 0.6f)
            })
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (snapshot.activeGoal?.category ?: "PERSONAL").uppercase(),
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (snapshot.lifecycleState) {
                            GoalLifecycleState.ACTIVE -> Color(0xFF064E3B)
                            GoalLifecycleState.PAUSED -> Color(0xFF713F12)
                            GoalLifecycleState.COMPLETED -> Color(0xFF064E3B)
                            else -> Color(0xFF1E293B)
                        }
                    ) {
                        Text(
                            text = snapshot.lifecycleState.name,
                            color = when (snapshot.lifecycleState) {
                                GoalLifecycleState.ACTIVE -> Color(0xFF34D399)
                                GoalLifecycleState.PAUSED -> Color(0xFFFDE047)
                                GoalLifecycleState.COMPLETED -> Color(0xFF34D399)
                                else -> Color(0xFF94A3B8)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = snapshot.activeGoal?.title ?: "Personal Goal",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = snapshot.statusNarrative,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ACTIVE FOR", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${snapshot.daysActiveCount} Days", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("MEANINGFUL DAYS", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${snapshot.meaningfulDaysCount} Days", color = Color(0xFF34D399), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PAUSES", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${snapshot.totalInterventionsCount}", color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Actions Title
        Text(
            text = "LIFECYCLE ACTIONS",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. State-specific Action Buttons
        when (snapshot.lifecycleState) {
            GoalLifecycleState.ACTIVE -> {
                Button(
                    onClick = { pendingTransition = GoalTransitionType.PAUSE },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("PAUSE GOAL", color = Color(0xFFFDE047), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { pendingTransition = GoalTransitionType.COMPLETE },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("COMPLETE THIS GOAL", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            GoalLifecycleState.PAUSED -> {
                Button(
                    onClick = onResumeGoal,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("RESUME GOAL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { pendingTransition = GoalTransitionType.COMPLETE },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("COMPLETE THIS GOAL", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            GoalLifecycleState.COMPLETED -> {
                Button(
                    onClick = onChangeGoal,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("CHOOSE NEXT GOAL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onChangeGoal,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Text("CHANGE GOAL", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { pendingTransition = GoalTransitionType.START_FRESH },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Text("START FRESH", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Goal History Link
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewHistory() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("GOAL HISTORY", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("View previous completed and paused goals", color = Color(0xFF64748B), fontSize = 11.sp)
                }
                Text("View →", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Confirmation Modal
    pendingTransition?.let { transition ->
        val preview = GoalLifecycleEngine.createTransitionPreview(transition, snapshot.activeGoal)
        AlertDialog(
            onDismissRequest = { pendingTransition = null },
            title = { Text(preview.confirmationHeadline, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(preview.confirmationNarrative, color = Color(0xFFCBD5E1), fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("WHAT STAYS:", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    preview.whatStays.forEach {
                        Text("• $it", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trans = pendingTransition
                        pendingTransition = null
                        when (trans) {
                            GoalTransitionType.PAUSE -> onPauseGoal()
                            GoalTransitionType.COMPLETE -> onCompleteGoal()
                            GoalTransitionType.START_FRESH -> onStartFresh()
                            else -> {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("CONFIRM")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTransition = null }) {
                    Text("CANCEL", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

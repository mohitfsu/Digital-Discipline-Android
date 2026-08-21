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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.behaviour.journey.BehaviourJourneySnapshot
import com.digitaldiscipline.spike.behaviour.journey.EventImportance
import com.digitaldiscipline.spike.behaviour.journey.JourneyEventType
import com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState

@Composable
fun SelfJourneyScreen(
    snapshot: BehaviourJourneySnapshot,
    onNavigateToCurrentPlan: () -> Unit,
    onNavigateToGoalHistory: () -> Unit,
    onNavigateToPlanContinuity: () -> Unit,
    onNavigateToGoalLifecycle: () -> Unit,
    onNavigateBack: () -> Unit
) {
    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }

    val scrollState = rememberScrollState()

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
                    text = "MY JOURNEY",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Personal Behaviour Timeline",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Current Chapter Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CURRENT CHAPTER",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (snapshot.currentGoalLifecycleState) {
                            GoalLifecycleState.ACTIVE -> Color(0xFF064E3B)
                            GoalLifecycleState.PAUSED -> Color(0xFF713F12)
                            GoalLifecycleState.COMPLETED -> Color(0xFF064E3B)
                            else -> Color(0xFF1E293B)
                        }
                    ) {
                        Text(
                            text = if (snapshot.currentGoalLifecycleState == GoalLifecycleState.ACTIVE) "Week ${snapshot.currentWeekNumber}" else snapshot.currentGoalLifecycleState.name,
                            color = when (snapshot.currentGoalLifecycleState) {
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
                    text = snapshot.currentGoal?.title ?: "Personal Goal",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Momentum: ${snapshot.habitMomentumScore}/100 • ${snapshot.planHealth.name}",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToPlanContinuity,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("VIEW CURRENT PLAN", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Long-Term Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("GOAL CHAPTERS", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${snapshot.summary.totalGoalChaptersCompleted} Completed", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("ACTIONS", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${snapshot.summary.totalMeaningfulActionsCount} Done", color = Color(0xFF34D399), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TIME SAVED", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("+${snapshot.summary.totalSavedMinutesCount}m", color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. What You've Learned Section
        Text(
            text = "WHAT YOU'VE LEARNED",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        snapshot.summary.topLearnings.forEach { learning ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(learning.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(learning.narrative, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Evidence: ${learning.evidence}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Timeline Section
        Text(
            text = "CHRONOLOGICAL TIMELINE",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (snapshot.timelineEvents.isEmpty()) {
            Text("No milestones recorded yet. Your timeline grows as you build habits.", color = Color(0xFF64748B), fontSize = 12.sp)
        } else {
            snapshot.timelineEvents.forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (event.importance) {
                                EventImportance.MILESTONE -> Color(0xFF0284C7)
                                EventImportance.HIGH -> Color(0xFF059669)
                                else -> Color(0xFF334155)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (event.eventType) {
                                        JourneyEventType.FIRST_WIN -> "★"
                                        JourneyEventType.GOAL_COMPLETED -> "✓"
                                        JourneyEventType.PLAN_REFINED -> "⚡"
                                        JourneyEventType.HABIT_MOMENTUM -> "▲"
                                        JourneyEventType.RECOVERY_DETECTED -> "↺"
                                        else -> "•"
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(event.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(event.dateFormatted, color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(event.shortDescription, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)

                            if (event.supportingMetric != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${event.supportingMetricLabel ?: "Metric"}: ${event.supportingMetric}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Past Goal Chapters Link
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToGoalHistory() },
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
                    Text("Explore detailed records of past goals", color = Color(0xFF64748B), fontSize = 11.sp)
                }
                Text("View →", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 7. Current Direction Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(snapshot.currentDirectionHeadline, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(snapshot.currentDirectionNarrative, color = Color(0xFFCBD5E1), fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (snapshot.currentGoalLifecycleState == GoalLifecycleState.ACTIVE) {
                            onNavigateToPlanContinuity()
                        } else {
                            onNavigateToGoalLifecycle()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(snapshot.currentDirectionActionLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

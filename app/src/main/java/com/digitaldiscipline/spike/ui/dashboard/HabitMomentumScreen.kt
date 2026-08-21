package com.digitaldiscipline.spike.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.behaviour.momentum.HabitDayStatus
import com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumSnapshot

@Composable
fun HabitMomentumScreen(
    snapshot: HabitMomentumSnapshot,
    onNavigateBack: () -> Unit,
    onStartDailyAction: () -> Unit
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

        // Top Navigation Bar
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
                    text = "HABIT FORMATION",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Your 7-Day Momentum",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Momentum Tier Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = snapshot.momentumTier.title.uppercase(),
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "${snapshot.meaningfulDaysCount} / 7 Days",
                            color = Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = snapshot.momentumTier.narrative,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 7-Day Visual Calendar
        Text(
            text = "ROLLING 7-DAY WINDOW",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                snapshot.days.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayLabel,
                            color = if (day.isToday) Color(0xFF38BDF8) else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = if (day.isToday) FontWeight.Black else FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (day.status) {
                                        HabitDayStatus.STRONG -> Color(0xFF065F46)
                                        HabitDayStatus.COMPLETED -> Color(0xFF064E3B)
                                        HabitDayStatus.PARTIAL -> Color(0xFF78350F)
                                        HabitDayStatus.ACTIVE -> Color(0xFF0369A1)
                                        HabitDayStatus.MISSED -> Color(0xFF1E293B)
                                        else -> Color(0xFF0B1120)
                                    }
                                )
                                .border(
                                    width = if (day.isToday) 2.dp else 1.dp,
                                    color = if (day.isToday) Color(0xFF38BDF8) else Color(0xFF334155),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = when (day.status) {
                                    HabitDayStatus.STRONG -> "✓✓"
                                    HabitDayStatus.COMPLETED -> "✓"
                                    HabitDayStatus.PARTIAL -> "◐"
                                    HabitDayStatus.ACTIVE -> if (day.isToday) "●" else "○"
                                    HabitDayStatus.MISSED -> "—"
                                    else -> "•"
                                },
                                color = when (day.status) {
                                    HabitDayStatus.STRONG, HabitDayStatus.COMPLETED -> Color(0xFF34D399)
                                    HabitDayStatus.PARTIAL -> Color(0xFFFBBF24)
                                    HabitDayStatus.ACTIVE -> Color(0xFF38BDF8)
                                    HabitDayStatus.MISSED -> Color(0xFF64748B)
                                    else -> Color(0xFF475569)
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        if (day.isRecovery) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🌱",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Stat Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 1: Meaningful Days
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("CONSISTENCY", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${snapshot.meaningfulDaysCount} / 7", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Days protected", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }

            // Stat 2: Recoveries
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("RECOVERIES", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${snapshot.recoveryCount}", color = Color(0xFF34D399), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Came back strong", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 3: Interventions
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("INTERRUPTIONS", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${snapshot.weekSummary.totalInterventionsCount}", color = Color(0xFF38BDF8), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Habit pauses", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }

            // Stat 4: Earned Time
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TIME EARNED", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("+${snapshot.weekSummary.totalEarnedMinutes}m", color = Color(0xFF10B981), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Screen time banked", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Contextual Insight Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = snapshot.contextualInsight,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Milestones List
        Text(
            text = "FORMATION MILESTONES",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        snapshot.milestones.forEach { milestone ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (milestone.isReached) Color(0xFF064E3B).copy(alpha = 0.2f) else Color(0xFF0F172A)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (milestone.isReached) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF334155)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (milestone.isReached) "✓" else "○",
                        color = if (milestone.isReached) Color(0xFF10B981) else Color(0xFF64748B),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = milestone.title,
                            color = if (milestone.isReached) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = milestone.description,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Primary Action CTA
        if (!snapshot.todayCompleted) {
            Button(
                onClick = onStartDailyAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("DO ONE SMALL THING", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF064E3B).copy(alpha = 0.3f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("YOU'RE DONE FOR TODAY ✓", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

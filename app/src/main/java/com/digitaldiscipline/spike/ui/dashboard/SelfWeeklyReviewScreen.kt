package com.digitaldiscipline.spike.ui.dashboard

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.behaviour.adaptive.PersonalizationRepository
import com.digitaldiscipline.spike.data.local.entities.WeeklyReviewEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SelfWeeklyReviewScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    personalizationRepository: PersonalizationRepository,
    onBack: () -> Unit,
    onNavigateToPlanEdit: () -> Unit
) {
    androidx.activity.compose.BackHandler {
        onBack()
    }

    val weeklyReviewState by personalizationRepository.getLatestWeeklyReviewFlow().collectAsState(initial = null)

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val weekStart = now - (7 * 86400000L)
            personalizationRepository.generateWeeklyReviewSnapshot(weekStart, now)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(20.dp)
    ) {
        val review = weeklyReviewState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WEEKLY REVIEW",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    IconButton(onClick = onBack) {
                        Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your Weekly Discipline",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Here is a summary of how your habits and screen-time interventions performed over the last 7 days.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (review != null) {
                    // Summary Stats Grid Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("7-DAY PERFORMANCE", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Distraction Attempts", color = Color(0xFF64748B), fontSize = 11.sp)
                                    Text("${review.attempts}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Completed", color = Color(0xFF64748B), fontSize = 11.sp)
                                    Text("${review.completed}", color = Color(0xFF34D399), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Interruption Rate", color = Color(0xFF64748B), fontSize = 11.sp)
                                    Text("${review.habitInterruptionRate.toInt()}%", color = Color(0xFF38BDF8), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Earned Screen Time", color = Color(0xFF64748B), fontSize = 11.sp)
                                    Text("${review.earnedSeconds / 60}m", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Used Screen Time", color = Color(0xFF64748B), fontSize = 11.sp)
                                    Text("${review.consumedSeconds / 60}m", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Saved Screen Time", color = Color(0xFF64748B), fontSize = 11.sp)
                                    val saved = ((review.earnedSeconds - review.consumedSeconds) / 60).coerceAtLeast(0)
                                    Text("${saved}m", color = Color(0xFF34D399), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biggest Win Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏆", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("BIGGEST WIN", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = review.biggestWin,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Suggested Next Step Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("SUGGESTED FOCUS FOR NEXT WEEK", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = review.suggestedNextStep,
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Generating your weekly review summary...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("KEEP MY PLAN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onNavigateToPlanEdit,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text("ADJUST MY PLAN", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

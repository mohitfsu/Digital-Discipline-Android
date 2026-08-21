package com.digitaldiscipline.spike.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.analytics.LocalAnalyticsRepository
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.adaptive.PersonalizationRepository
import com.digitaldiscipline.spike.behaviour.intelligence.*
import com.digitaldiscipline.spike.behaviour.planner.DailyActionItem
import com.digitaldiscipline.spike.behaviour.planner.DailyActionPlanner
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun TodayScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    behaviourRepository: BehaviourRepository,
    analyticsRepository: LocalAnalyticsRepository,
    walletService: EarnedTimeWalletService,
    personalizationRepository: PersonalizationRepository,
    experimentRepository: ExperimentRepository,
    preferencesManager: PreferencesManager,
    isA11yActive: Boolean,
    isOverlayActive: Boolean,
    isUsageStatsActive: Boolean = true,
    userDisplayName: String = "",
    onNavigateToPlan: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToWeeklyReview: () -> Unit,
    onNavigateToMomentum: () -> Unit = {},
    onNavigateToPlanContinuity: () -> Unit = {},
    onNavigateToGoalLifecycle: () -> Unit = {},
    onNavigateToJourney: () -> Unit = {},
    onNavigateToInterventions: () -> Unit = {},
    onSwitchToParentMode: () -> Unit
) {
    val goals by behaviourRepository.getAllGoalsFlow().collectAsState(initial = emptyList())
    val triggers by behaviourRepository.getAllTriggersFlow().collectAsState(initial = emptyList())
    val behaviours by behaviourRepository.getAllBehavioursFlow().collectAsState(initial = emptyList())
    val policies by behaviourRepository.getAllPoliciesFlow().collectAsState(initial = emptyList())
    val recentEvents by analyticsRepository.getRecentInterventionEventsFlow(100).collectAsState(initial = emptyList())

    // Wallet States
    val wallet by walletService.getWalletFlow().collectAsState(initial = null)
    val recentTransactions by walletService.getRecentTransactionsFlow().collectAsState(initial = emptyList())
    val activeSession by walletService.getActiveSessionFlow().collectAsState(initial = null)

    // Intervention Catalog Preferences
    val enabledInterventionIds by preferencesManager.enabledInterventionsFlow.collectAsState(initial = emptySet())
    val enabledCategories by preferencesManager.enabledCategoriesFlow.collectAsState(initial = emptySet())

    // First Win State
    val firstWinState by preferencesManager.firstWinStateFlow.collectAsState(initial = "NOT_STARTED")

    // 1-second live ticker for fluid UI countdowns
    var liveTimerTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            liveTimerTick = android.os.SystemClock.elapsedRealtime()
        }
    }

    // Active Action Execution State
    var activeDailyActionItem by remember { mutableStateOf<DailyActionItem?>(null) }
    var isInsightsExpanded by remember { mutableStateOf(false) }

    val primaryGoal = goals.firstOrNull { it.active } ?: goals.firstOrNull()

    // 7-day consistency calculation
    val goalProgressList by if (primaryGoal != null) {
        behaviourRepository.getProgressForGoalFlow(primaryGoal.goalId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val goalProgress = goalProgressList.firstOrNull()
    val activePolicy = policies.firstOrNull()
    val activeBehaviour = behaviours.firstOrNull { it.behaviourId == activePolicy?.replacementBehaviourId } ?: behaviours.firstOrNull()

    val dailyActionPlan = remember(primaryGoal, goalProgress, activeBehaviour, activePolicy) {
        DailyActionPlanner.planDailyActions(primaryGoal, goalProgress, activeBehaviour, activePolicy)
    }

    val habitMomentumSnapshot = remember(recentEvents, primaryGoal, goalProgressList, recentTransactions, firstWinState) {
        com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumEngine.evaluate7DayWindow(
            events = recentEvents,
            goal = primaryGoal,
            progressList = goalProgressList,
            walletTransactions = recentTransactions,
            firstWinCompleted = (firstWinState == "FIRST_WIN_COMPLETED" || firstWinState == "TIME_USED" || firstWinState == "TIME_SAVED")
        )
    }

    val isProtected = isA11yActive && isOverlayActive

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. TOP HEADER & STATUS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DIGITAL DISCIPLINE",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = if (userDisplayName.isNotBlank()) "Hi, $userDisplayName 👋" else "Today",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Custom Interventions Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { onNavigateToInterventions() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            val countLabel = if (enabledInterventionIds.isNotEmpty()) "${enabledInterventionIds.size}" else "35"
                            Text("Friction ($countLabel)", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Settings
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable { onNavigateToPlan() }
                    ) {
                        Text("⚙️", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }

                    // Admin Mode Switch
                    OutlinedButton(
                        onClick = onSwitchToParentMode,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Admin", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 2. PROMINENT PERMISSIONS & PROTECTION SETUP CARD (If anything missing)
            // =========================================================================
            if (!isProtected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A).copy(alpha = 0.5f)),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PROTECTION INACTIVE",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Enable permissions so Digital Discipline can shield your apps:",
                                    color = Color(0xFFFECACA),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Button 1: Accessibility
                        if (!isA11yActive) {
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("1. Enable Accessibility Service", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("→", color = Color.White, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Button 2: Display Over Apps
                        if (!isOverlayActive) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("2. Allow Display Over Other Apps", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("→", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                // Calm Minimalist Active Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🟢", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shield Active & Enforcing", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${triggers.size} Apps Protected", color = Color(0xFF6EE7B7), fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // =========================================================================
            // 3. HERO TIME WALLET CARD
            // =========================================================================
            val availableSecTotal = wallet?.availableSeconds ?: 0
            val availableMins = availableSecTotal / 60
            val availableSecs = availableSecTotal % 60
            val dailyEarnedMins = (wallet?.dailyEarnedSeconds ?: 0) / 60
            val dailyConsumedMins = (wallet?.dailyConsumedSeconds ?: 0) / 60

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱️ EARNED TIME WALLET",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        if (activeSession != null) {
                            val currentElapsed = if (liveTimerTick > 0L) liveTimerTick else android.os.SystemClock.elapsedRealtime()
                            val liveElapsedSeconds = ((currentElapsed - activeSession!!.startedElapsedRealtime) / 1000L).toInt().coerceAtLeast(0)
                            val remainingSec = (activeSession!!.maxAllowedSeconds - liveElapsedSeconds).coerceAtLeast(0)
                            val remM = remainingSec / 60
                            val remS = remainingSec % 60
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF064E3B)
                            ) {
                                Text(
                                    text = "ACTIVE: ${remM}:${if (remS < 10) "0$remS" else remS}",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            val timeText = if (availableSecTotal == 0) "0 min" else if (availableSecs == 0) "$availableMins min" else "${availableMins}m ${availableSecs}s"
                            Text(
                                text = timeText,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Available Screen Time",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("+$dailyEarnedMins min earned", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("-$dailyConsumedMins min used", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                activeDailyActionItem = DailyActionItem(
                                    actionId = "action_quick_test_${System.currentTimeMillis()}",
                                    title = "⚡ 10-Second Quick Pause",
                                    description = "Take a quick 10-second mindful pause to earn 10 minutes of screen time.",
                                    category = BehaviourCategory.MINDFUL.name,
                                    type = BehaviourType.MINDFUL_PAUSE.name,
                                    targetCount = 1,
                                    unit = "seconds",
                                    estimatedDurationSeconds = 10,
                                    rewardSeconds = 600
                                )
                            },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("⚡ 10s Test Challenge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (availableMins > 0) {
                                    val topApp = triggers.firstOrNull()?.packageName ?: "com.google.android.youtube"
                                    coroutineScope.launch(Dispatchers.IO) {
                                        walletService.startOrResumeSession(topApp)
                                    }
                                } else {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        walletService.earnTime(600, "QUICK_TEST", "test_earn_${System.currentTimeMillis()}")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(if (availableMins > 0) "🔓 Unlock App (10m)" else "+ Add 10m Test", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 4. ACTIVE PROTECTION & CUSTOM FRICTION CARD
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ ACTIVE FRICTION & APPS",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Customize →",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToInterventions() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Protected Apps", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Text("${triggers.size} Apps", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Active Friction", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                val activeCount = if (enabledInterventionIds.isNotEmpty()) "${enabledInterventionIds.size}" else "35"
                                Text("$activeCount Types", color = Color(0xFF34D399), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Impulse triggers present positive friction (Fitness, Reading, Breathing, Meditation) to earn screen time.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 5. TODAY'S HABIT CARD
            // =========================================================================
            val completed = dailyActionPlan.completedCount
            val target = dailyActionPlan.dailyTarget
            val progressFraction = (completed.toFloat() / target.toFloat()).coerceIn(0f, 1f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (primaryGoal?.title ?: "DAILY FOCUS").uppercase(),
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "${dailyActionPlan.progressPercentage}% Done",
                            color = Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$completed / $target actions completed today",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF1E293B)
                    )

                    val nextAction = dailyActionPlan.nextAction
                    if (nextAction != null && !dailyActionPlan.isGoalComplete) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { activeDailyActionItem = nextAction },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("▶ Complete Next Action: ${nextAction.title}", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 6. EXPANDABLE INSIGHTS & ANALYTICS HUB (Clean, Uncluttered)
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isInsightsExpanded = !isInsightsExpanded },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MOMENTUM & ANALYTICS",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = if (isInsightsExpanded) "▲ Hide" else "▼ View",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isInsightsExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // 7-day Dot Row
                        Text(
                            text = "7-Day Momentum: ${habitMomentumSnapshot.meaningfulDaysCount} / 7 Days Active",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            habitMomentumSnapshot.days.forEach { day ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (day.status) {
                                                com.digitaldiscipline.spike.behaviour.momentum.HabitDayStatus.STRONG,
                                                com.digitaldiscipline.spike.behaviour.momentum.HabitDayStatus.COMPLETED -> Color(0xFF065F46)
                                                com.digitaldiscipline.spike.behaviour.momentum.HabitDayStatus.PARTIAL -> Color(0xFF78350F)
                                                com.digitaldiscipline.spike.behaviour.momentum.HabitDayStatus.ACTIVE -> Color(0xFF0369A1)
                                                else -> Color(0xFF1E293B)
                                            }
                                        )
                                        .border(
                                            width = if (day.isToday) 2.dp else 1.dp,
                                            color = if (day.isToday) Color(0xFF38BDF8) else Color(0xFF334155),
                                            shape = CircleShape
                                        )
                                ) {
                                    Text(
                                        text = day.dayLabel.take(1),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Shortcuts Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onNavigateToInsights,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🧠 Insights", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToWeeklyReview,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📅 Review", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Active Daily Action Modal / Runner
        if (activeDailyActionItem != null) {
            DailyActionScreen(
                context = context,
                coroutineScope = coroutineScope,
                actionItem = activeDailyActionItem!!,
                goalId = primaryGoal?.goalId ?: "default_goal",
                behaviourRepository = behaviourRepository,
                walletService = walletService,
                preferencesManager = preferencesManager,
                onCompleteAction = { earnedSeconds, startSession ->
                    activeDailyActionItem = null
                },
                onCancel = {
                    activeDailyActionItem = null
                }
            )
        }
    }
}

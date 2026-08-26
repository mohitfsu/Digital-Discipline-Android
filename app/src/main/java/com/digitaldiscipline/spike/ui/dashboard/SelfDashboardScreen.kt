package com.digitaldiscipline.spike.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.analytics.LocalAnalyticsRepository
import com.digitaldiscipline.spike.behaviour.BehaviourInsightsEngine
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.adaptive.AdaptivePlanEngine
import com.digitaldiscipline.spike.behaviour.adaptive.PersonalizationRepository
import com.digitaldiscipline.spike.behaviour.adaptive.PlanHealth
import com.digitaldiscipline.spike.behaviour.intelligence.*
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SelfDashboardScreen(
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
    onBack: () -> Unit = {},
    onSwitchToParentMode: () -> Unit
) {
    androidx.activity.compose.BackHandler {
        onBack()
    }

    var showWeeklyReviewScreen by remember { mutableStateOf(false) }
    var showBehaviourInsightsScreen by remember { mutableStateOf(false) }

    if (showWeeklyReviewScreen) {
        SelfWeeklyReviewScreen(
            context = context,
            coroutineScope = coroutineScope,
            personalizationRepository = personalizationRepository,
            onBack = { showWeeklyReviewScreen = false },
            onNavigateToPlanEdit = { showWeeklyReviewScreen = false }
        )
        return
    }

    if (showBehaviourInsightsScreen) {
        SelfBehaviourInsightsScreen(
            context = context,
            coroutineScope = coroutineScope,
            behaviourRepository = behaviourRepository,
            analyticsRepository = analyticsRepository,
            experimentRepository = experimentRepository,
            onBack = { showBehaviourInsightsScreen = false }
        )
        return
    }

    val goals by behaviourRepository.getAllGoalsFlow().collectAsState(initial = emptyList())
    val triggers by behaviourRepository.getAllTriggersFlow().collectAsState(initial = emptyList())
    val behaviours by behaviourRepository.getAllBehavioursFlow().collectAsState(initial = emptyList())
    val policies by behaviourRepository.getAllPoliciesFlow().collectAsState(initial = emptyList())
    val dailyUsageList by analyticsRepository.getTodayUsageFlow().collectAsState(initial = emptyList())
    val recentEvents by analyticsRepository.getRecentInterventionEventsFlow(100).collectAsState(initial = emptyList())

    // Wallet States
    val wallet by walletService.getWalletFlow().collectAsState(initial = null)
    val recentTransactions by walletService.getRecentTransactionsFlow().collectAsState(initial = emptyList())
    val activeSession by walletService.getActiveSessionFlow().collectAsState(initial = null)

    // Adaptive & Personalization States
    val profile by personalizationRepository.getProfileFlow().collectAsState(initial = null)
    val pendingAdjustment by personalizationRepository.getLatestPendingAdjustmentFlow().collectAsState(initial = null)

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            personalizationRepository.recalculateProfileAndSuggestions()
        }
    }

    val primaryGoal = goals.firstOrNull { it.active } ?: goals.firstOrNull()
    val activeTriggers = triggers.filter { it.active }
    val primaryPolicy = policies.firstOrNull { it.enabled }
    val primaryBehaviour = behaviours.firstOrNull { it.behaviourId == primaryPolicy?.replacementBehaviourId }
        ?: behaviours.firstOrNull()

    val goalProgressList by if (primaryGoal != null) {
        behaviourRepository.getProgressForGoalFlow(primaryGoal.goalId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val todayProgress = goalProgressList.firstOrNull { it.dateString == behaviourRepository.todayString }

    // Dialog state for in-place editing
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showEditTriggersDialog by remember { mutableStateOf(false) }
    var showEditInterventionDialog by remember { mutableStateOf(false) }
    var showEditRewardDialog by remember { mutableStateOf(false) }

    // Deterministic Insights & Intelligence
    val hir = remember(recentEvents) {
        BehaviourInsightsEngine.calculateHabitInterruptionRate(recentEvents)
    }
    val planHealth = remember(recentEvents, recentTransactions) {
        AdaptivePlanEngine.evaluatePlanHealth(recentEvents, recentTransactions, emptyList())
    }
    val momentumResult = remember(recentEvents, primaryGoal, goalProgressList, dailyUsageList) {
        BehaviourMomentumEngine.calculateMomentumScore(recentEvents, primaryGoal, goalProgressList, dailyUsageList)
    }
    val integrityResult = remember(recentEvents, primaryGoal, goalProgressList) {
        GoalIntegrityEngine.calculateGoalIntegrity(primaryGoal, goalProgressList, recentEvents)
    }
    val patternResult = remember(recentEvents) {
        BehaviourInsightsEngine.calculateDistractionPattern(recentEvents, minThreshold = 10)
    }
    val feedbackResult = remember(recentEvents) {
        BehaviourInsightsEngine.evaluatePersonalFeedback(recentEvents, emptyList())
    }
    val whatWorked = remember(recentEvents) {
        BehaviourInsightsEngine.getWhatWorkedSummary(recentEvents)
    }
    val recentWins = remember(recentEvents, recentTransactions) {
        BehaviourInsightsEngine.getRecentWins(recentEvents, recentTransactions, limit = 4)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBack() }
            ) {
                Text("← Back to Today", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DIGITAL DISCIPLINE",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Plan Settings & Configuration",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    OutlinedButton(
                        onClick = { showBehaviourInsightsScreen = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text("Insights", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Protection Status Card
            val isProtectionHealthy = isA11yActive && isOverlayActive
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isProtectionHealthy) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFF7F1D1D).copy(alpha = 0.3f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isProtectionHealthy) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isProtectionHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isProtectionHealthy) "DISCIPLINE ACTIVE" else "PROTECTION NEEDS ATTENTION",
                                color = if (isProtectionHealthy) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isProtectionHealthy) {
                                Text(
                                    text = "Enable permissions to activate real-time habit control.",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (!isProtectionHealthy) {
                        Button(
                            onClick = {
                                if (!isA11yActive) {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                } else if (!isOverlayActive) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ENABLE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. PRIMARY GOAL CARD (GOAL-FIRST WITH MOMENTUM & INTEGRITY)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TODAY'S PRIMARY GOAL",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Momentum Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(momentumResult.state.badge, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(momentumResult.state.displayName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Text(
                            text = "Edit Goal",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showEditGoalDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (primaryGoal != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (primaryGoal.category) {
                                            GoalCategory.FITNESS.name -> "💪"
                                            GoalCategory.STUDY.name -> "📚"
                                            GoalCategory.PRODUCTIVITY.name -> "🎯"
                                            GoalCategory.MINDFULNESS.name -> "🧘"
                                            GoalCategory.READING.name -> "📖"
                                            GoalCategory.SLEEP.name -> "😴"
                                            else -> "✨"
                                        },
                                        fontSize = 24.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = primaryGoal.title,
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = primaryGoal.description.ifBlank { "Protect your focus and build this habit." },
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val completed = todayProgress?.completedCount ?: 0
                        val target = primaryGoal.dailyTarget.coerceAtLeast(1)
                        val progressFraction = (completed.toFloat() / target.toFloat()).coerceIn(0f, 1f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily Progress", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            Text("$completed / $target ${primaryGoal.unit}", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Momentum: ${momentumResult.score}/100",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Goal Integrity: ${integrityResult.score}%",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text("No active goal configured.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }

            // ADAPTIVE PLAN SUGGESTION CARD (IF PENDING ADJUSTMENT EXISTS)
            val pendingAdj = pendingAdjustment
            if (pendingAdj != null && pendingAdj.status == AdjustmentStatus.PENDING.name) {
                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2A4D)),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLAN SUGGESTION",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = pendingAdj.reason,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF091829),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("CURRENT: ${pendingAdj.currentConfiguration}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("SUGGESTED: ${pendingAdj.suggestedConfiguration}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        personalizationRepository.applyAdjustment(pendingAdj)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("APPLY CHANGE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        personalizationRepository.rejectAdjustment(pendingAdj)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text("KEEP MY PLAN", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. EARNED TIME WALLET
            val availableMins = (wallet?.availableSeconds ?: 0) / 60
            val dailyEarnedMins = (wallet?.dailyEarnedSeconds ?: 0) / 60
            val dailyConsumedMins = (wallet?.dailyConsumedSeconds ?: 0) / 60

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EARNED TIME WALLET",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (activeSession != null) {
                            Text(
                                text = "🟢 ACTIVE SESSION",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$availableMins min",
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

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⏱️", fontSize = 26.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Earned Today", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text("+$dailyEarnedMins min", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Used Today", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text("-$dailyConsumedMins min", color = Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Daily Cap", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text("${(wallet?.dailyEarnCapSeconds ?: 3600) / 60} min", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Zero balance positive prompt
                    if (availableMins == 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your earned time is used up.\nWant to earn a little more?",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            walletService.earnTime(
                                                amountSeconds = 600,
                                                source = "SQUATS",
                                                idempotencyKey = "manual_earn_${System.currentTimeMillis()}"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("EARN 10 MIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. TODAY'S BEHAVIOUR ANALYTICS
            val totalAttemptsToday = dailyUsageList.sumOf { it.attempts }
            val completedInterventionsToday = dailyUsageList.sumOf { it.completed }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TODAY'S BEHAVIOUR",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Distraction Attempts", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text("$totalAttemptsToday", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Interventions Done", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text("$completedInterventionsToday", color = Color(0xFF34D399), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Habit Interruption", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text("${hir.toInt()}%", color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. WHAT WORKED & YOUR PATTERN
            if (whatWorked != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("WHAT WORKED", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(whatWorked, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // YOUR PATTERN CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📊", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("YOUR PATTERN", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(patternResult.message, color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. COACHING INSIGHT
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌱", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("COACHING INSIGHT", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(feedbackResult.feedbackMessage, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. ACTION TILES (INSIGHTS & WEEKLY REVIEW)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showBehaviourInsightsScreen = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🔍", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Deep Insights", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Patterns & Experiments", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showWeeklyReviewScreen = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("📅", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Weekly Review", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("7-day progress snapshot", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7. MY PLAN SUMMARY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MY PLAN",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Edit Plan",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showEditTriggersDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Goal", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(primaryGoal?.title ?: "Get Fit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Distractions", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text("${activeTriggers.size} apps monitored", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Replacement Habit", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(primaryBehaviour?.title ?: "10 Squats", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEditRewardDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reward (Tap to edit)", color = Color(0xFF64748B), fontSize = 12.sp)
                            val rewardSecs = primaryPolicy?.earnedSeconds ?: 300
                            val rewardMins = rewardSecs / 60
                            Text("${if (rewardMins > 0) "$rewardMins min" else "${rewardSecs}s"} Screen Time ✏️", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Daily Limit", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text("${(wallet?.dailyEarnCapSeconds ?: 3600) / 60} minutes", color = Color.White, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Max Session", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text("${(wallet?.maxSessionSeconds ?: 1800) / 60} minutes", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // EDIT GOAL DIALOG
        if (showEditGoalDialog && primaryGoal != null) {
            var newTitle by remember { mutableStateOf(primaryGoal.title) }
            var newTarget by remember { mutableIntStateOf(primaryGoal.dailyTarget) }

            AlertDialog(
                onDismissRequest = { showEditGoalDialog = false },
                title = { Text("Edit Primary Goal", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Goal Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Daily Target:", color = Color.White, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (newTarget > 1) newTarget-- }) {
                                    Text("−", color = Color(0xFF38BDF8), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$newTarget", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { if (newTarget < 20) newTarget++ }) {
                                    Text("+", color = Color(0xFF38BDF8), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                behaviourRepository.saveGoal(
                                    primaryGoal.copy(
                                        title = newTitle,
                                        dailyTarget = newTarget,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                            showEditGoalDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditGoalDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // EDIT DISTRACTIONS DIALOG (REAL INSTALLED APPS WITH SEARCH)
        if (showEditTriggersDialog) {
            val installedApps = remember {
                try {
                    val pm = context.packageManager
                    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val resolveList = pm.queryIntentActivities(launcherIntent, 0)
                    val myPackage = context.packageName
                    resolveList.mapNotNull { ri ->
                        val pkg = ri.activityInfo.packageName
                        if (pkg == myPackage) null
                        else {
                            val label = ri.loadLabel(pm).toString()
                            pkg to label
                        }
                    }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
                } catch (e: Exception) {
                    listOf(
                        "com.instagram.android" to "Instagram",
                        "com.google.android.youtube" to "YouTube",
                        "com.dts.freefireth" to "Free Fire",
                        "com.whatsapp" to "WhatsApp",
                        "com.snapchat.android" to "Snapchat",
                        "com.reddit.frontpage" to "Reddit",
                        "com.facebook.katana" to "Facebook",
                        "com.twitter.android" to "X / Twitter",
                        "com.netflix.mediaclient" to "Netflix",
                        "com.android.chrome" to "Chrome"
                    )
                }
            }

            var dialogSearchQuery by remember { mutableStateOf("") }
            val filteredApps = remember(installedApps, dialogSearchQuery) {
                if (dialogSearchQuery.isBlank()) installedApps
                else installedApps.filter {
                    it.second.contains(dialogSearchQuery, ignoreCase = true) ||
                    it.first.contains(dialogSearchQuery, ignoreCase = true)
                }
            }

            val currentSelectedPackages = remember {
                mutableStateListOf<String>().apply {
                    addAll(activeTriggers.map { it.packageName })
                }
            }

            AlertDialog(
                onDismissRequest = { showEditTriggersDialog = false },
                title = {
                    Column {
                        Text("Select Protected Apps", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Selected: ${currentSelectedPackages.size} apps", color = Color(0xFF38BDF8), fontSize = 12.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        OutlinedTextField(
                            value = dialogSearchQuery,
                            onValueChange = { dialogSearchQuery = it },
                            placeholder = { Text("Search installed apps...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredApps.forEach { (pkg, name) ->
                                val isChecked = currentSelectedPackages.contains(pkg)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                if (currentSelectedPackages.size > 1) currentSelectedPackages.remove(pkg)
                                            } else {
                                                currentSelectedPackages.add(pkg)
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    com.digitaldiscipline.spike.ui.components.AppIconImage(
                                        packageName = pkg,
                                        modifier = Modifier.size(30.dp),
                                        cornerRadius = 6.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(pkg, color = Color(0xFF64748B), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val goalId = primaryGoal?.goalId ?: "goal_self_default"
                                val currentPolicies = behaviourRepository.getActivePolicies()
                                val existingBehaviourId = currentPolicies.firstOrNull()?.replacementBehaviourId ?: "beh_pause_10s"
                                val existingMode = currentPolicies.firstOrNull()?.interventionMode ?: "EARN"
                                val existingEarnedSeconds = currentPolicies.firstOrNull()?.earnedSeconds ?: 300

                                activeTriggers.forEach {
                                    behaviourRepository.deleteTrigger(it.triggerId)
                                }
                                currentPolicies.forEach {
                                    behaviourRepository.deletePolicy(it.policyId)
                                }

                                currentSelectedPackages.forEachIndexed { index, pkg ->
                                    val triggerId = "trig_${UUID.randomUUID()}"
                                    val appName = installedApps.firstOrNull { it.first == pkg }?.second ?: pkg
                                    behaviourRepository.saveTrigger(
                                        TriggerEntity(
                                            triggerId = triggerId,
                                            ownerId = "self",
                                            goalId = goalId,
                                            packageName = pkg,
                                            appDisplayName = appName,
                                            category = TriggerCategory.CUSTOM.name,
                                            active = true,
                                            priority = index + 1
                                        )
                                    )
                                    behaviourRepository.savePolicy(
                                        BehaviourPolicyEntity(
                                            policyId = "pol_${UUID.randomUUID()}",
                                            ownerId = "self",
                                            goalId = goalId,
                                            triggerId = triggerId,
                                            replacementBehaviourId = existingBehaviourId,
                                            interventionMode = existingMode,
                                            earnedSeconds = existingEarnedSeconds,
                                            enabled = true,
                                            priority = index + 1
                                        )
                                    )
                                }
                            }
                            showEditTriggersDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Save (${currentSelectedPackages.size})")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTriggersDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // EDIT REWARD DIALOG
        if (showEditRewardDialog) {
            val currentRewardSecs = primaryPolicy?.earnedSeconds ?: 300
            val currentRewardMins = currentRewardSecs / 60
            val rewardOptions = listOf(
                5 to "5 minutes (Strict & Controlled)",
                10 to "10 minutes (Recommended Balanced)",
                15 to "15 minutes (Generous Access)",
                30 to "30 minutes (Extended Block)"
            )
            var selectedMinutes by remember { mutableIntStateOf(if (currentRewardMins > 0) currentRewardMins else 5) }

            AlertDialog(
                onDismissRequest = { showEditRewardDialog = false },
                title = { Text("Choose Reward Access Time", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "How much intentional screen time do you want to unlock upon completing a friction challenge?",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        rewardOptions.forEach { (mins, label) ->
                            val isSelected = mins == selectedMinutes
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMinutes = mins }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedMinutes = mins },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val currentPolicies = behaviourRepository.getActivePolicies()
                                currentPolicies.forEach { policy ->
                                    behaviourRepository.savePolicy(policy.copy(earnedSeconds = selectedMinutes * 60))
                                }
                            }
                            showEditRewardDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Save Reward")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditRewardDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // EDIT INTERVENTION DIALOG
        if (showEditInterventionDialog) {
            val interventionChoices = listOf(
                Triple("beh_squats_10", "10 Squats Challenge", "EARN"),
                Triple("beh_pause_10s", "10s Mindful Pause", "EARN"),
                Triple("beh_breathing_30s", "30s Box Breathing", "EARN"),
                Triple("beh_study_timer_25m", "25m Focus Block", "EARN"),
                Triple("beh_hard_block", "Strict Hard Block", "BLOCK")
            )
            var selectedChoice by remember { mutableStateOf(primaryPolicy?.replacementBehaviourId ?: "beh_squats_10") }

            AlertDialog(
                onDismissRequest = { showEditInterventionDialog = false },
                title = { Text("Choose Replacement Habit", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        interventionChoices.forEach { (id, title, mode) ->
                            val isSelected = selectedChoice == id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedChoice = id },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    if (isSelected) {
                                        Text("✓", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val currentPolicies = behaviourRepository.getActivePolicies()
                                val selectedMode = interventionChoices.firstOrNull { it.first == selectedChoice }?.third ?: "EARN"

                                currentPolicies.forEach { pol ->
                                    behaviourRepository.savePolicy(
                                        pol.copy(
                                            replacementBehaviourId = selectedChoice,
                                            interventionMode = selectedMode,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            showEditInterventionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditInterventionDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }
    }
}

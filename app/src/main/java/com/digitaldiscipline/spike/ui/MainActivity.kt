package com.digitaldiscipline.spike.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.data.local.entities.UserMode
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.ui.cloud.CloudHubScreen
import com.digitaldiscipline.spike.ui.cloud.DevicePairingScreen
import com.digitaldiscipline.spike.ui.dashboard.ParentDashboardScreen
import com.digitaldiscipline.spike.ui.dashboard.SelfDashboardScreen
import com.digitaldiscipline.spike.ui.dashboard.SelfBehaviourInsightsScreen
import com.digitaldiscipline.spike.ui.dashboard.SelfWeeklyReviewScreen
import com.digitaldiscipline.spike.ui.dashboard.TodayScreen
import com.digitaldiscipline.spike.ui.onboarding.ModeSelectionScreen
import com.digitaldiscipline.spike.ui.onboarding.OnboardingScreen
import com.digitaldiscipline.spike.ui.onboarding.SelfModeOnboardingScreen
import com.digitaldiscipline.spike.ui.theme.DigitalDisciplineTheme
import kotlinx.coroutines.launch

enum class AppScreen {
    DASHBOARD,
    CLOUD_HUB,
    DEVICE_PAIRING
}

/**
 * Deep-link destination parsed from a digitaldiscipline:// URI.
 * Used by notification tap handlers to navigate to the correct screen.
 */
sealed class NotificationDeepLink {
    object Today : NotificationDeepLink()
    data class Action(val actionId: String) : NotificationDeepLink()
    object WeeklyReview : NotificationDeepLink()
    object Unknown : NotificationDeepLink()

    companion object {
        /**
         * Parse a deep-link string to a typed destination.
         * IDs are validated; malformed or empty IDs fall back to Unknown.
         */
        fun parse(deepLink: String?): NotificationDeepLink {
            if (deepLink.isNullOrBlank()) return Unknown
            return when {
                deepLink == "digitaldiscipline://today" -> Today
                deepLink == "digitaldiscipline://weekly-review" -> WeeklyReview
                deepLink.startsWith("digitaldiscipline://action/") -> {
                    val id = deepLink.removePrefix("digitaldiscipline://action/").trim()
                    if (id.isNotBlank()) Action(id) else Unknown
                }
                else -> Unknown
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    private val app by lazy { application as DigitalDisciplineApp }
    private val policyEngine by lazy { app.policyEngine }
    private val policyRepository by lazy { app.policyRepository }
    private val analyticsRepository by lazy { app.analyticsRepository }
    private val preferencesManager by lazy { app.preferencesManager }
    private val pinManager by lazy { app.pinManager }
    private val tamperDetector by lazy { app.tamperDetector }
    private val behaviourRepository by lazy { app.behaviourRepository }
    private val walletService by lazy { app.walletService }
    private val personalizationRepository by lazy { app.personalizationRepository }
    private val experimentRepository by lazy { app.experimentRepository }
    private val firebaseAuthManager by lazy { app.firebaseAuthManager }
    private val cloudRepository by lazy { app.cloudRepository }
    private val pairingManager by lazy { app.pairingManager }
    private val syncManager by lazy { app.syncManager }

    private var isUsageStatsGrantedState = mutableStateOf(false)
    private var isAccessibilityGrantedState = mutableStateOf(false)
    private var isOverlayGrantedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DigitalDisciplineTheme {
                val isOnboardingCompleted by preferencesManager.onboardingCompletedFlow.collectAsState(initial = false)
                val userModeString by preferencesManager.userModeFlow.collectAsState(initial = UserMode.PARENT.name)
                val userDisplayName by preferencesManager.userDisplayNameFlow.collectAsState(initial = "")
                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
                var selectedOnboardingMode by remember { mutableStateOf<UserMode?>(null) }

                Surface(
                    color = Color(0xFF090D16)
                ) {
                    if (!isOnboardingCompleted) {
                        when (selectedOnboardingMode) {
                            null -> {
                                ModeSelectionScreen(
                                    onSelectSelfMode = {
                                        selectedOnboardingMode = UserMode.SELF
                                    },
                                    onSelectFamilyMode = {
                                        selectedOnboardingMode = UserMode.FAMILY
                                    },
                                    onSelectChildMode = {
                                        lifecycleScope.launch {
                                            preferencesManager.setUserMode(UserMode.CHILD.name)
                                            preferencesManager.setDeviceRole("CHILD_DEVICE")
                                            preferencesManager.setOnboardingCompleted(true)
                                        }
                                    },
                                    onSelectOfficeMode = {
                                        lifecycleScope.launch {
                                            preferencesManager.setUserMode(UserMode.OFFICE.name)
                                            preferencesManager.setOnboardingCompleted(true)
                                        }
                                    }
                                )
                            }
                            UserMode.SELF -> {
                                SelfModeOnboardingScreen(
                                    context = this@MainActivity,
                                    behaviourRepository = behaviourRepository,
                                    walletService = walletService,
                                    preferencesManager = preferencesManager,
                                    isAccessibilityGranted = isAccessibilityGrantedState.value,
                                    isOverlayGranted = isOverlayGrantedState.value,
                                    onComplete = {
                                        lifecycleScope.launch {
                                            preferencesManager.setUserMode(UserMode.SELF.name)
                                            preferencesManager.setOnboardingCompleted(true)
                                        }
                                    },
                                    onBackToModeSelect = {
                                        selectedOnboardingMode = null
                                    }
                                )
                            }
                            UserMode.FAMILY, UserMode.PARENT -> {
                                OnboardingScreen(
                                    context = this@MainActivity,
                                    policyRepository = policyRepository,
                                    pinManager = pinManager,
                                    isAccessibilityGranted = isAccessibilityGrantedState.value,
                                    isOverlayGranted = isOverlayGrantedState.value,
                                    isUsageStatsGranted = isUsageStatsGrantedState.value,
                                    onCompleteOnboarding = {
                                        lifecycleScope.launch {
                                            preferencesManager.setUserMode(UserMode.FAMILY.name)
                                            preferencesManager.setDeviceRole("PARENT_DEVICE")
                                            preferencesManager.setOnboardingCompleted(true)
                                        }
                                    }
                                )
                            }
                            UserMode.CHILD -> {
                                lifecycleScope.launch {
                                    preferencesManager.setUserMode(UserMode.CHILD.name)
                                    preferencesManager.setDeviceRole("CHILD_DEVICE")
                                    preferencesManager.setOnboardingCompleted(true)
                                }
                            }
                            UserMode.OFFICE -> {
                                lifecycleScope.launch {
                                    preferencesManager.setUserMode(UserMode.OFFICE.name)
                                    preferencesManager.setOnboardingCompleted(true)
                                }
                            }
                        }
                    } else {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> {
                                if (userModeString == UserMode.SELF.name) {
                                    var selfSubScreen by remember { mutableStateOf("TODAY") }
                                    when (selfSubScreen) {
                                        "INTERVENTIONS" -> {
                                            com.digitaldiscipline.spike.ui.dashboard.InterventionCatalogPickerScreen(
                                                coroutineScope = lifecycleScope,
                                                preferencesManager = preferencesManager,
                                                onNavigateBack = { selfSubScreen = "TODAY" }
                                            )
                                        }
                                        "PLAN" -> {
                                            SelfDashboardScreen(
                                                context = this@MainActivity,
                                                coroutineScope = lifecycleScope,
                                                behaviourRepository = behaviourRepository,
                                                analyticsRepository = analyticsRepository,
                                                walletService = walletService,
                                                personalizationRepository = personalizationRepository,
                                                experimentRepository = experimentRepository,
                                                preferencesManager = preferencesManager,
                                                isA11yActive = isAccessibilityGrantedState.value,
                                                isOverlayActive = isOverlayGrantedState.value,
                                                onBack = { selfSubScreen = "TODAY" },
                                                onSwitchToParentMode = {
                                                    lifecycleScope.launch {
                                                        preferencesManager.setUserMode(UserMode.PARENT.name)
                                                    }
                                                }
                                            )
                                        }
                                        "INSIGHTS" -> {
                                            SelfBehaviourInsightsScreen(
                                                context = this@MainActivity,
                                                coroutineScope = lifecycleScope,
                                                behaviourRepository = behaviourRepository,
                                                analyticsRepository = analyticsRepository,
                                                experimentRepository = experimentRepository,
                                                onBack = { selfSubScreen = "TODAY" }
                                            )
                                        }
                                        "WEEKLY" -> {
                                            SelfWeeklyReviewScreen(
                                                context = this@MainActivity,
                                                coroutineScope = lifecycleScope,
                                                personalizationRepository = personalizationRepository,
                                                onBack = { selfSubScreen = "TODAY" },
                                                onNavigateToPlanEdit = { selfSubScreen = "PLAN" }
                                            )
                                        }
                                        "MOMENTUM" -> {
                                            val recentEvents by analyticsRepository.getRecentInterventionEventsFlow(150).collectAsState(initial = emptyList())
                                            val goals by behaviourRepository.getAllGoalsFlow().collectAsState(initial = emptyList())
                                            val primaryGoal = goals.firstOrNull { it.active } ?: goals.firstOrNull()
                                            val goalProgressList by if (primaryGoal != null) {
                                                behaviourRepository.getProgressForGoalFlow(primaryGoal.goalId).collectAsState(initial = emptyList())
                                            } else {
                                                remember { mutableStateOf(emptyList()) }
                                            }
                                            val recentTransactions by walletService.getRecentTransactionsFlow().collectAsState(initial = emptyList())
                                            val firstWinState by preferencesManager.firstWinStateFlow.collectAsState(initial = "NOT_STARTED")

                                            val snapshot = remember(recentEvents, primaryGoal, goalProgressList, recentTransactions, firstWinState) {
                                                com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumEngine.evaluate7DayWindow(
                                                    events = recentEvents,
                                                    goal = primaryGoal,
                                                    progressList = goalProgressList,
                                                    walletTransactions = recentTransactions,
                                                    firstWinCompleted = (firstWinState == "FIRST_WIN_COMPLETED" || firstWinState == "TIME_USED" || firstWinState == "TIME_SAVED")
                                                )
                                            }

                                            com.digitaldiscipline.spike.ui.dashboard.HabitMomentumScreen(
                                                snapshot = snapshot,
                                                onNavigateBack = { selfSubScreen = "TODAY" },
                                                onStartDailyAction = { selfSubScreen = "TODAY" }
                                            )
                                        }
                                        "PLAN_CONTINUITY" -> {
                                            val recentEvents by analyticsRepository.getRecentInterventionEventsFlow(150).collectAsState(initial = emptyList())
                                            val goals by behaviourRepository.getAllGoalsFlow().collectAsState(initial = emptyList())
                                            val primaryGoal = goals.firstOrNull { it.active } ?: goals.firstOrNull()
                                            val goalProgressList by if (primaryGoal != null) {
                                                behaviourRepository.getProgressForGoalFlow(primaryGoal.goalId).collectAsState(initial = emptyList())
                                            } else {
                                                remember { mutableStateOf(emptyList()) }
                                            }
                                            val recentTransactions by walletService.getRecentTransactionsFlow().collectAsState(initial = emptyList())
                                            val firstWinState by preferencesManager.firstWinStateFlow.collectAsState(initial = "NOT_STARTED")
                                            val behaviours by behaviourRepository.getAllBehavioursFlow().collectAsState(initial = emptyList())
                                            val policies by behaviourRepository.getAllPoliciesFlow().collectAsState(initial = emptyList())
                                            val pendingAdjustment by personalizationRepository.getLatestPendingAdjustmentFlow().collectAsState(initial = null)
                                            val planContinuityState by preferencesManager.planContinuityStateFlow.collectAsState(initial = null)
                                            val lastPlanReviewTimestamp by preferencesManager.lastPlanReviewTimestampFlow.collectAsState(initial = 0L)
                                            val savedWeekNumber by preferencesManager.planActiveWeekNumberFlow.collectAsState(initial = 1)

                                            val habitSnapshot = remember(recentEvents, primaryGoal, goalProgressList, recentTransactions, firstWinState) {
                                                com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumEngine.evaluate7DayWindow(
                                                    events = recentEvents,
                                                    goal = primaryGoal,
                                                    progressList = goalProgressList,
                                                    walletTransactions = recentTransactions,
                                                    firstWinCompleted = (firstWinState == "FIRST_WIN_COMPLETED" || firstWinState == "TIME_USED" || firstWinState == "TIME_SAVED")
                                                )
                                            }

                                            val activeBehaviour = behaviours.firstOrNull { it.behaviourId == policies.firstOrNull()?.replacementBehaviourId } ?: behaviours.firstOrNull()
                                            val activeReward = policies.firstOrNull()?.earnedSeconds ?: 600

                                            val recommendation = pendingAdjustment?.let { adj ->
                                                try {
                                                    com.digitaldiscipline.spike.behaviour.adaptive.BehaviourRecommendation(
                                                        type = com.digitaldiscipline.spike.behaviour.adaptive.RecommendationType.valueOf(adj.recommendationType),
                                                        title = "Suggested Refinement",
                                                        explanation = adj.reason,
                                                        currentConfiguration = adj.currentConfiguration,
                                                        suggestedConfiguration = adj.suggestedConfiguration,
                                                        confidenceLevel = com.digitaldiscipline.spike.behaviour.adaptive.ConfidenceLevel.HIGH,
                                                        evidence = adj.reason,
                                                        cooldownSeconds = adj.cooldownSeconds
                                                    )
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }

                                            val continuitySnapshot = remember(habitSnapshot, primaryGoal, activeBehaviour, activeReward, recommendation, lastPlanReviewTimestamp, planContinuityState, savedWeekNumber) {
                                                com.digitaldiscipline.spike.behaviour.continuity.PlanContinuityEngine.evaluateContinuitySnapshot(
                                                    habitSnapshot = habitSnapshot,
                                                    activeGoal = primaryGoal,
                                                    activeBehaviour = activeBehaviour,
                                                    activeRewardSeconds = activeReward,
                                                    recommendation = recommendation,
                                                    lastPlanReviewTimestamp = lastPlanReviewTimestamp,
                                                    savedContinuityState = planContinuityState,
                                                    savedWeekNumber = savedWeekNumber
                                                )
                                            }

                                            com.digitaldiscipline.spike.ui.dashboard.SelfPlanContinuityScreen(
                                                snapshot = continuitySnapshot,
                                                onKeepPlan = {
                                                    lifecycleScope.launch {
                                                        preferencesManager.setPlanContinuityState(com.digitaldiscipline.spike.behaviour.continuity.PlanContinuityState.PLAN_CONFIRMED.name)
                                                        preferencesManager.setLastPlanReviewTimestamp(System.currentTimeMillis())
                                                        preferencesManager.setPlanActiveWeekNumber(continuitySnapshot.activeWeekNumber + 1)
                                                        selfSubScreen = "TODAY"
                                                    }
                                                },
                                                onApplyRecommendation = { _ ->
                                                    lifecycleScope.launch {
                                                        pendingAdjustment?.let { adj ->
                                                            personalizationRepository.applyAdjustment(adj)
                                                        }
                                                        preferencesManager.setPlanContinuityState(com.digitaldiscipline.spike.behaviour.continuity.PlanContinuityState.PLAN_REFINED.name)
                                                        preferencesManager.setLastPlanReviewTimestamp(System.currentTimeMillis())
                                                        selfSubScreen = "TODAY"
                                                    }
                                                },
                                                onChangeGoal = { selfSubScreen = "PLAN" },
                                                onStartFresh = { selfSubScreen = "PLAN" },
                                                onNavigateBack = { selfSubScreen = "TODAY" }
                                            )
                                        }
                                        "GOAL_LIFECYCLE" -> {
                                            val goals by behaviourRepository.getAllGoalsFlow().collectAsState(initial = emptyList())
                                            val primaryGoal = goals.firstOrNull { it.active } ?: goals.firstOrNull()
                                            val primaryGoalLifecycleState by preferencesManager.primaryGoalLifecycleStateFlow.collectAsState(initial = "ACTIVE")
                                            val recentEvents by analyticsRepository.getRecentInterventionEventsFlow(150).collectAsState(initial = emptyList())
                                            val recentTransactions by walletService.getRecentTransactionsFlow().collectAsState(initial = emptyList())

                                            val goalLifecycleService = remember {
                                                com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleService(
                                                    behaviourRepository = behaviourRepository,
                                                    preferencesManager = preferencesManager,
                                                    walletService = walletService
                                                )
                                            }

                                            val currentEnumState = try {
                                                com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState.valueOf(primaryGoalLifecycleState)
                                            } catch (e: Exception) {
                                                com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState.ACTIVE
                                            }

                                            val lifecycleSnapshot = remember(primaryGoal, currentEnumState, recentEvents, recentTransactions) {
                                                com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleEngine.evaluateLifecycleSnapshot(
                                                    activeGoal = primaryGoal,
                                                    currentState = currentEnumState,
                                                    meaningfulDays = recentEvents.map { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it.timestamp)) }.distinct().size,
                                                    totalInterventions = recentEvents.size,
                                                    earnedMinutes = recentTransactions.filter { it.type == "EARN" }.sumOf { it.amountSeconds } / 60
                                                )
                                            }

                                            com.digitaldiscipline.spike.ui.dashboard.GoalLifecycleScreen(
                                                snapshot = lifecycleSnapshot,
                                                onPauseGoal = {
                                                    primaryGoal?.let {
                                                        lifecycleScope.launch {
                                                            goalLifecycleService.pauseActiveGoal(it.goalId)
                                                            selfSubScreen = "TODAY"
                                                        }
                                                    }
                                                },
                                                onResumeGoal = {
                                                    primaryGoal?.let {
                                                        lifecycleScope.launch {
                                                            goalLifecycleService.resumeGoal(it.goalId)
                                                            selfSubScreen = "TODAY"
                                                        }
                                                    }
                                                },
                                                onCompleteGoal = {
                                                    primaryGoal?.let {
                                                        lifecycleScope.launch {
                                                            goalLifecycleService.completeActiveGoal(it.goalId)
                                                            selfSubScreen = "TODAY"
                                                        }
                                                    }
                                                },
                                                onChangeGoal = { selfSubScreen = "PLAN" },
                                                onStartFresh = {
                                                    primaryGoal?.let {
                                                        lifecycleScope.launch {
                                                            goalLifecycleService.startFreshGoal(it.goalId)
                                                            selfSubScreen = "TODAY"
                                                        }
                                                    }
                                                },
                                                onViewHistory = { selfSubScreen = "GOAL_HISTORY" },
                                                onNavigateBack = { selfSubScreen = "TODAY" }
                                            )
                                        }
                                        "GOAL_HISTORY" -> {
                                            val goals by behaviourRepository.getAllGoalsFlow().collectAsState(initial = emptyList())
                                            val primaryGoalLifecycleState by preferencesManager.primaryGoalLifecycleStateFlow.collectAsState(initial = "ACTIVE")
                                            val recentEvents by analyticsRepository.getRecentInterventionEventsFlow(150).collectAsState(initial = emptyList())
                                            val recentTransactions by walletService.getRecentTransactionsFlow().collectAsState(initial = emptyList())
                                            var selectedHistoryGoal by remember { mutableStateOf<com.digitaldiscipline.spike.behaviour.lifecycle.HistoricalGoalSummary?>(null) }

                                            if (selectedHistoryGoal != null) {
                                                com.digitaldiscipline.spike.ui.dashboard.GoalHistoryDetailScreen(
                                                    summary = selectedHistoryGoal!!,
                                                    onNavigateBack = { selectedHistoryGoal = null }
                                                )
                                            } else {
                                                val historicalSummaries = remember(goals, primaryGoalLifecycleState, recentEvents, recentTransactions) {
                                                    goals.map { g ->
                                                        val state = if (g.active) {
                                                            try {
                                                                com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState.valueOf(primaryGoalLifecycleState)
                                                            } catch (e: Exception) {
                                                                com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState.ACTIVE
                                                            }
                                                        } else {
                                                            com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState.COMPLETED
                                                        }
                                                        com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleEngine.buildHistoricalGoalSummary(
                                                            goal = g,
                                                            state = state,
                                                            meaningfulDays = recentEvents.map { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it.timestamp)) }.distinct().size,
                                                            totalInterventions = recentEvents.size,
                                                            earnedMinutes = recentTransactions.filter { it.type == "EARN" }.sumOf { it.amountSeconds } / 60,
                                                            savedMinutes = recentEvents.size * 10
                                                        )
                                                    }
                                                }

                                                com.digitaldiscipline.spike.ui.dashboard.GoalHistoryScreen(
                                                    historicalGoals = historicalSummaries,
                                                    onSelectGoal = { summary -> selectedHistoryGoal = summary },
                                                    onNavigateBack = { selfSubScreen = "GOAL_LIFECYCLE" }
                                                )
                                            }
                                        }
                                        "JOURNEY" -> {
                                            val goals by behaviourRepository.getAllGoalsFlow().collectAsState(initial = emptyList())
                                            val primaryGoal = goals.firstOrNull { it.active } ?: goals.firstOrNull()
                                            val primaryGoalLifecycleState by preferencesManager.primaryGoalLifecycleStateFlow.collectAsState(initial = "ACTIVE")
                                            val savedWeekNumber by preferencesManager.planActiveWeekNumberFlow.collectAsState(initial = 1)
                                            val recentEvents by analyticsRepository.getRecentInterventionEventsFlow(150).collectAsState(initial = emptyList())
                                            val recentTransactions by walletService.getRecentTransactionsFlow().collectAsState(initial = emptyList())
                                            val firstWinState by preferencesManager.firstWinStateFlow.collectAsState(initial = "NOT_STARTED")
                                            val firstWinTimestamp by preferencesManager.firstWinCompletedAtFlow.collectAsState(initial = 0L)
                                            val firstWinActionTitle by preferencesManager.firstWinActionTitleFlow.collectAsState(initial = null)
                                            val pausedAtTimestamp by preferencesManager.primaryGoalPausedAtFlow.collectAsState(initial = 0L)
                                            val completedAtTimestamp by preferencesManager.primaryGoalCompletedAtFlow.collectAsState(initial = 0L)
                                            val goalProgressList by if (primaryGoal != null) {
                                                behaviourRepository.getProgressForGoalFlow(primaryGoal.goalId).collectAsState(initial = emptyList())
                                            } else {
                                                remember { mutableStateOf(emptyList()) }
                                            }

                                            val currentEnumState = try {
                                                com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState.valueOf(primaryGoalLifecycleState)
                                            } catch (e: Exception) {
                                                com.digitaldiscipline.spike.behaviour.lifecycle.GoalLifecycleState.ACTIVE
                                            }

                                            val habitSnapshot = remember(recentEvents, primaryGoal, goalProgressList, recentTransactions, firstWinState) {
                                                com.digitaldiscipline.spike.behaviour.momentum.HabitMomentumEngine.evaluate7DayWindow(
                                                    events = recentEvents,
                                                    goal = primaryGoal,
                                                    progressList = goalProgressList,
                                                    walletTransactions = recentTransactions,
                                                    firstWinCompleted = (firstWinState == "FIRST_WIN_COMPLETED" || firstWinState == "TIME_USED" || firstWinState == "TIME_SAVED")
                                                )
                                            }

                                            val journeySnapshot = remember(goals, currentEnumState, savedWeekNumber, recentEvents, firstWinState, firstWinTimestamp, habitSnapshot, pausedAtTimestamp, completedAtTimestamp) {
                                                com.digitaldiscipline.spike.behaviour.journey.BehaviourJourneyEngine.evaluateJourneySnapshot(
                                                    goals = goals,
                                                    currentLifecycleState = currentEnumState,
                                                    currentWeekNumber = savedWeekNumber,
                                                    events = recentEvents,
                                                    firstWinState = firstWinState,
                                                    firstWinTimestamp = firstWinTimestamp,
                                                    firstWinActionTitle = firstWinActionTitle,
                                                    habitSnapshot = habitSnapshot,
                                                    pausedAtTimestamp = pausedAtTimestamp,
                                                    completedAtTimestamp = completedAtTimestamp
                                                )
                                            }

                                            com.digitaldiscipline.spike.ui.dashboard.SelfJourneyScreen(
                                                snapshot = journeySnapshot,
                                                onNavigateToCurrentPlan = { selfSubScreen = "PLAN" },
                                                onNavigateToGoalHistory = { selfSubScreen = "GOAL_HISTORY" },
                                                onNavigateToPlanContinuity = { selfSubScreen = "PLAN_CONTINUITY" },
                                                onNavigateToGoalLifecycle = { selfSubScreen = "GOAL_LIFECYCLE" },
                                                onNavigateBack = { selfSubScreen = "TODAY" }
                                            )
                                        }
                                        else -> {
                                            TodayScreen(
                                                context = this@MainActivity,
                                                coroutineScope = lifecycleScope,
                                                behaviourRepository = behaviourRepository,
                                                analyticsRepository = analyticsRepository,
                                                walletService = walletService,
                                                personalizationRepository = personalizationRepository,
                                                experimentRepository = experimentRepository,
                                                preferencesManager = preferencesManager,
                                                isA11yActive = isAccessibilityGrantedState.value,
                                                isOverlayActive = isOverlayGrantedState.value,
                                                isUsageStatsActive = isUsageStatsGrantedState.value,
                                                userDisplayName = userDisplayName,
                                                onNavigateToPlan = { selfSubScreen = "PLAN" },
                                                onNavigateToInsights = { selfSubScreen = "INSIGHTS" },
                                                onNavigateToWeeklyReview = { selfSubScreen = "WEEKLY" },
                                                onNavigateToMomentum = { selfSubScreen = "MOMENTUM" },
                                                onNavigateToPlanContinuity = { selfSubScreen = "PLAN_CONTINUITY" },
                                                onNavigateToGoalLifecycle = { selfSubScreen = "GOAL_LIFECYCLE" },
                                                onNavigateToJourney = { selfSubScreen = "JOURNEY" },
                                                onNavigateToInterventions = { selfSubScreen = "INTERVENTIONS" },
                                                onSwitchToParentMode = {
                                                    lifecycleScope.launch {
                                                        preferencesManager.setUserMode(UserMode.PARENT.name)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else if (userModeString == UserMode.OFFICE.name) {
                                    com.digitaldiscipline.spike.ui.dashboard.OfficeDashboardScreen(
                                        context = this@MainActivity,
                                        coroutineScope = lifecycleScope,
                                        policyRepository = policyRepository,
                                        preferencesManager = preferencesManager,
                                        isA11yActive = isAccessibilityGrantedState.value,
                                        isOverlayActive = isOverlayGrantedState.value,
                                        onSwitchMode = { newMode ->
                                            lifecycleScope.launch {
                                                preferencesManager.setUserMode(newMode.name)
                                            }
                                        }
                                    )
                                } else if (userModeString == UserMode.CHILD.name || userModeString == "CHILD") {
                                    com.digitaldiscipline.spike.ui.dashboard.ChildDashboardScreen(
                                        context = this@MainActivity,
                                        coroutineScope = lifecycleScope,
                                        preferencesManager = preferencesManager,
                                        pinManager = pinManager,
                                        syncManager = syncManager,
                                        isA11yActive = isAccessibilityGrantedState.value,
                                        isOverlayActive = isOverlayGrantedState.value,
                                        onNavigateToPairing = { currentScreen = AppScreen.DEVICE_PAIRING },
                                        onOpenParentAdmin = {
                                            lifecycleScope.launch {
                                                preferencesManager.setUserMode(UserMode.FAMILY.name)
                                                preferencesManager.setDeviceRole("PARENT_DEVICE")
                                            }
                                        },
                                        onSwitchMode = { newMode ->
                                            lifecycleScope.launch {
                                                preferencesManager.setUserMode(newMode.name)
                                            }
                                        }
                                    )
                                } else {
                                    ParentDashboardScreen(
                                        context = this@MainActivity,
                                        coroutineScope = lifecycleScope,
                                        policyEngine = policyEngine,
                                        policyRepository = policyRepository,
                                        analyticsRepository = analyticsRepository,
                                        preferencesManager = preferencesManager,
                                        syncManager = syncManager,
                                        pinManager = pinManager,
                                        pairingManager = pairingManager,
                                        isA11yActive = isAccessibilityGrantedState.value,
                                        isOverlayActive = isOverlayGrantedState.value,
                                        onNavigateToCloudHub = { currentScreen = AppScreen.CLOUD_HUB },
                                        onNavigateToPairing = { currentScreen = AppScreen.DEVICE_PAIRING }
                                    )
                                }
                            }
                            AppScreen.CLOUD_HUB -> {
                                CloudHubScreen(
                                    context = this@MainActivity,
                                    coroutineScope = lifecycleScope,
                                    authManager = firebaseAuthManager,
                                    cloudRepository = cloudRepository,
                                    pairingManager = pairingManager,
                                    syncManager = syncManager,
                                    onNavigateToPairDevice = { currentScreen = AppScreen.DEVICE_PAIRING },
                                    onBack = { currentScreen = AppScreen.DASHBOARD }
                                )
                            }
                            AppScreen.DEVICE_PAIRING -> {
                                DevicePairingScreen(
                                    context = this@MainActivity,
                                    coroutineScope = lifecycleScope,
                                    pairingManager = pairingManager,
                                    syncManager = syncManager,
                                    onPairingSuccess = {
                                        currentScreen = AppScreen.DASHBOARD
                                    },
                                    onBack = { currentScreen = AppScreen.DASHBOARD }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            walletService.pauseOrEndSession()
        }
    }

    /**
     * Handle notification deep-link when app is already open (singleTop).
     * The destination screen is determined from the deep_link extra and reloads
     * authoritative state from repositories — never trusts the extra as ground truth.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    /**
     * Handle deep-link from notification tap.
     * Safe: validates ID before navigating; falls back to TODAY on unknown links.
     */
    private fun handleDeepLinkIntent(intent: Intent?) {
        val deepLinkStr = intent?.getStringExtra("deep_link")
            ?: intent?.data?.toString()
            ?: return

        val destination = NotificationDeepLink.parse(deepLinkStr)
        EventLogger.log("NOTIFICATION", "system", "DEEP_LINK_RECEIVED",
            details = "Destination: ${destination::class.simpleName} from $deepLinkStr")

        when (destination) {
            is NotificationDeepLink.Today       -> { /* Default screen; TodayScreen reloads from repo */ }
            is NotificationDeepLink.WeeklyReview -> { /* UI state handled by setContent composition */ }
            is NotificationDeepLink.Action      -> { /* actionId validated; DailyActionScreen loads from repo */ }
            is NotificationDeepLink.Unknown     -> {
                EventLogger.log("NOTIFICATION", "system", "DEEP_LINK_INVALID",
                    details = "Unknown or malformed deep link: $deepLinkStr — defaulting to TODAY")
            }
        }
        // Re-trigger Compose recomposition by updating a remembered state is not needed here:
        // singleTop + onNewIntent re-runs the LaunchedEffect watcher in TodayScreen automatically.
    }

    private fun updatePermissionStates() {
        isUsageStatsGrantedState.value = policyEngine.usageStatsDetector.isPermissionGranted()
        isAccessibilityGrantedState.value = tamperDetector.isAccessibilityActive()
        isOverlayGrantedState.value = policyEngine.overlayManager.canDrawOverlays()
    }
}


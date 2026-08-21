package com.digitaldiscipline.spike.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.activation.SelfModeActivationCoordinator
import com.digitaldiscipline.spike.behaviour.templates.*
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — Phase 8D premium dark palette
// ─────────────────────────────────────────────────────────────────────────────
private val Bg0 = Color(0xFF070B12)
private val BgCard = Color(0xFF0F172A)
private val BgCardRaised = Color(0xFF111827)
private val AccentBlue = Color(0xFF38BDF8)
private val AccentGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)
private val TextMuted = Color(0xFF475569)
private val BorderDefault = Color(0xFF1E293B)
private val BorderSelected = Color(0xFF38BDF8)

// ─────────────────────────────────────────────────────────────────────────────
// Phase 8D — Premium Rewire-Style Self Mode Onboarding
// One question per screen · Cinematic pacing · Dark premium UI
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SelfModeOnboardingScreen(
    context: Context,
    behaviourRepository: BehaviourRepository,
    walletService: EarnedTimeWalletService,
    preferencesManager: PreferencesManager,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onComplete: () -> Unit,
    onBackToModeSelect: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // ── Per-screen state ────────────────────────────────────────────────────
    var step by remember { mutableIntStateOf(0) }

    // Resume from last persisted step (only move forward if > 0 and we haven't advanced yet)
    val savedStep by preferencesManager.selfOnboardingStepFlow.collectAsState(initial = 0)
    if (savedStep in 1..10 && step == 0) {
        step = savedStep
    }

    // Selections
    var selectedPattern by remember { mutableStateOf("") }

    val allDistractions = remember {
        try {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveList = pm.queryIntentActivities(launcherIntent, 0)
            val myPkg = context.packageName
            val installed = resolveList.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == myPkg) null
                else {
                    val label = ri.loadLabel(pm).toString()
                    val category = GoalTemplateRepository.categorizeApp(pkg, label)
                    val icon = when (category) {
                        TriggerCategory.SOCIAL_MEDIA -> "💬"
                        TriggerCategory.VIDEO_STREAMING -> "▶️"
                        TriggerCategory.GAMING -> "🎮"
                        TriggerCategory.SHOPPING -> "🛍️"
                        TriggerCategory.FOOD_DELIVERY -> "🍔"
                        TriggerCategory.CUSTOM -> "📱"
                    }
                    DistractionAppRecommendation(packageName = pkg, displayName = label, icon = icon, category = category)
                }
            }.distinctBy { it.packageName }.sortedWith(
                compareBy<DistractionAppRecommendation> {
                    when (it.category) {
                        TriggerCategory.SOCIAL_MEDIA -> 0
                        TriggerCategory.VIDEO_STREAMING -> 1
                        TriggerCategory.GAMING -> 2
                        TriggerCategory.FOOD_DELIVERY -> 3
                        TriggerCategory.SHOPPING -> 4
                        TriggerCategory.CUSTOM -> 5
                    }
                }.thenBy { it.displayName.lowercase() }
            )

            if (installed.isNotEmpty()) installed
            else GoalTemplateRepository.getAllDistractionRecommendations()
        } catch (_: Exception) {
            GoalTemplateRepository.getAllDistractionRecommendations()
        }
    }
    val selectedApps = remember { mutableStateListOf<DistractionAppRecommendation>() }

    var selectedTimeEstimate by remember { mutableStateOf("1–2 hours") }

    val allOnboardingCategories = remember {
        listOf(
            InterventionCategory.MOVEMENT,
            InterventionCategory.UPPER_BODY,
            InterventionCategory.BREATHING,
            InterventionCategory.MEDITATION,
            InterventionCategory.YOGA_MOBILITY,
            InterventionCategory.PHYSICAL_RESET,
            InterventionCategory.COGNITIVE,
            InterventionCategory.CREATIVE_FLOW,
            InterventionCategory.MINDFUL_PERSPECTIVE
        )
    }
    val selectedCategories = remember {
        mutableStateListOf<InterventionCategory>().apply { addAll(allOnboardingCategories) }
    }

    // Screen 7 micro-intervention
    var breathCount by remember { mutableIntStateOf(0) }
    var microDone by remember { mutableStateOf(false) }

    var selectedRewardMinutes by remember { mutableIntStateOf(5) }

    var isActivating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Back handler ────────────────────────────────────────────────────────
    androidx.activity.compose.BackHandler {
        if (step > 0) step -= 1 else onBackToModeSelect()
    }

    // ── Persist step progress ───────────────────────────────────────────────
    LaunchedEffect(step) {
        if (step > 0) preferencesManager.setSelfOnboardingStep(step)
    }

    // ── Continue guard ──────────────────────────────────────────────────────
    val canContinue = when (step) {
        1 -> selectedPattern.isNotEmpty()
        2 -> selectedApps.isNotEmpty()
        3 -> selectedTimeEstimate.isNotEmpty()
        6 -> selectedCategories.isNotEmpty()
        7 -> microDone
        9 -> isAccessibilityGranted && isOverlayGranted
        else -> true
    }

    // ── Animated screen transitions ─────────────────────────────────────────
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(280))).togetherWith(
                    slideOutHorizontally(tween(260)) { -it / 4 } + fadeOut(tween(200))
                )
            } else {
                (slideInHorizontally(tween(320)) { -it / 3 } + fadeIn(tween(280))).togetherWith(
                    slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(200))
                )
            }
        },
        label = "screen_transition"
    ) { currentStep ->
        when (currentStep) {
            // ───────────────────────────────────────────────────────────────
            // SCREEN 0 — Cinematic opening
            // ───────────────────────────────────────────────────────────────
            0 -> Ob0Opening(onNext = { step = 1 })

            // ───────────────────────────────────────────────────────────────
            // SCREEN 1 — Identify the behaviour pattern
            // ───────────────────────────────────────────────────────────────
            1 -> Ob1BehaviourPattern(
                selected = selectedPattern,
                onSelect = { selectedPattern = it },
                onBack = { onBackToModeSelect() },
                onNext = {
                    coroutineScope.launch { preferencesManager.setOnboardingBehaviourPattern(selectedPattern) }
                    step = 2
                },
                canContinue = canContinue
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 2 — App picker
            // ───────────────────────────────────────────────────────────────
            2 -> Ob2AppPicker(
                apps = allDistractions,
                selected = selectedApps,
                onToggle = { app ->
                    if (selectedApps.any { it.packageName == app.packageName })
                        selectedApps.removeAll { it.packageName == app.packageName }
                    else selectedApps.add(app)
                },
                onBack = { step = 1 },
                onNext = { step = 3 },
                canContinue = canContinue
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 3 — Screen time estimate
            // ───────────────────────────────────────────────────────────────
            3 -> Ob3TimeEstimate(
                selected = selectedTimeEstimate,
                onSelect = { selectedTimeEstimate = it },
                onBack = { step = 2 },
                onNext = {
                    coroutineScope.launch { preferencesManager.setOnboardingScreenTimeEstimate(selectedTimeEstimate) }
                    step = 4
                },
                canContinue = canContinue
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 4 — Aha moment: lifetime projection
            // ───────────────────────────────────────────────────────────────
            4 -> Ob4AhaMoment(
                estimate = selectedTimeEstimate,
                onBack = { step = 3 },
                onNext = { step = 5 }
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 5 — Reframe: you don't have to quit
            // ───────────────────────────────────────────────────────────────
            5 -> Ob5Reframe(
                onBack = { step = 4 },
                onNext = { step = 6 }
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 6 — Choose interruption style
            // ───────────────────────────────────────────────────────────────
            6 -> Ob6InterventionStyle(
                selectedCategories = selectedCategories,
                onToggle = { cat ->
                    if (selectedCategories.contains(cat)) selectedCategories.remove(cat)
                    else selectedCategories.add(cat)
                },
                onSelectAll = {
                    selectedCategories.clear()
                    selectedCategories.addAll(allOnboardingCategories)
                },
                onClearAll = {
                    selectedCategories.clear()
                },
                onBack = { step = 5 },
                onNext = { step = 7 },
                canContinue = canContinue
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 7 — Try your first interruption
            // ───────────────────────────────────────────────────────────────
            7 -> Ob7MicroIntervention(
                done = microDone,
                breathCount = breathCount,
                onBreath = {
                    breathCount++
                    if (breathCount >= 3) microDone = true
                },
                onBack = { step = 6 },
                onNext = { step = 8 },
                canContinue = canContinue
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 8 — Earned access rule
            // ───────────────────────────────────────────────────────────────
            8 -> Ob8EarnedAccess(
                selected = selectedRewardMinutes,
                onSelect = { selectedRewardMinutes = it },
                onBack = { step = 7 },
                onNext = { step = 9 },
                canContinue = canContinue
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 9 — Permissions
            // ───────────────────────────────────────────────────────────────
            9 -> Ob9Permissions(
                context = context,
                isA11y = isAccessibilityGranted,
                isOverlay = isOverlayGranted,
                onBack = { step = 8 },
                onNext = { step = 10 },
                canContinue = canContinue
            )

            // ───────────────────────────────────────────────────────────────
            // SCREEN 10 — Protection ready + atomic activation
            // ───────────────────────────────────────────────────────────────
            10 -> Ob10Ready(
                appsCount = if (selectedApps.isNotEmpty()) selectedApps.size else GoalTemplateRepository.getAllDistractionRecommendations().take(3).size,
                categoriesCount = selectedCategories.size.coerceAtLeast(1),
                rewardMinutes = selectedRewardMinutes,
                isActivating = isActivating,
                errorMessage = errorMessage,
                onBack = { step = 9 },
                onActivate = {
                    isActivating = true
                    errorMessage = null
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val template = GoalTemplateRepository.getAllTemplates()
                                .firstOrNull { it.category == GoalCategory.PRODUCTIVITY }
                                ?: GoalTemplateRepository.getAllTemplates().first()

                            val distractions = if (selectedApps.isNotEmpty()) selectedApps.toList()
                            else GoalTemplateRepository.getAllDistractionRecommendations().take(3)

                            val rewardPreset = when (selectedRewardMinutes) {
                                5 -> RewardPreset.LIGHT
                                15 -> RewardPreset.GENEROUS
                                else -> RewardPreset.STANDARD
                            }

                            val draft = SelfModeActivationCoordinator.createDraft(
                                template = template,
                                selectedDistractions = distractions,
                                selectedReplacement = template.recommendedReplacementBehaviours.first(),
                                rewardPreset = rewardPreset
                            )

                            SelfModeActivationCoordinator.activatePlan(
                                draft = draft,
                                behaviourRepository = behaviourRepository,
                                walletService = walletService,
                                preferencesManager = preferencesManager
                            )

                            val enabledCatNames = selectedCategories.map { it.name }.toSet()
                            val enabledIds = InterventionCatalog.getAllInterventions()
                                .filter { selectedCategories.contains(it.category) }
                                .map { it.id }
                                .toSet()

                            preferencesManager.setEnabledCategories(enabledCatNames)
                            preferencesManager.setEnabledInterventions(enabledIds)
                            preferencesManager.setSelfOnboardingState(SelfModeActivationCoordinator.STATE_COMPLETED)
                            preferencesManager.setOnboardingCompleted(true)

                            withContext(Dispatchers.Main) {
                                isActivating = false
                                onComplete()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isActivating = false
                                errorMessage = "Setup failed: ${e.message}"
                            }
                        }
                    }
                }
            )

            else -> Ob0Opening(onNext = { step = 1 })
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 0 — Cinematic opening
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob0Opening(onNext: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(500); phase = 1
        delay(2200); phase = 2
        delay(1600); phase = 3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg0)
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = phase >= 1,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(900)) { 50 }
            ) {
                Text(
                    text = "Your phone isn't the problem.",
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )
            }

            Spacer(Modifier.height(22.dp))

            AnimatedVisibility(
                visible = phase >= 2,
                enter = fadeIn(tween(900)) + slideInVertically(tween(900)) { 40 }
            ) {
                Text(
                    text = "The moment between impulse and action is.",
                    color = AccentBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            AnimatedVisibility(
                visible = phase >= 3,
                enter = fadeIn(tween(800))
            ) {
                Text(
                    text = "Digital Discipline helps you build that moment.",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Spacer(Modifier.height(64.dp))

            AnimatedVisibility(
                visible = phase >= 3,
                enter = fadeIn(tween(900)) + slideInVertically(tween(800)) { 30 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            text = "BUILD MY PLAN",
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Private. On-device. No surveillance.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 1 — Identify behaviour pattern
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob1BehaviourPattern(
    selected: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean
) {
    val patterns = listOf(
        "I open my phone without thinking.",
        "I scroll longer than I planned.",
        "Minutes turn into hours.",
        "I keep checking apps even when I should be doing something else.",
        "I want to stop, but I open them again."
    )

    ObScaffold(
        step = 1, progress = 0.1f, onBack = onBack, onNext = onNext,
        canContinue = canContinue, ctaLabel = "CONTINUE"
    ) {
        ObQuestion(
            label = "WHAT SOUNDS LIKE YOU",
            question = "What sounds most like you?",
            subtitle = "Pick the one that happens most often."
        )
        Spacer(Modifier.height(24.dp))
        patterns.forEach { p ->
            ObSingleCard(text = p, isSelected = p == selected, onClick = { onSelect(p) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 2 — App picker
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob2AppPicker(
    apps: List<DistractionAppRecommendation>,
    selected: List<DistractionAppRecommendation>,
    onToggle: (DistractionAppRecommendation) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean
) {
    var selectedCategoryFilter by remember { mutableStateOf<TriggerCategory?>(null) }

    val categoryTabs = listOf(
        null to "🔥 All",
        TriggerCategory.SOCIAL_MEDIA to "💬 Social",
        TriggerCategory.VIDEO_STREAMING to "🎬 Video",
        TriggerCategory.GAMING to "🎮 Games",
        TriggerCategory.FOOD_DELIVERY to "🍔 Food",
        TriggerCategory.SHOPPING to "🛍️ Shopping",
        TriggerCategory.CUSTOM to "📱 Other"
    )

    val filteredApps = remember(apps, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) apps
        else apps.filter { it.category == selectedCategoryFilter }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, bottom = 90.dp)
        ) {
            // Top bar
            ObTopBar(step = 2, progress = 0.2f, onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(12.dp))
                ObQuestion(
                    label = "DISTRACTION APPS",
                    question = "Which apps pull you in?",
                    subtitle = "Select from apps installed on your phone."
                )
                if (selected.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("${selected.size} selected", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))

                // Category Chips Row
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryTabs) { (cat, label) ->
                        val isSelectedTab = selectedCategoryFilter == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelectedTab) AccentBlue.copy(alpha = 0.2f) else BgCardRaised,
                            border = BorderStroke(
                                1.dp,
                                if (isSelectedTab) AccentBlue else BorderDefault
                            ),
                            modifier = Modifier.clickable { selectedCategoryFilter = cat }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelectedTab) AccentBlue else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelectedTab) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps) { app ->
                    val isSel = selected.any { it.packageName == app.packageName }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFF1E293B) else BgCardRaised,
                        border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) BorderSelected else BorderDefault),
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(app) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                com.digitaldiscipline.spike.ui.components.AppIconImage(
                                    packageName = app.packageName,
                                    fallbackEmoji = app.icon,
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(app.displayName, color = if (isSel) TextPrimary else TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    val catLabel = when (app.category) {
                                        TriggerCategory.SOCIAL_MEDIA -> "Social Media"
                                        TriggerCategory.VIDEO_STREAMING -> "Video & Streaming"
                                        TriggerCategory.GAMING -> "Gaming"
                                        TriggerCategory.SHOPPING -> "Shopping"
                                        TriggerCategory.FOOD_DELIVERY -> "Food & Delivery"
                                        TriggerCategory.CUSTOM -> "App"
                                    }
                                    Text(catLabel, color = TextMuted, fontSize = 11.sp)
                                }
                            }
                            if (isSel) {
                                Box(
                                    modifier = Modifier.size(20.dp).clip(CircleShape).background(AccentBlue),
                                    contentAlignment = Alignment.Center
                                ) { Text("✓", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        // CTA
        ObCta(
            modifier = Modifier.align(Alignment.BottomCenter),
            label = "CONTINUE",
            canContinue = canContinue,
            onClick = onNext
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 3 — Time estimate
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob3TimeEstimate(
    selected: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean
) {
    val opts = listOf("Less than 1 hour", "1–2 hours", "2–3 hours", "3–5 hours", "5+ hours")

    ObScaffold(step = 3, progress = 0.3f, onBack = onBack, onNext = onNext, canContinue = canContinue, ctaLabel = "CONTINUE") {
        ObQuestion(
            label = "YOUR SCREEN TIME",
            question = "How much time do you think you spend on these apps?",
            subtitle = "Just your best guess."
        )
        Spacer(Modifier.height(24.dp))
        opts.forEach { opt ->
            ObSingleCard(text = opt, isSelected = opt == selected, onClick = { onSelect(opt) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 4 — Aha moment (lifetime projection)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob4AhaMoment(estimate: String, onBack: () -> Unit, onNext: () -> Unit) {
    val yearsText = when (estimate) {
        "Less than 1 hour" -> "2–3 years"
        "1–2 hours" -> "4–6 years"
        "2–3 hours" -> "6–9 years"
        "3–5 hours" -> "9–14 years"
        else -> "14+ years"
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(250); visible = true }

    ObScaffold(
        step = 4, progress = 0.4f, onBack = onBack, onNext = onNext,
        canContinue = true, ctaLabel = "I WANT TO CHANGE THIS"
    ) {
        Spacer(Modifier.height(20.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(700))) {
            Text("If nothing changed...", color = TextSecondary, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(900)) + slideInVertically(tween(900)) { 40 }) {
            Text(
                text = yearsText,
                color = AccentBlue,
                fontSize = 68.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 76.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(1000))) {
            Text(
                text = "could be spent on these apps over the rest of your life.",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )
        }

        Spacer(Modifier.height(28.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(1300))) {
            Text("Even a small change adds up.", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 5 — Reframe
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob5Reframe(onBack: () -> Unit, onNext: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { delay(200); phase = 1; delay(1100); phase = 2; delay(900); phase = 3 }

    ObScaffold(step = 5, progress = 0.5f, onBack = onBack, onNext = onNext, canContinue = true, ctaLabel = "SHOW ME HOW") {
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(phase >= 1, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { 30 }) {
            Text("You don't have to quit your apps.", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 36.sp)
        }

        Spacer(Modifier.height(20.dp))

        AnimatedVisibility(phase >= 2, enter = fadeIn(tween(700))) {
            Text("You just need a better interruption.", color = AccentBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))

        AnimatedVisibility(phase >= 2, enter = fadeIn(tween(900))) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BgCard,
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "IMPULSE" to AccentBlue,
                        "↓" to TextMuted,
                        "PAUSE" to AccentGreen,
                        "↓" to TextMuted,
                        "CHOOSE" to TextPrimary
                    ).forEach { (label, color) ->
                        Text(
                            text = label,
                            color = color,
                            fontSize = if (label == "↓") 16.sp else 22.sp,
                            fontWeight = if (label == "↓") FontWeight.Normal else FontWeight.Black,
                            letterSpacing = if (label == "↓") 0.sp else 2.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(phase >= 3, enter = fadeIn(tween(700))) {
            Text(
                text = "Digital Discipline creates a moment between opening an app and using it.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 6 — Choose interruption style (multi-select)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob6InterventionStyle(
    selectedCategories: List<InterventionCategory>,
    onToggle: (InterventionCategory) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean
) {
    val options = listOf(
        Triple(InterventionCategory.MOVEMENT, "💪", "MOVE" to "Push-ups, squats, movement"),
        Triple(InterventionCategory.UPPER_BODY, "🧗", "UPPER BODY" to "Pull-ups and upper body"),
        Triple(InterventionCategory.BREATHING, "🫁", "BREATHE" to "Quick breathing resets"),
        Triple(InterventionCategory.MEDITATION, "🧘", "RESET" to "Mindfulness and meditation"),
        Triple(InterventionCategory.YOGA_MOBILITY, "🧘‍♂️", "MOBILITY" to "Yoga and stretches"),
        Triple(InterventionCategory.PHYSICAL_RESET, "💧", "STEP AWAY" to "Water, walking, eyes away"),
        Triple(InterventionCategory.COGNITIVE, "🧠", "REFLECT" to "Quick cognitive challenges"),
        Triple(InterventionCategory.CREATIVE_FLOW, "🎨", "CREATE" to "Zen Enso, Haiku, Dexterity, Lateral thinking"),
        Triple(InterventionCategory.MINDFUL_PERSPECTIVE, "🔮", "PERSPECTIVE" to "Binaural soundscapes, Future Self, Stoic Tarot")
    )

    ObScaffold(step = 6, progress = 0.6f, onBack = onBack, onNext = onNext, canContinue = canContinue, ctaLabel = "CONTINUE") {
        ObQuestion(
            label = "YOUR INTERRUPTION STYLE",
            question = "When distraction hits, what should interrupt you?",
            subtitle = "All styles enabled by default. Tap to customize."
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedCategories.size} of ${options.size} selected",
                color = AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.clickable {
                    if (selectedCategories.size == options.size) onClearAll() else onSelectAll()
                }
            ) {
                Text(
                    text = if (selectedCategories.size == options.size) "Deselect All" else "Select All",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        options.forEach { (cat, emoji, titleDesc) ->
            val (title, desc) = titleDesc
            val isSel = selectedCategories.contains(cat)
            ObCategoryCard(emoji = emoji, title = title, subtitle = desc, isSelected = isSel, onClick = { onToggle(cat) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 7 — Micro-intervention
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob7MicroIntervention(
    done: Boolean,
    breathCount: Int,
    onBreath: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean
) {
    ObScaffold(
        step = 7, progress = 0.7f, onBack = onBack, onNext = onNext,
        canContinue = canContinue, ctaLabel = if (done) "CONTINUE" else "SKIP FOR NOW"
    ) {
        ObQuestion(
            label = "YOUR FIRST INTERRUPTION",
            question = "Let's try your first interruption.",
            subtitle = "Before distraction, there can be a pause."
        )
        Spacer(Modifier.height(28.dp))

        if (!done) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BgCard,
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🫁", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Take 3 slow breaths.", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text("Breathe in slowly, breathe out fully.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { i ->
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape)
                                    .background(if (i < breathCount) AccentBlue else Color.Transparent)
                                    .border(1.5.dp, if (i < breathCount) AccentBlue else TextMuted, CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    if (breathCount < 3) {
                        Button(
                            onClick = onBreath,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text(
                                text = if (breathCount == 0) "TAP TO BEGIN" else "BREATHE (${breathCount}/3)",
                                color = Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF064E3B).copy(alpha = 0.25f),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✓", fontSize = 48.sp, color = AccentGreen)
                    Spacer(Modifier.height(12.dp))
                    Text("That's the idea.", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("A small interruption gives you a chance to choose.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 8 — Earned access rule
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob8EarnedAccess(
    selected: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean
) {
    val opts = listOf(
        Triple(5, "5 minutes", "Strict — short controlled access"),
        Triple(10, "10 minutes", "Recommended — balanced approach"),
        Triple(15, "15 minutes", "Generous — relaxed access")
    )

    ObScaffold(step = 8, progress = 0.8f, onBack = onBack, onNext = onNext, canContinue = canContinue, ctaLabel = "CONTINUE") {
        ObQuestion(
            label = "EARNED ACCESS RULE",
            question = "How should earned access work?",
            subtitle = "Complete an intervention → earn intentional screen time."
        )
        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.5f),
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Complete an intervention → earn access time.\nAccess only lasts until your balance runs out.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        opts.forEach { (mins, label, desc) ->
            ObTwoLineCard(title = label, subtitle = desc, isSelected = mins == selected, onClick = { onSelect(mins) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 9 — Permissions
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob9Permissions(
    context: Context,
    isA11y: Boolean,
    isOverlay: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean
) {
    ObScaffold(
        step = 9, progress = 0.9f, onBack = onBack, onNext = onNext,
        canContinue = canContinue,
        ctaLabel = if (canContinue) "CONTINUE" else "WAITING FOR PERMISSIONS"
    ) {
        ObQuestion(
            label = "ONE LAST THING",
            question = "Digital Discipline can only help if it can step in at the right moment.",
            subtitle = null
        )
        Spacer(Modifier.height(24.dp))

        ObPermissionCard(
            icon = "♿",
            title = "Accessibility Protection",
            description = "Detects when a protected app is opened so Digital Discipline can show your chosen interruption.",
            privacyNote = "We do not read your messages, keystrokes, screen contents, or personal information.",
            isGranted = isA11y,
            buttonLabel = "ENABLE ACCESSIBILITY",
            onAction = { PermissionGuideOverlay.show(context) }
        )

        Spacer(Modifier.height(14.dp))

        ObPermissionCard(
            icon = "🪟",
            title = "Display Over Other Apps",
            description = "Allows Digital Discipline to appear when you need an interruption.",
            privacyNote = null,
            isGranted = isOverlay,
            buttonLabel = "ALLOW DISPLAY",
            onAction = {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Private by design. Your interventions are processed on your device.",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN 10 — Protection ready + atomic activation
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun Ob10Ready(
    appsCount: Int,
    categoriesCount: Int,
    rewardMinutes: Int,
    isActivating: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onActivate: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); visible = true }

    ObScaffold(
        step = 10, progress = 1f, onBack = onBack, onNext = onActivate,
        canContinue = !isActivating,
        ctaLabel = if (isActivating) "ACTIVATING..." else "START WITH INTENTION"
    ) {
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 24 }) {
            Column {
                // Glowing shield
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AccentBlue.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                        .border(1.dp, AccentBlue.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("🛡️", fontSize = 36.sp) }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Your interruption system is ready.",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 34.sp
                )

                Spacer(Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BgCard,
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ObSummaryRow("Protected apps", "$appsCount")
                        Spacer(Modifier.height(10.dp))
                        ObSummaryRow("Your interruption styles", "$categoriesCount")
                        Spacer(Modifier.height(10.dp))
                        ObSummaryRow("Earned access", "$rewardMinutes min per challenge")
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("The goal isn't to use Digital Discipline more.", color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(6.dp))
                Text("It's to need it less.", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(errorMessage, color = Color(0xFFFECACA), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// REUSABLE COMPOSABLES
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Standard scaffold for steps 1–10. Handles top bar, progress, scrollable content, and CTA.
 */
@Composable
private fun ObScaffold(
    step: Int,
    progress: Float,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean,
    ctaLabel: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "obProgress"
    )

    Box(modifier = Modifier.fillMaxSize().background(Bg0)) {
        // Top bar
        ObTopBar(step = step, progress = animProgress, onBack = onBack)

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, bottom = 88.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            content = content
        )

        // Bottom CTA
        ObCta(
            modifier = Modifier.align(Alignment.BottomCenter),
            label = ctaLabel,
            canContinue = canContinue,
            onClick = onNext
        )
    }
}

@Composable
private fun ObTopBar(step: Int, progress: Float, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BgCardRaised,
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.clickable { onBack() }
            ) {
                Text("← Back", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
            }
            Text("$step / 10", color = TextMuted, fontSize = 12.sp)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = AccentBlue,
            trackColor = BorderDefault
        )
    }
}

@Composable
private fun ObCta(
    modifier: Modifier = Modifier,
    label: String,
    canContinue: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Bg0.copy(alpha = 0.95f), Bg0)
                )
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = canContinue,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue,
                disabledContainerColor = AccentBlue.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text(
                text = label,
                color = if (canContinue) Color(0xFF0F172A) else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun ObQuestion(label: String, question: String, subtitle: String?) {
    Text(label, color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
    Text(question, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
    if (subtitle != null) {
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun ObSingleCard(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.015f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "cardScale"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF1E293B) else BgCardRaised,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) BorderSelected else BorderDefault),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape)
                    .background(if (isSelected) AccentBlue else Color.Transparent)
                    .border(1.5.dp, if (isSelected) AccentBlue else TextMuted, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0F172A)))
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = text,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ObTwoLineCard(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF1E293B) else BgCardRaised,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) BorderSelected else BorderDefault),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape)
                    .background(if (isSelected) AccentBlue else Color.Transparent)
                    .border(1.5.dp, if (isSelected) AccentBlue else TextMuted, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0F172A)))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (isSelected) TextPrimary else TextSecondary, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ObCategoryCard(emoji: String, title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF1E293B) else BgCardRaised,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) AccentBlue else BorderDefault),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AccentBlue.copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 20.sp) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, color = if (isSelected) TextPrimary else TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = TextMuted, fontSize = 12.sp)
                }
            }
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentBlue.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, AccentBlue)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✓", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("ACTIVE", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, TextMuted, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ObPermissionCard(
    icon: String,
    title: String,
    description: String,
    privacyNote: String?,
    isGranted: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isGranted) Color(0xFF064E3B).copy(alpha = 0.2f) else BgCardRaised,
        border = BorderStroke(1.dp, if (isGranted) AccentGreen.copy(alpha = 0.4f) else BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (isGranted) {
                    Surface(shape = RoundedCornerShape(8.dp), color = AccentGreen.copy(alpha = 0.15f)) {
                        Text("✓ Enabled", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(description, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            if (privacyNote != null) {
                Spacer(Modifier.height(8.dp))
                Text(privacyNote, color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
            }
            if (!isGranted) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(buttonLabel, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ObSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

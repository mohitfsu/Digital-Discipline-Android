package com.digitaldiscipline.spike.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.intervention.model.ValidationType
import com.digitaldiscipline.spike.ui.challenges.*
import com.digitaldiscipline.spike.ui.vision.CameraPoseWorkoutScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterventionCatalogPickerScreen(
    coroutineScope: CoroutineScope,
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allInterventions = remember { InterventionCatalog.getAllInterventions() }
    val savedEnabledIds by preferencesManager.enabledInterventionsFlow.collectAsState(initial = emptySet())

    // If user hasn't saved custom subset yet, all are active by default
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasInitialized by remember { mutableStateOf(false) }

    // Active Challenge Demo Preview Modal State
    var activeDemoChallenge by remember { mutableStateOf<InterventionDefinition?>(null) }
    var demoCompletedSuccess by remember { mutableStateOf(false) }

    // Camera permission for live pose demos
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission needed for live AI pose tracking.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(savedEnabledIds) {
        if (!hasInitialized) {
            selectedIds = if (savedEnabledIds.isEmpty()) {
                allInterventions.map { it.id }.toSet()
            } else {
                savedEnabledIds
            }
            hasInitialized = true
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateMapOf<InterventionCategory, Boolean>() }

    // By default expand all
    LaunchedEffect(Unit) {
        InterventionCategory.values().forEach {
            expandedCategories[it] = true
        }
    }

    val filteredInterventions = remember(searchQuery, allInterventions) {
        if (searchQuery.isBlank()) allInterventions
        else allInterventions.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.category.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedByCategory = remember(filteredInterventions) {
        filteredInterventions.groupBy { it.category }
    }

    Scaffold(
        containerColor = Color(0xFF090D16),
        topBar = {
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable { onNavigateBack() }
                            ) {
                                Text(
                                    text = "← Back",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "INTERVENTIONS",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "Active Friction Catalog",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    preferencesManager.setEnabledInterventions(selectedIds)
                                    val activeCategories = allInterventions
                                        .filter { selectedIds.contains(it.id) }
                                        .map { it.category.name }
                                        .toSet()
                                    preferencesManager.setEnabledCategories(activeCategories)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Saved ${selectedIds.size} active interventions", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Save (${selectedIds.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search 35 interventions (e.g. pushups, breathing, reading)...", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedIds.size} of ${allInterventions.size} Selected",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cross-select fitness, reading, breathing freely",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectedIds = allInterventions.map { it.id }.toSet()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Select All", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedIds = emptySet()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Clear", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Guidance Note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Digital Discipline adapts to what works for you. Enable the types of friction you enjoy — like combining Fitness with Reading or Breathing.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            InterventionCategory.values().forEach { category ->
                val categoryItems = groupedByCategory[category] ?: emptyList()
                if (categoryItems.isNotEmpty()) {
                    val isExpanded = expandedCategories[category] ?: true
                    val categorySelectedCount = categoryItems.count { selectedIds.contains(it.id) }
                    val isAllCategorySelected = categorySelectedCount == categoryItems.size

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, if (categorySelectedCount > 0) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Category Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategories[category] = !isExpanded
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (category) {
                                            InterventionCategory.MOVEMENT -> "🏃"
                                            InterventionCategory.UPPER_BODY -> "💪"
                                            InterventionCategory.BREATHING -> "🫁"
                                            InterventionCategory.MEDITATION -> "🧘"
                                            InterventionCategory.YOGA_MOBILITY -> "🧘‍♂️"
                                            InterventionCategory.PHYSICAL_RESET -> "💧"
                                            InterventionCategory.COGNITIVE -> "🧠"
                                            InterventionCategory.CREATIVE_FLOW -> "🎨"
                                            InterventionCategory.MINDFUL_PERSPECTIVE -> "🔮"
                                        },
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = formatCategoryTitle(category),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$categorySelectedCount of ${categoryItems.size} active",
                                            color = if (categorySelectedCount > 0) Color(0xFF34D399) else Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isAllCategorySelected) "Deselect All" else "Select All",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                selectedIds = if (isAllCategorySelected) {
                                                    selectedIds - categoryItems.map { it.id }.toSet()
                                                } else {
                                                    selectedIds + categoryItems.map { it.id }.toSet()
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isExpanded) "▲" else "▼",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categoryItems.forEach { item ->
                                        val isSelected = selectedIds.contains(item.id)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF131C2E).copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7).copy(alpha = 0.4f) else Color(0xFF1E293B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                                        },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(item.iconEmoji, fontSize = 22.sp)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = item.title,
                                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = item.description,
                                                            color = Color(0xFF64748B),
                                                            fontSize = 11.sp,
                                                            maxLines = 2
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        val durationOrReps = if (item.defaultReps > 0) "${item.defaultReps} reps" else "${item.defaultDurationSeconds}s"
                                                        Text(
                                                            text = "$durationOrReps • +${item.rewardSeconds / 60}m earned",
                                                            color = Color(0xFF38BDF8),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Try Demo Button
                                                    OutlinedButton(
                                                        onClick = {
                                                            demoCompletedSuccess = false
                                                            activeDemoChallenge = item
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text("🎯 Try Demo", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            selectedIds = if (checked) selectedIds + item.id else selectedIds - item.id
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = Color(0xFF0284C7),
                                                            uncheckedColor = Color(0xFF475569)
                                                        )
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
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIVE CHALLENGE DEMO / TEST STUDIO DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    val activeChallenge = activeDemoChallenge
    if (activeChallenge != null) {
        val isCameraWorkout = isCameraWorkoutChallenge(activeChallenge)

        LaunchedEffect(activeChallenge) {
            if (isCameraWorkout && !hasCameraPermission) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        Dialog(
            onDismissRequest = { activeDemoChallenge = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            if (isCameraWorkout && hasCameraPermission) {
                // Fullscreen Live AI Camera Tracking Studio
                CameraPoseWorkoutScreen(
                    exerciseId = activeChallenge.id,
                    exerciseTitle = activeChallenge.title,
                    targetReps = if (activeChallenge.defaultReps > 0) activeChallenge.defaultReps else 10,
                    targetHoldSeconds = if (activeChallenge.defaultDurationSeconds > 0) activeChallenge.defaultDurationSeconds else 30,
                    onComplete = {
                        demoCompletedSuccess = true
                        selectedIds = selectedIds + activeChallenge.id
                        coroutineScope.launch {
                            preferencesManager.setEnabledInterventions(selectedIds)
                            Toast.makeText(context, "🎉 ${activeChallenge.title} Completed & Added to Plan!", Toast.LENGTH_SHORT).show()
                        }
                        activeDemoChallenge = null
                    },
                    onSwitchChallenge = { newChallengeId ->
                        val newDef = InterventionCatalog.getIntervention(newChallengeId)
                        if (newDef != null) {
                            activeDemoChallenge = newDef
                        }
                    },
                    onDismiss = { activeDemoChallenge = null }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF090D16).copy(alpha = 0.96f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Modal Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "🎯 LIVE DEMO TEST",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = activeChallenge.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { activeDemoChallenge = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Challenge Runner Container
                            when {
                                isCameraWorkout -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📷 Camera Permission Needed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Camera access is needed for live AI pose tracking & rep counting for ${activeChallenge.title}.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                        ) {
                                            Text("ENABLE CAMERA NOW", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                            // Interactive Games & Cognitive Puzzles
                            activeChallenge.id == "IMAGE_PUZZLE_3X3" -> {
                                ImageTilePuzzleGame(timeLimitSeconds = 30, onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "HANGMAN_CLASSIC" -> {
                                HangmanWordGame(timeLimitSeconds = 45, onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "MATH_SPRINT" || activeChallenge.id == "SIMPLE_MATH" -> {
                                MathSprintGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "MEMORY_MATRIX" || activeChallenge.id == "MEMORY_SEQUENCE" -> {
                                MemoryMatrixGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "STROOP_TEST" -> {
                                StroopChallengeGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "ZEN_ENSO_CANVAS" -> {
                                ZenCanvasEnsoGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "SCAVENGER_HUNT" -> {
                                RealWorldScavengerGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "HAND_MUDRA_DEXTERITY" -> {
                                HandMudraDexterityGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "DIVERGENT_THINKING" -> {
                                DivergentThinkingGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "HAIKU_CRAFTER" -> {
                                HaikuCrafterGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "BINAURAL_SOUNDSCAPE" -> {
                                BinauralSoundscapeGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "FUTURE_SELF_CAPSULE" -> {
                                FutureSelfCapsuleGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "STOIC_TAROT_DECIDER" -> {
                                StoicTarotDeciderGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "MINDFUL_READING" -> {
                                MindfulReadingGame(onSuccess = { demoCompletedSuccess = true })
                            }
                            activeChallenge.id == "INTENTIONAL_WRITING" -> {
                                IntentionalityFrictionGame(targetAppName = "Instagram", onSuccess = { demoCompletedSuccess = true })
                            }

                            // Breathing & Mindfulness Paced Reset Runner
                            activeChallenge.category == InterventionCategory.BREATHING || activeChallenge.category == InterventionCategory.MEDITATION -> {
                                MindfulBreathingDemoRunner(
                                    intervention = activeChallenge,
                                    onComplete = { demoCompletedSuccess = true }
                                )
                            }

                            // General Interactive Prompt & Reflection Runner
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                    Text(activeChallenge.iconEmoji, fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(activeChallenge.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(activeChallenge.instructions, color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { demoCompletedSuccess = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("COMPLETE DEMO TRIAL", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Success Actions Banner
                        if (demoCompletedSuccess) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF064E3B),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🎉 Challenge Tested Successfully!",
                                        color = Color(0xFF34D399),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Would you like to enable this challenge in your active plan?",
                                        color = Color(0xFFD1FAE5),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            selectedIds = selectedIds + activeChallenge.id
                                            coroutineScope.launch {
                                                preferencesManager.setEnabledInterventions(selectedIds)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "✓ Enabled ${activeChallenge.title}!", Toast.LENGTH_SHORT).show()
                                                    activeDemoChallenge = null
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("✓ ENABLE IN MY ACTIVE PLAN", fontWeight = FontWeight.Black, color = Color(0xFF064E3B))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}

@Composable
private fun MindfulBreathingDemoRunner(
    intervention: InterventionDefinition,
    onComplete: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(intervention.defaultDurationSeconds.coerceAtLeast(15)) }
    var phaseText by remember { mutableStateOf("Breathe In Slowly...") }
    var isRunning by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && isRunning) {
            delay(1000L)
            secondsLeft--
            phaseText = when ((secondsLeft % 8)) {
                in 4..7 -> "Inhale Deeply (Expand Belly)..."
                in 2..3 -> "Hold Breath & Mindful Stillness..."
                else -> "Exhale Fully & Release Tension..."
            }
        }
        if (secondsLeft <= 0) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🫁 MINDFUL PACING", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Animated Breathing Bubble
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                .border(2.dp, Color(0xFF38BDF8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size((90 * scale).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8).copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${secondsLeft}s",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(phaseText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(intervention.instructions, color = Color(0xFF94A3B8), fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

private fun formatCategoryTitle(category: InterventionCategory): String {
    return when (category) {
        InterventionCategory.MOVEMENT -> "Movement & Fitness"
        InterventionCategory.UPPER_BODY -> "Upper Body & Posture"
        InterventionCategory.BREATHING -> "Breathing Exercises"
        InterventionCategory.MEDITATION -> "Mindfulness & Meditation"
        InterventionCategory.YOGA_MOBILITY -> "Yoga & Mobility"
        InterventionCategory.PHYSICAL_RESET -> "Physical Reset"
        InterventionCategory.COGNITIVE -> "Cognitive & Reading"
        InterventionCategory.CREATIVE_FLOW -> "Creative Flow & Expression"
        InterventionCategory.MINDFUL_PERSPECTIVE -> "Mindful Perspective & Audio"
    }
}

private fun isCameraWorkoutChallenge(item: InterventionDefinition): Boolean {
    if (item.id == "SCAVENGER_HUNT") return false
    return item.category == InterventionCategory.MOVEMENT ||
           item.category == InterventionCategory.UPPER_BODY ||
           item.category == InterventionCategory.YOGA_MOBILITY ||
           item.validationType == ValidationType.CAMERA_VALIDATED ||
           item.validationType == ValidationType.SENSOR_VALIDATED ||
           item.id in setOf(
               "PUSH_UPS", "SQUATS", "WALL_SIT", "PLANK", "CALF_RAISES", "JUMPING_JACKS",
               "LUNGES", "HIGH_KNEES", "BURPEES", "MOUNTAIN_CLIMBERS", "SIT_TO_STAND",
               "YOGA_TREE", "YOGA_WARRIOR", "YOGA_CHAIR", "YOGA_DOWNWARD_DOG",
               "NECK_SHOULDER_ROLLS", "WRIST_FINGER_STRETCH"
           )
}

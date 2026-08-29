package com.digitaldiscipline.spike.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.digitaldiscipline.spike.data.local.entities.UserMode
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.wallet.EarnResult
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.security.ParentPinManager
import com.digitaldiscipline.spike.security.PinVerificationResult
import com.digitaldiscipline.spike.sync.SyncManager
import com.digitaldiscipline.spike.ui.challenges.*
import com.digitaldiscipline.spike.ui.vision.CameraPoseWorkoutScreen
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    preferencesManager: PreferencesManager,
    walletService: EarnedTimeWalletService,
    pinManager: ParentPinManager,
    syncManager: SyncManager,
    isA11yActive: Boolean,
    isOverlayActive: Boolean,
    onNavigateToPairing: () -> Unit,
    onOpenParentAdmin: () -> Unit,
    onSwitchMode: (UserMode) -> Unit
) {
    val pairedFamilyId by preferencesManager.pairedFamilyIdFlow.collectAsState(initial = null)
    val pairedChildName by preferencesManager.pairedChildNameFlow.collectAsState(initial = "Child")
    val isPaired = !pairedFamilyId.isNullOrBlank()
    val lastSync by preferencesManager.lastPolicySyncFlow.collectAsState(initial = 0L)

    val autoBlockGames by preferencesManager.autoBlockGamesFlow.collectAsState(initial = true)
    val autoBlockSocial by preferencesManager.autoBlockSocialFlow.collectAsState(initial = true)
    val autoBlockStreaming by preferencesManager.autoBlockStreamingFlow.collectAsState(initial = true)

    // Wallet flow for earned time balance
    val wallet by walletService.getWalletFlow("wallet_self").collectAsState(initial = null)
    val availableSeconds = wallet?.availableSeconds ?: 0
    val availableMinutes = availableSeconds / 60

    val isProtectionActive = isA11yActive && isOverlayActive

    // PIN Gate Dialog
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val hasConfiguredChallenges by preferencesManager.hasConfiguredChallengesFlow.collectAsState(initial = true)
    var showChildCatalogSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hasConfiguredChallenges) {
        if (!hasConfiguredChallenges) {
            showChildCatalogSetupDialog = true
        }
    }

    // Interactive "Earn Screen Time Now" Studio Dialog
    var showEarnStudioDialog by remember { mutableStateOf(false) }
    var activeChallengeItem by remember { mutableStateOf<InterventionDefinition?>(null) }

    val allCatalogInterventions = remember { InterventionCatalog.getAllInterventions() }
    var selectedCategoryFilter by remember { mutableStateOf<InterventionCategory?>(null) }
    val filteredInterventions = remember(selectedCategoryFilter) {
        if (selectedCategoryFilter == null) allCatalogInterventions
        else allCatalogInterventions.filter { it.category == selectedCategoryFilter }
    }

    // Camera Permission for movement challenges
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
            Toast.makeText(context, "Camera permission needed for live skeletal tracking.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(activeChallengeItem) {
        val challenge = activeChallengeItem
        if (challenge != null && (challenge.category == InterventionCategory.MOVEMENT || challenge.category == InterventionCategory.UPPER_BODY || challenge.category == InterventionCategory.YOGA_MOBILITY)) {
            if (!hasCameraPermission) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHILD FOCUS SHIELD",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isPaired) "Device: $pairedChildName" else "Child Device",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isProtectionActive) Color(0xFF059669).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (isProtectionActive) Color(0xFF10B981) else Color(0xFFEF4444)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (isProtectionActive) "🛡️" else "⚠️", fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Earned Screen Time Wallet Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8))), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AVAILABLE EARNED TIME",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$availableMinutes",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MINS",
                        color = Color(0xFF94A3B8),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = if (availableMinutes > 0) "You have intentional access to play!" else "Complete a challenge to unlock gaming & social time.",
                    color = if (availableMinutes > 0) Color(0xFFA7F3D0) else Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showEarnStudioDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "▶ EARN SCREEN TIME NOW (+10m)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Active Rules & Protection Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PARENTAL RULES (EARN MODE)",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Rule item 1: Games
                RuleStatusRow("🎮", "Gaming Apps (Free Fire, BGMI, etc.)", autoBlockGames)
                Spacer(modifier = Modifier.height(8.dp))

                // Rule item 2: Social
                RuleStatusRow("📸", "Social & Short Videos (Reels, Shorts)", autoBlockSocial)
                Spacer(modifier = Modifier.height(8.dp))

                // Rule item 3: Streaming
                RuleStatusRow("🎬", "Streaming & Entertainment (YouTube, OTT)", autoBlockStreaming)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Pairing & Sync Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isPaired) "☁️" else "🔗", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isPaired) "Paired with Parent" else "Not Paired",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val timeStr = if (lastSync > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSync)) else "Recent"
                            Text(
                                text = if (isPaired) "Last synced: $timeStr" else "Enter pairing code",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (isPaired) {
                                syncManager.triggerImmediateSync()
                                Toast.makeText(context, "Synced latest rules from Parent.", Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigateToPairing()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(if (isPaired) "🔄 Sync" else "🔗 Pair", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Discreet Parent Admin Settings Gateway at Bottom
        OutlinedButton(
            onClick = {
                enteredPin = ""
                pinError = null
                showPinDialog = true
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Parent Lock", modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Parent Settings (PIN Protected)", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EARN SCREEN TIME STUDIO MODAL (Lists all 49 challenges for child to play)
    // ─────────────────────────────────────────────────────────────────────────
    if (showEarnStudioDialog) {
        Dialog(
            onDismissRequest = { showEarnStudioDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16).copy(alpha = 0.95f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🎮 Earn Screen Time Studio",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Complete any challenge to deposit +10 minutes!",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { showEarnStudioDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Tabs
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == null,
                                    onClick = { selectedCategoryFilter = null },
                                    label = { Text("All (${allCatalogInterventions.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A)
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == InterventionCategory.COGNITIVE,
                                    onClick = { selectedCategoryFilter = InterventionCategory.COGNITIVE },
                                    label = { Text("🧩 Puzzles & Math") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A)
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == InterventionCategory.MOVEMENT,
                                    onClick = { selectedCategoryFilter = InterventionCategory.MOVEMENT },
                                    label = { Text("💪 Movement & AI") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A)
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == InterventionCategory.BREATHING,
                                    onClick = { selectedCategoryFilter = InterventionCategory.BREATHING },
                                    label = { Text("🫁 Breathing") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredInterventions) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(item.iconEmoji, fontSize = 24.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text(item.description, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1)
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (item.category == InterventionCategory.MOVEMENT && !hasCameraPermission) {
                                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                                activeChallengeItem = item
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("PLAY ▶", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
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

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIVE CHALLENGE PLAY DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    val activeChallenge = activeChallengeItem
    if (activeChallenge != null) {
        Dialog(
            onDismissRequest = { activeChallengeItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16).copy(alpha = 0.95f))
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
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 ${activeChallenge.title}",
                                color = Color(0xFF38BDF8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { activeChallengeItem = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Challenge Runner
                        when {
                            activeChallenge.id == "IMAGE_PUZZLE_3X3" -> {
                                ImageTilePuzzleGame(
                                    timeLimitSeconds = 30,
                                    onSuccess = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val res = walletService.earnTime(amountSeconds = 300, source = "PUZZLE_CHALLENGE")
                                            withContext(Dispatchers.Main) {
                                                when (res) {
                                                    is EarnResult.Success -> Toast.makeText(context, "🎉 +${res.earnedSeconds / 60}m Added! Total: ${res.newBalanceSeconds / 60}m (15m max)", Toast.LENGTH_SHORT).show()
                                                    is EarnResult.CapReached -> Toast.makeText(context, "⚠️ ${res.reason}", Toast.LENGTH_LONG).show()
                                                    else -> {}
                                                }
                                                activeChallengeItem = null
                                                showEarnStudioDialog = false
                                            }
                                        }
                                    }
                                )
                            }
                            activeChallenge.id == "HANGMAN_CLASSIC" -> {
                                HangmanWordGame(
                                    timeLimitSeconds = 45,
                                    onSuccess = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val res = walletService.earnTime(amountSeconds = 300, source = "HANGMAN_CHALLENGE")
                                            withContext(Dispatchers.Main) {
                                                when (res) {
                                                    is EarnResult.Success -> Toast.makeText(context, "🎉 +${res.earnedSeconds / 60}m Added! Total: ${res.newBalanceSeconds / 60}m (15m max)", Toast.LENGTH_SHORT).show()
                                                    is EarnResult.CapReached -> Toast.makeText(context, "⚠️ ${res.reason}", Toast.LENGTH_LONG).show()
                                                    else -> {}
                                                }
                                                activeChallengeItem = null
                                                showEarnStudioDialog = false
                                            }
                                        }
                                    }
                                )
                            }
                            activeChallenge.id == "MATH_SPRINT" || activeChallenge.id == "SIMPLE_MATH" -> {
                                MathSprintGame(
                                    onSuccess = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val res = walletService.earnTime(amountSeconds = 300, source = "MATH_CHALLENGE")
                                            withContext(Dispatchers.Main) {
                                                when (res) {
                                                    is EarnResult.Success -> Toast.makeText(context, "🎉 +${res.earnedSeconds / 60}m Added! Total: ${res.newBalanceSeconds / 60}m (15m max)", Toast.LENGTH_SHORT).show()
                                                    is EarnResult.CapReached -> Toast.makeText(context, "⚠️ ${res.reason}", Toast.LENGTH_LONG).show()
                                                    else -> {}
                                                }
                                                activeChallengeItem = null
                                                showEarnStudioDialog = false
                                            }
                                        }
                                    }
                                )
                            }
                            activeChallenge.category == InterventionCategory.MOVEMENT || activeChallenge.category == InterventionCategory.UPPER_BODY || activeChallenge.category == InterventionCategory.YOGA_MOBILITY -> {
                                if (hasCameraPermission) {
                                    CameraPoseWorkoutScreen(
                                        exerciseId = activeChallenge.id,
                                        exerciseTitle = activeChallenge.title,
                                        targetReps = if (activeChallenge.defaultReps > 0) activeChallenge.defaultReps else 10,
                                        targetHoldSeconds = if (activeChallenge.defaultDurationSeconds > 0) activeChallenge.defaultDurationSeconds else 30,
                                        onComplete = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val res = walletService.earnTime(amountSeconds = 300, source = "MOVEMENT_CHALLENGE")
                                                withContext(Dispatchers.Main) {
                                                    when (res) {
                                                        is EarnResult.Success -> Toast.makeText(context, "🎉 +${res.earnedSeconds / 60}m Added! Total: ${res.newBalanceSeconds / 60}m (15m max)", Toast.LENGTH_SHORT).show()
                                                        is EarnResult.CapReached -> Toast.makeText(context, "⚠️ ${res.reason}", Toast.LENGTH_LONG).show()
                                                        else -> {}
                                                    }
                                                    activeChallengeItem = null
                                                    showEarnStudioDialog = false
                                                }
                                            }
                                        },
                                        onSwitchChallenge = { newChallengeId ->
                                            val newDef = InterventionCatalog.getIntervention(newChallengeId)
                                            if (newDef != null) {
                                                activeChallengeItem = newDef
                                            }
                                        },
                                        onDismiss = { activeChallengeItem = null }
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📷 Camera Permission Needed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Camera is needed for live pose counting.", color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                        ) {
                                            Text("GRANT CAMERA ACCESS", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                    Text(activeChallenge.iconEmoji, fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(activeChallenge.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(activeChallenge.instructions, color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val res = walletService.earnTime(amountSeconds = 300, source = "HABIT_CHALLENGE")
                                                withContext(Dispatchers.Main) {
                                                    when (res) {
                                                        is EarnResult.Success -> Toast.makeText(context, "🎉 +${res.earnedSeconds / 60}m Added! Total: ${res.newBalanceSeconds / 60}m (15m max)", Toast.LENGTH_SHORT).show()
                                                        is EarnResult.CapReached -> Toast.makeText(context, "⚠️ ${res.reason}", Toast.LENGTH_LONG).show()
                                                        else -> {}
                                                    }
                                                    activeChallengeItem = null
                                                    showEarnStudioDialog = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("COMPLETE & EARN +5m", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARENT PIN AUTHORIZATION DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Parent PIN Required", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your 4-digit Parent PIN to open settings or manage policies:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) enteredPin = it },
                        placeholder = { Text("••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = pinManager.verifyPin(enteredPin)
                        if (result is PinVerificationResult.Success) {
                            showPinDialog = false
                            onOpenParentAdmin()
                        } else {
                            pinError = "Incorrect Parent PIN"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHILD FAVORITE CHALLENGE SETUP POPUP DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    if (showChildCatalogSetupDialog) {
        AlertDialog(
            onDismissRequest = {
                coroutineScope.launch {
                    preferencesManager.setHasConfiguredChallenges(true)
                }
                showChildCatalogSetupDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎮", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Your Favorite Challenges!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Whenever you want screen time, which challenges do you want to play to earn +5 minutes?",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    // Option 1: Puzzles & Word Games
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val ids = setOf("IMAGE_PUZZLE_3X3", "HANGMAN_CLASSIC", "MATH_SPRINT", "MEMORY_MATRIX")
                                    preferencesManager.setEnabledInterventions(ids)
                                    preferencesManager.setHasConfiguredChallenges(true)
                                }
                                showChildCatalogSetupDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🧩", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Puzzles & Word Games", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Picture Puzzle, Hangman Word Guess & Math Sprint", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    // Option 2: Active Fitness & Jumps
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val ids = setOf("SQUATS", "CALF_RAISES", "JUMPING_JACKS", "PLANK")
                                    preferencesManager.setEnabledInterventions(ids)
                                    preferencesManager.setHasConfiguredChallenges(true)
                                }
                                showChildCatalogSetupDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Fun Fitness & Movement", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Jumping Jacks, Squats & Calf Raises with camera", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    // Option 3: Everything
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val ids = setOf("IMAGE_PUZZLE_3X3", "HANGMAN_CLASSIC", "SCAVENGER_HUNT", "SQUATS", "CALF_RAISES", "MATH_SPRINT")
                                    preferencesManager.setEnabledInterventions(ids)
                                    preferencesManager.setHasConfiguredChallenges(true)
                                }
                                showChildCatalogSetupDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🌟", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("All-in-One Adventure Mix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Scavenger Hunt, Hangman, Puzzles & Workouts", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setHasConfiguredChallenges(true)
                        }
                        showChildCatalogSetupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("START EARNING TIME", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setHasConfiguredChallenges(true)
                        }
                        showChildCatalogSetupDialog = false
                        showEarnStudioDialog = true
                    }
                ) {
                    Text("Browse Studio", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}

@Composable
private fun RuleStatusRow(icon: String, title: String, isBlocked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = if (isBlocked) "⛔ Locked (Earn Mode)" else "✓ Allowed",
            color = if (isBlocked) Color(0xFFF87171) else Color(0xFF34D399),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

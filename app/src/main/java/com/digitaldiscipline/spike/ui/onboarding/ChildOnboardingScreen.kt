package com.digitaldiscipline.spike.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.digitaldiscipline.spike.behaviour.templates.DistractionAppRecommendation
import com.digitaldiscipline.spike.behaviour.templates.GoalTemplateRepository
import com.digitaldiscipline.spike.cloud.PairingManager
import com.digitaldiscipline.spike.cloud.PairingResult
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.TriggerCategory
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.security.ParentPinManager
import com.digitaldiscipline.spike.sync.SyncManager
import com.digitaldiscipline.spike.ui.challenges.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChallengeItem(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val ageSuitability: String,
    val isEnabledByDefault: Boolean = true
)

@Composable
fun ChildOnboardingScreen(
    context: Context,
    pairingManager: PairingManager,
    syncManager: SyncManager,
    policyRepository: PolicyRepository,
    preferencesManager: PreferencesManager,
    pinManager: ParentPinManager,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    isUsageStatsGranted: Boolean,
    onComplete: () -> Unit,
    onBackToModeSelect: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(1) }

    // Step 1 State: Pairing Code
    var pairingCode by remember { mutableStateOf("") }
    var isPairing by remember { mutableStateOf(false) }
    var pairingError by remember { mutableStateOf<String?>(null) }
    var pairedChildName by remember { mutableStateOf("Child Device") }

    // Step 3 State: App Scanner
    val installedApps = remember {
        try {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveList = pm.queryIntentActivities(launcherIntent, 0)
            val myPkg = context.packageName
            val apps = resolveList.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == myPkg) null
                else {
                    val label = ri.loadLabel(pm).toString()
                    val category = GoalTemplateRepository.categorizeApp(pkg)
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
            }.distinctBy { it.packageName }.sortedBy { it.displayName.lowercase() }

            val common = GoalTemplateRepository.getAllDistractionRecommendations()
            val combined = mutableListOf<DistractionAppRecommendation>()
            combined.addAll(apps)
            common.forEach { c -> if (combined.none { it.packageName == c.packageName }) combined.add(c) }
            combined
        } catch (_: Exception) {
            GoalTemplateRepository.getAllDistractionRecommendations()
        }
    }

    val selectedApps = remember {
        mutableStateListOf<DistractionAppRecommendation>().apply {
            // Auto-select gaming, social and streaming by default
            addAll(installedApps.filter {
                it.category == TriggerCategory.GAMING ||
                it.category == TriggerCategory.SOCIAL_MEDIA ||
                it.category == TriggerCategory.VIDEO_STREAMING
            })
        }
    }

    // Step 4 State: Interactive Challenge Studio
    val availableChallenges = remember {
        listOf(
            ChallengeItem("IMAGE_PUZZLE_3X3", "🧩", "9-Piece Image Puzzle", "30-sec sliding puzzle • Resets if time expires", "All Ages", true),
            ChallengeItem("THREE_BREATHS", "🫁", "3 Mindful Breaths", "Calming breathing reset before screen time", "All Ages", true),
            ChallengeItem("MATH_SPRINT", "🧠", "Mental Math Quiz", "Quick arithmetic questions to sharpen focus", "Age 7+", true),
            ChallengeItem("MOVEMENT_10", "💪", "10 Jumping Jacks / Squats", "Physical movement break before digital play", "Age 6+", true),
            ChallengeItem("PHYSICAL_RESET", "💧", "Physical & Eye Reset", "Drink water and look away from screen", "All Ages", true)
        )
    }
    val enabledChallengeIds = remember { mutableStateListOf("IMAGE_PUZZLE_3X3", "THREE_BREATHS", "MATH_SPRINT", "MOVEMENT_10", "PHYSICAL_RESET") }
    var activeDemoChallengeId by remember { mutableStateOf<String?>(null) }

    // Step 5 State: Parent PIN
    var parentPinText by remember { mutableStateOf("") }
    var parentPinConfirmText by remember { mutableStateOf("") }
    var parentPinError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress Bar
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CHILD PHONE SETUP",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { step / 5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Step $step of 5: " + when (step) {
                        1 -> "Pair with Parent"
                        2 -> "Shield Permissions"
                        3 -> "Scanned Apps & Rules"
                        4 -> "Interactive Challenge Studio"
                        5 -> "Parent PIN Seal"
                        else -> ""
                    },
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step Body Content
            when (step) {
                // ─────────────────────────────────────────────────────────────
                // STEP 1: PAIRING CODE
                // ─────────────────────────────────────────────────────────────
                1 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔗", fontSize = 34.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Enter 6-Digit Pairing Code",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Generate the code from the Parent phone under '🔑 GENERATE PAIR CODE'.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pairingCode = it },
                            placeholder = { Text("000000", fontFamily = FontFamily.Monospace, letterSpacing = 8.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                letterSpacing = 8.sp,
                                color = Color(0xFF38BDF8)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pairingError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = pairingError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("🔒 1-Tap Secure Handshake", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pairing links this phone to your family dashboard. All rules run 100% locally offline.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────
                // STEP 2: SHIELD PERMISSIONS
                // ─────────────────────────────────────────────────────────────
                2 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🛡️", fontSize = 34.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Enable Shield Permissions",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "These 3 Android permissions allow the app to enforce parental rules and display block screens.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Permission 1: Accessibility
                        PermissionCard(
                            number = "1",
                            title = "Accessibility Protection",
                            desc = "Required to detect when restricted games or social apps open in real-time.",
                            isGranted = isAccessibilityGranted,
                            onGrant = { PermissionGuideOverlay.show(context) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Permission 2: Display Over Other Apps
                        PermissionCard(
                            number = "2",
                            title = "Display Over Other Apps (Overlay)",
                            desc = "Allows Digital Discipline to show block screens and pause challenges.",
                            isGranted = isOverlayGranted,
                            onGrant = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    )
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Permission 3: Usage Access
                        PermissionCard(
                            number = "3",
                            title = "Usage Access",
                            desc = "Enables daily screen time tracking and syncs analytics to Parent Dashboard.",
                            isGranted = isUsageStatsGranted,
                            onGrant = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        )
                    }
                }

                // ─────────────────────────────────────────────────────────────
                // STEP 3: INSTALLED APPS SCANNER
                // ─────────────────────────────────────────────────────────────
                3 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Scanned Apps on This Device",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-detected ${selectedApps.size} entertainment & gaming apps to protect:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(installedApps) { app ->
                                    val isSelected = selectedApps.any { it.packageName == app.packageName }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF090D16))
                                            .clickable {
                                                if (isSelected) selectedApps.removeAll { it.packageName == app.packageName }
                                                else selectedApps.add(app)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(app.icon, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(app.displayName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = when (app.category) {
                                                    TriggerCategory.GAMING -> "🎮 Gaming"
                                                    TriggerCategory.SOCIAL_MEDIA -> "💬 Social Media"
                                                    TriggerCategory.VIDEO_STREAMING -> "▶️ Video Streaming"
                                                    else -> "📱 App"
                                                },
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (!selectedApps.any { it.packageName == app.packageName }) selectedApps.add(app)
                                                } else {
                                                    selectedApps.removeAll { it.packageName == app.packageName }
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────
                // STEP 4: INTERACTIVE CHALLENGE STUDIO (LIVE DEMO)
                // ─────────────────────────────────────────────────────────────
                4 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Interactive Challenge Studio",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Test and choose which challenges your child can complete to earn screen time:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        availableChallenges.forEach { challenge ->
                            val isEnabled = enabledChallengeIds.contains(challenge.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .border(
                                        1.dp,
                                        if (isEnabled) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF1E293B),
                                        RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(challenge.emoji, fontSize = 24.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(challenge.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Text(challenge.subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            }
                                        }

                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (!enabledChallengeIds.contains(challenge.id)) enabledChallengeIds.add(challenge.id)
                                                } else {
                                                    enabledChallengeIds.remove(challenge.id)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF38BDF8)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Suitability: ${challenge.ageSuitability}",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Button(
                                            onClick = { activeDemoChallengeId = challenge.id },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("▶ TRY LIVE DEMO", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────
                // STEP 5: PARENT PIN SEALING
                // ─────────────────────────────────────────────────────────────
                5 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF059669).copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔒", fontSize = 34.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Seal with Master Parent PIN",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter the 4-digit PIN you configured on your Parent phone to lock this device's rules.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = parentPinText,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) parentPinText = it },
                            label = { Text("Enter 4-Digit Parent PIN") },
                            placeholder = { Text("••••") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = parentPinConfirmText,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) parentPinConfirmText = it },
                            label = { Text("Confirm 4-Digit Parent PIN") },
                            placeholder = { Text("••••") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = parentPinError != null,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (parentPinError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = parentPinError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("🛡️ Tamper-Proofing Notice", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Once sealed, all settings, bypass attempts, and rule changes require your Parent PIN.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Navigation Buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("Back", color = Color(0xFF94A3B8))
                    }
                } else {
                    OutlinedButton(
                        onClick = onBackToModeSelect,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }

                Button(
                    onClick = {
                        when (step) {
                            1 -> {
                                if (pairingCode.length == 6) {
                                    isPairing = true
                                    pairingError = null
                                    coroutineScope.launch {
                                        val res = pairingManager.redeemPairingCode(pairingCode)
                                        isPairing = false
                                        when (res) {
                                            is PairingResult.Success -> {
                                                pairedChildName = res.childName
                                                Toast.makeText(context, "Pairing handshake successful!", Toast.LENGTH_SHORT).show()
                                                step = 2
                                            }
                                            is PairingResult.InvalidCode -> pairingError = res.message
                                            is PairingResult.ExpiredCode -> pairingError = res.message
                                            is PairingResult.AlreadyUsed -> pairingError = res.message
                                            is PairingResult.Error -> pairingError = res.message
                                        }
                                    }
                                } else {
                                    pairingError = "Please enter the complete 6-digit code."
                                }
                            }
                            2 -> {
                                if (!isAccessibilityGranted || !isOverlayGranted) {
                                    Toast.makeText(context, "Please enable Accessibility and Draw Over Apps to continue.", Toast.LENGTH_SHORT).show()
                                } else {
                                    step = 3
                                }
                            }
                            3 -> {
                                // Save selected apps as EARN rules in PolicyRepository
                                coroutineScope.launch(Dispatchers.IO) {
                                    selectedApps.forEach { app ->
                                        policyRepository.saveRule(
                                            AppRuleEntity(
                                                packageName = app.packageName,
                                                appDisplayName = app.displayName,
                                                mode = RuleMode.EARN,
                                                isEnabled = true,
                                                unlockDurationSeconds = 600
                                            )
                                        )
                                    }
                                }
                                step = 4
                            }
                            4 -> {
                                if (enabledChallengeIds.isEmpty()) {
                                    Toast.makeText(context, "Please enable at least 1 challenge.", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        preferencesManager.setEnabledInterventions(enabledChallengeIds.toSet())
                                    }
                                    step = 5
                                }
                            }
                            5 -> {
                                if (parentPinText.length < 4) {
                                    parentPinError = "PIN must be 4 digits"
                                } else if (parentPinText != parentPinConfirmText) {
                                    parentPinError = "PINs do not match"
                                } else {
                                    pinManager.setPin(parentPinText)
                                    coroutineScope.launch(Dispatchers.IO) {
                                        preferencesManager.setUserMode(com.digitaldiscipline.spike.data.local.entities.UserMode.CHILD.name)
                                        preferencesManager.setDeviceRole("CHILD_DEVICE")
                                        preferencesManager.setOnboardingCompleted(true)
                                        syncManager.triggerImmediateSync()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Child Phone protection sealed!", Toast.LENGTH_LONG).show()
                                            onComplete()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(start = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (step == 5) Color(0xFF059669) else Color(0xFF0284C7)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isPairing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = when (step) {
                                1 -> "Verify & Pair"
                                5 -> "Seal & Finish"
                                else -> "Continue"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Interactive Demo Pop-Up Dialog
    if (activeDemoChallengeId != null) {
        Dialog(
            onDismissRequest = { activeDemoChallengeId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16).copy(alpha = 0.95f))
                    .padding(20.dp),
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
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎮 Live Challenge Demo",
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { activeDemoChallengeId = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        when (activeDemoChallengeId) {
                            "IMAGE_PUZZLE_3X3" -> {
                                ImageTilePuzzleGame(
                                    timeLimitSeconds = 30,
                                    onSuccess = {
                                        Toast.makeText(context, "Puzzle Solved! Great job!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            "MATH_SPRINT" -> {
                                MathSprintGame(
                                    onSuccess = {
                                        Toast.makeText(context, "Math Sprint completed!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            "THREE_BREATHS" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🫁 3 Mindful Breaths", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Slow, deep breathing centers attention before digital screen time.", color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text("Inhale slowly... Hold... Exhale fully...", color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { activeDemoChallengeId = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Done Testing")
                                    }
                                }
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💪 Movement & Reset Challenge", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Perform 10 jumping jacks or bodyweight squats.", color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { activeDemoChallengeId = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Done Testing")
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
private fun PermissionCard(
    number: String,
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isGranted) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "$number. $title", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = desc, color = Color(0xFF64748B), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (isGranted) {
                Text("✓ ACTIVE", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("ENABLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.digitaldiscipline.spike.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.analytics.LocalAnalyticsRepository
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.policy.PolicyEngine
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.security.ParentPinManager
import com.digitaldiscipline.spike.security.PinVerificationResult
import com.digitaldiscipline.spike.cloud.PairingManager
import com.digitaldiscipline.spike.policy.profiles.ProfileTemplateManager
import com.digitaldiscipline.spike.sync.SyncManager
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.ui.dashboard.components.GeofenceBuilderCard
import com.digitaldiscipline.spike.ui.dashboard.components.ScheduleBuilderCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParentDashboardScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    policyEngine: PolicyEngine,
    policyRepository: PolicyRepository,
    analyticsRepository: LocalAnalyticsRepository,
    preferencesManager: PreferencesManager,
    syncManager: SyncManager,
    pinManager: ParentPinManager,
    pairingManager: PairingManager,
    isA11yActive: Boolean,
    isOverlayActive: Boolean,
    onNavigateToCloudHub: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onLockReturnToChildMode: (() -> Unit)? = null
) {
    val rules by policyRepository.getAllRulesFlow().collectAsState(initial = emptyList())
    val schedules by policyRepository.getAllSchedulesFlow().collectAsState(initial = emptyList())
    val geofenceZones by policyRepository.getAllGeofenceZonesFlow().collectAsState(initial = emptyList())
    val todayUsageList by analyticsRepository.getTodayUsageFlow().collectAsState(initial = emptyList())
    val activeProfileStr by preferencesManager.activePolicyProfileFlow.collectAsState(initial = "CORPORATE")
    val isInsideGeofence by preferencesManager.isInsideGeofenceFlow.collectAsState(initial = false)
    val activeGeofenceName by preferencesManager.activeGeofenceNameFlow.collectAsState(initial = "")

    val pairedFamilyId by preferencesManager.pairedFamilyIdFlow.collectAsState(initial = null)
    val pairedChildName by preferencesManager.pairedChildNameFlow.collectAsState(initial = "Child")
    val policyVersion by preferencesManager.policyVersionFlow.collectAsState(initial = 1)
    val lastPolicySync by preferencesManager.lastPolicySyncFlow.collectAsState(initial = 0L)

    val autoBlockGames by preferencesManager.autoBlockGamesFlow.collectAsState(initial = false)
    val autoBlockSocial by preferencesManager.autoBlockSocialFlow.collectAsState(initial = false)
    val autoBlockStreaming by preferencesManager.autoBlockStreamingFlow.collectAsState(initial = false)
    var showParentModeSwitcherDialog by remember { mutableStateOf(false) }

    var showGeneratePairCodeDialog by remember { mutableStateOf(false) }
    var activePairingCode by remember { mutableStateOf<String?>(null) }
    var isGeneratingPairCode by remember { mutableStateOf(false) }

    var showPinDialogForAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var newPinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var setPinError by remember { mutableStateOf<String?>(null) }
    var showAddCustomAppDialog by remember { mutableStateOf(false) }
    var showDiagnosticLogDialog by remember { mutableStateOf(false) }
    val enabledInterventionIds by preferencesManager.enabledInterventionsFlow.collectAsState(initial = emptySet())
    var showChallengeCatalogDialog by remember { mutableStateOf(false) }

    // Summary calculation
    val totalBlocks = todayUsageList.sumOf { it.blockCount }
    val totalUnlocks = todayUsageList.sumOf { it.unlockCount }
    val totalEarnedMinutes = todayUsageList.sumOf { it.earnedMinutes.coerceAtLeast(it.unlockCount * 10) }

    val isProtectionActive = isA11yActive && isOverlayActive
    val isPaired = !pairedFamilyId.isNullOrBlank()

    if (onLockReturnToChildMode != null) {
        androidx.activity.compose.BackHandler {
            onLockReturnToChildMode()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (onLockReturnToChildMode != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔑", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Parent Admin Active", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Child Device Settings Unlocked", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = { onLockReturnToChildMode() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("🔒 Lock Child Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // App Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FAMILY PROTECTION",
                    color = Color(0xFF10B981),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isPaired) "Child Device ($pairedChildName)" else "Parent & Guardian Control Center",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Switch Mode Button
                OutlinedButton(
                    onClick = { showParentModeSwitcherDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Switch Mode", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = CircleShape,
                    color = if (isProtectionActive) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (isProtectionActive) "🛡️" else "⚠️", fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category-Level Auto-Blocking Card (Games, Social Media, Short Video)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF059669).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.25f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🛡️ CATEGORY AUTO-BLOCKING", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("Auto-Lock Future Installs", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Text("Active Watchdog", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Automatically blocks newly installed apps in selected categories without requiring manual setup on child's phone.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 1: Games
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎮 Block All Games (BGMI, Free Fire, Ludo...)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Current & future game installs", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                    Switch(
                        checked = autoBlockGames,
                        onCheckedChange = { enabled ->
                            showPinDialogForAction = {
                                coroutineScope.launch {
                                    preferencesManager.setAutoBlockGames(enabled)
                                    Toast.makeText(context, if (enabled) "Auto-Block Games enabled" else "Auto-Block Games disabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981), checkedTrackColor = Color(0xFF059669))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle 2: Social Media
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📸 Block All Social & Video (Insta, Moj, Josh...)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Current & future social media apps", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                    Switch(
                        checked = autoBlockSocial,
                        onCheckedChange = { enabled ->
                            showPinDialogForAction = {
                                coroutineScope.launch {
                                    preferencesManager.setAutoBlockSocial(enabled)
                                    Toast.makeText(context, if (enabled) "Auto-Block Social Media enabled" else "Auto-Block Social Media disabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981), checkedTrackColor = Color(0xFF059669))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle 3: Streaming & OTT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎬 Block All Streaming (YouTube, Hotstar...)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Current & future OTT apps", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                    Switch(
                        checked = autoBlockStreaming,
                        onCheckedChange = { enabled ->
                            showPinDialogForAction = {
                                coroutineScope.launch {
                                    preferencesManager.setAutoBlockStreaming(enabled)
                                    Toast.makeText(context, if (enabled) "Auto-Block Streaming enabled" else "Auto-Block Streaming disabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981), checkedTrackColor = Color(0xFF059669))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Child Friction & Challenge Catalog Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🎯 ACTIVE FRICTION CATALOG", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("Earned Screen Time Challenges", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f)
                    ) {
                        Text("${if (enabledInterventionIds.isEmpty()) 50 else enabledInterventionIds.size} Active", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select which mindful puzzles, exercises, and games appear when child earns screen time.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        showPinDialogForAction = {
                            showChallengeCatalogDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("⚙️ CONFIGURE ACTIVE CHALLENGES", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Cloud Child Device Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isPaired) Color(0xFF059669) else Color(0xFF334155), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = if (isPaired) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFF0F172A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isPaired) "☁️" else "📱", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isPaired) "CHILD DEVICE: ${pairedChildName.uppercase()}" else "MANAGING THIS DEVICE DIRECTLY",
                                color = if (isPaired) Color(0xFF10B981) else Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isPaired) {
                                    val timeStr = if (lastPolicySync > 0) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastPolicySync)) else "Just now"
                                    "Active Policy: v$policyVersion • Last Synced: $timeStr"
                                } else {
                                    "Operating in standalone device mode."
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (isPaired) {
                        Button(
                            onClick = {
                                syncManager.triggerImmediateSync()
                                Toast.makeText(context, "Syncing cloud policy...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).padding(end = 4.dp).height(38.dp)
                        ) {
                            Text("🔄 1-TAP SYNC NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                isGeneratingPairCode = true
                                coroutineScope.launch {
                                    val deviceId = preferencesManager.getOrCreateDeviceId()
                                    val famId = pairedFamilyId ?: "family_$deviceId"
                                    val res = pairingManager.generatePairingCode(
                                        familyId = famId,
                                        childId = "child_1",
                                        childName = "Child Phone",
                                        parentId = deviceId
                                    )
                                    isGeneratingPairCode = false
                                    if (res.isSuccess) {
                                        activePairingCode = res.getOrNull()
                                        showGeneratePairCodeDialog = true
                                    } else {
                                        Toast.makeText(context, "Failed to generate code: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f).padding(end = 4.dp).height(38.dp)
                        ) {
                            if (isGeneratingPairCode) {
                                CircularProgressIndicator(color = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            } else {
                                Text("🔑 GENERATE PAIR CODE", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Button(
                        onClick = onNavigateToCloudHub,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(start = 4.dp).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text("☁️ PARENT CLOUD HUB", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Parent Control Hub Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF059669), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.6f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛡️", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PARENT CONTROL HUB ACTIVE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Managing remote policies, school schedules, and child device boundaries.",
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Today's Discipline Summary Card (with 1-Tap Reset)
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TODAY'S DISCIPLINE METRICS",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                analyticsRepository.clearAllMetrics()
                                Toast.makeText(context, "Today's metrics reset to 0!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("🔄 Reset Metrics", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricBox(
                        title = "Blocked",
                        value = "$totalBlocks",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MetricBox(
                        title = "Completed",
                        value = "$totalUnlocks",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MetricBox(
                        title = "Earned",
                        value = "${totalEarnedMinutes}m",
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Quick Testing & Escalation Harness
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B192C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TEST ESCALATION SEQUENCES",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                policyRepository.revokeTemporaryUnlock("com.instagram.android")
                                policyRepository.revokeTemporaryUnlock("com.google.android.youtube")
                                policyRepository.revokeTemporaryUnlock("com.dts.freefireth")
                                policyEngine.resetAttempts()
                                Toast.makeText(context, "All Unlocks & Attempts Reset to #1 (Mindful Pause)!", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Reset to Attempt #1", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap below to directly preview each intervention screen:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            policyEngine.overlayManager.showInterventionOverlay(
                                targetPackage = "com.google.android.youtube",
                                targetAppName = "YouTube",
                                unlockDurationSeconds = 10,
                                attemptNumber = 1
                            )
                        },
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("1. Pause (10s)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            policyEngine.overlayManager.showInterventionOverlay(
                                targetPackage = "com.google.android.youtube",
                                targetAppName = "YouTube",
                                unlockDurationSeconds = 10,
                                attemptNumber = 2
                            )
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("2. Breathe (30s)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            policyEngine.overlayManager.showInterventionOverlay(
                                targetPackage = "com.google.android.youtube",
                                targetAppName = "YouTube",
                                unlockDurationSeconds = 10,
                                attemptNumber = 3
                            )
                        },
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("3. Squats (10)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Configurable Time Windows & Schedules (Office Hours / Study Windows)
        ScheduleBuilderCard(
            schedules = schedules,
            onSaveSchedule = { schedule ->
                showPinDialogForAction = {
                    coroutineScope.launch {
                        policyRepository.saveSchedule(schedule)
                        Toast.makeText(context, "Schedule '${schedule.label}' saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onToggleSchedule = { schedule, isEnabled ->
                showPinDialogForAction = {
                    coroutineScope.launch {
                        policyRepository.updateSchedule(schedule.copy(isEnabled = isEnabled))
                    }
                }
            },
            onDeleteSchedule = { schedule ->
                showPinDialogForAction = {
                    coroutineScope.launch {
                        policyRepository.deleteSchedule(schedule)
                        Toast.makeText(context, "Schedule removed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4B. Workplace & School Geofences
        GeofenceBuilderCard(
            zones = geofenceZones,
            isInsideGeofence = isInsideGeofence,
            activeGeofenceName = activeGeofenceName,
            onSaveZone = { zone ->
                showPinDialogForAction = {
                    coroutineScope.launch {
                        policyRepository.saveGeofenceZone(zone)
                        val updatedZones = policyRepository.getEnabledGeofenceZones()
                        DigitalDisciplineApp.instance.workplaceGeofenceManager.registerGeofences(updatedZones)
                        Toast.makeText(context, "Geofence '${zone.name}' saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onToggleZone = { zone, isEnabled ->
                showPinDialogForAction = {
                    coroutineScope.launch {
                        policyRepository.updateGeofenceZone(zone.copy(isEnabled = isEnabled))
                        val updatedZones = policyRepository.getEnabledGeofenceZones()
                        DigitalDisciplineApp.instance.workplaceGeofenceManager.registerGeofences(updatedZones)
                    }
                }
            },
            onDeleteZone = { zone ->
                showPinDialogForAction = {
                    coroutineScope.launch {
                        policyRepository.deleteGeofenceZone(zone)
                        val updatedZones = policyRepository.getEnabledGeofenceZones()
                        DigitalDisciplineApp.instance.workplaceGeofenceManager.registerGeofences(updatedZones)
                        Toast.makeText(context, "Geofence removed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Target Applications & Active Rules
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ACTIVE ENFORCEMENT RULES",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Button(
                        onClick = {
                            showPinDialogForAction = {
                                showAddCustomAppDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ Add Target App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                rules.forEach { rule ->
                    DashboardAppRuleRow(
                        rule = rule,
                        onUpdateMode = { newMode ->
                            showPinDialogForAction = {
                                coroutineScope.launch {
                                    policyRepository.saveRule(rule.copy(mode = newMode))
                                }
                            }
                        },
                        onToggle = { isEnabled ->
                            showPinDialogForAction = {
                                coroutineScope.launch {
                                    policyRepository.saveRule(rule.copy(isEnabled = isEnabled))
                                }
                            }
                        },
                        onSetDuration = { durationSec ->
                            showPinDialogForAction = {
                                coroutineScope.launch {
                                    policyRepository.saveRule(rule.copy(unlockDurationSeconds = durationSec))
                                    Toast.makeText(context, "${rule.appDisplayName} duration set to ${if (durationSec < 60) "${durationSec}s" else "${durationSec/60}m"}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                }
            }
        }

        // 6. OEM & Subsystem Hardening Card
        val isBatteryIgnored = remember { com.digitaldiscipline.spike.detection.OemBatteryHelper.isIgnoringBatteryOptimizations(context) }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SYSTEM RELIABILITY & OEM HEALTH",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = com.digitaldiscipline.spike.detection.OemBatteryHelper.getOemName(),
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isBatteryIgnored) Color(0xFF064E3B) else Color(0xFF78350F),
                        border = BorderStroke(1.dp, if (isBatteryIgnored) Color(0xFF059669) else Color(0xFFD97706))
                    ) {
                        Text(
                            text = if (isBatteryIgnored) "BATTERY: SAFE" else "BATTERY: RESTRICTED",
                            color = if (isBatteryIgnored) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            com.digitaldiscipline.spike.detection.OemBatteryHelper.openOemBackgroundSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f).padding(end = 4.dp).height(36.dp)
                    ) {
                        Text("🔋 OEM Background Settings", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showDiagnosticLogDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(start = 4.dp).height(36.dp)
                    ) {
                        Text("📋 View Local Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. Anti-Tamper & OS-Level Security Shield Card
        var isDeviceAdminActive by remember { mutableStateOf(com.digitaldiscipline.spike.security.DeviceAdminSecurityManager.isDeviceAdminActive(context)) }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ANTI-TAMPER & OS SECURITY",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isDeviceAdminActive) "Anti-Uninstall Shield: ACTIVE" else "Anti-Uninstall Shield: INACTIVE",
                                color = if (isDeviceAdminActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDeviceAdminActive) Color(0xFF064E3B) else Color(0xFF78350F),
                        border = BorderStroke(1.dp, if (isDeviceAdminActive) Color(0xFF059669) else Color(0xFFD97706))
                    ) {
                        Text(
                            text = if (isDeviceAdminActive) "PROTECTED" else "UNPROTECTED",
                            color = if (isDeviceAdminActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• Settings Watchdog: Intercepts unauthorized Force-Stop & Disabling.\n• Web Interceptor: Blocks instagram.com & youtube.com in browsers.\n• Device Admin: Blocks uninstallation at the OS level.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isDeviceAdminActive) {
                    Button(
                        onClick = {
                            if (context is android.app.Activity) {
                                com.digitaldiscipline.spike.security.DeviceAdminSecurityManager.requestDeviceAdmin(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("🛡️ Activate Anti-Uninstall Protection (Device Admin)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            showPinDialogForAction = {
                                com.digitaldiscipline.spike.security.DeviceAdminSecurityManager.removeDeviceAdmin(context)
                                isDeviceAdminActive = false
                                Toast.makeText(context, "Device Admin protection deactivated.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Deactivate Device Admin (Requires PIN)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        newPinText = ""
                        confirmPinText = ""
                        setPinError = null
                        showSetPinDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(if (pinManager.isPinSet()) "🔑 Change Parent PIN" else "🔑 Set Secure Parent PIN (Required)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Diagnostic Log Inspector Dialog
    if (showDiagnosticLogDialog) {
            val diagnosticEvents by com.digitaldiscipline.spike.logging.DiagnosticLogger.getRecentEventsFlow(context)
                .collectAsState(initial = emptyList())

            AlertDialog(
                onDismissRequest = { showDiagnosticLogDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Local Diagnostic Telemetry (${diagnosticEvents.size})", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Privacy Guarantee: Diagnostics remain 100% on-device.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (diagnosticEvents.isEmpty()) {
                            Text("No diagnostic events recorded yet.", color = Color(0xFF64748B), fontSize = 12.sp)
                        } else {
                            diagnosticEvents.forEach { event ->
                                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMs))
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(event.eventType, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(timeStr, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                        if (!event.details.isNullOrBlank()) {
                                            Text(event.details, color = Color(0xFFCBD5E1), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDiagnosticLogDialog = false }) {
                        Text("Close", color = Color(0xFF38BDF8))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            com.digitaldiscipline.spike.logging.DiagnosticLogger.clearLogs(context)
                            Toast.makeText(context, "Diagnostic logs cleared.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Clear Logs", color = Color(0xFFEF4444))
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

    // Set / Change Parent PIN Dialog
    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showSetPinDialog = false
                newPinText = ""
                confirmPinText = ""
                setPinError = null
            },
            title = { Text(if (pinManager.isPinSet()) "Change Parent PIN" else "Create Secure Parent PIN", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Set a custom 4-digit PIN known only to the parent. This prevents children from modifying enforcement rules or disabling protections.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinText = it },
                        label = { Text("New 4-Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPinText,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPinText = it },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (setPinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = setPinError!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinText.length < 4) {
                            setPinError = "PIN must be exactly 4 digits."
                        } else if (newPinText != confirmPinText) {
                            setPinError = "PINs do not match."
                        } else {
                            val success = pinManager.setPin(newPinText)
                            if (success) {
                                Toast.makeText(context, "Parent PIN saved securely!", Toast.LENGTH_SHORT).show()
                                showSetPinDialog = false
                                newPinText = ""
                                confirmPinText = ""
                                setPinError = null
                            } else {
                                setPinError = "Failed to save PIN."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSetPinDialog = false
                    newPinText = ""
                    confirmPinText = ""
                    setPinError = null
                }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // Admin PIN Verification Dialog
    if (showPinDialogForAction != null) {
        val isPinAlreadySet = pinManager.isPinSet()
        AlertDialog(
            onDismissRequest = {
                showPinDialogForAction = null
                enteredPin = ""
                newPinText = ""
                confirmPinText = ""
                pinError = null
            },
            title = { Text(if (isPinAlreadySet) "Parent PIN Verification" else "Set Parent PIN Required", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (isPinAlreadySet) {
                        Text("Enter your 4-digit Parent PIN to authorize this change:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) enteredPin = it },
                            label = { Text("4-Digit PIN") },
                            singleLine = true,
                            isError = pinError != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("A secure Parent PIN is required before modifying enforcement policies. Set your 4-digit PIN below:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPinText,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinText = it },
                            label = { Text("New 4-Digit PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPinText,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPinText = it },
                            label = { Text("Confirm PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isPinAlreadySet) {
                            val result = pinManager.verifyPin(enteredPin)
                            when (result) {
                                is PinVerificationResult.Success -> {
                                    val action = showPinDialogForAction
                                    showPinDialogForAction = null
                                    enteredPin = ""
                                    pinError = null
                                    action?.invoke()
                                }
                                is PinVerificationResult.IncorrectPin -> {
                                    pinError = "Incorrect PIN. ${result.attemptsRemaining} attempts left."
                                }
                                is PinVerificationResult.LockedOut -> {
                                    pinError = "Locked out for ${result.remainingLockoutSeconds}s."
                                }
                                is PinVerificationResult.PinNotSet -> {
                                    pinError = "Please configure your Parent PIN."
                                }
                            }
                        } else {
                            if (newPinText.length < 4) {
                                pinError = "PIN must be exactly 4 digits."
                            } else if (newPinText != confirmPinText) {
                                pinError = "PINs do not match."
                            } else {
                                pinManager.setPin(newPinText)
                                val action = showPinDialogForAction
                                showPinDialogForAction = null
                                newPinText = ""
                                confirmPinText = ""
                                pinError = null
                                Toast.makeText(context, "Parent PIN set and authorized!", Toast.LENGTH_SHORT).show()
                                action?.invoke()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text(if (isPinAlreadySet) "Authorize" else "Save & Authorize")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialogForAction = null
                    enteredPin = ""
                    newPinText = ""
                    confirmPinText = ""
                    pinError = null
                }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // Single-Use Pair Code Generation Dialog
    if (showGeneratePairCodeDialog && activePairingCode != null) {
        AlertDialog(
            onDismissRequest = { showGeneratePairCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔗", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pair Child Device", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "Enter this 6-digit code on your child's phone:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFF10B981)),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = activePairingCode!!,
                            color = Color(0xFF10B981),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text("📲 Steps on Child's Phone:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("1. Open Digital Discipline on Child Phone.\n2. Tap '👶 Child Phone' on start screen.\n3. Tap 'Enter 6-Digit Pairing Code'.\n4. Type this code to link instantly.", color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 16.sp)
                    }

                    Text("⏱️ Code valid for 15 minutes • Single-use", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGeneratePairCodeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Done", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // Parent / Family Mode Switcher Dialog
    if (showParentModeSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showParentModeSwitcherDialog = false },
            title = { Text("Switch Operating Mode", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    preferencesManager.setUserMode(com.digitaldiscipline.spike.data.local.entities.UserMode.CHILD.name)
                                    preferencesManager.setDeviceRole("CHILD_DEVICE")
                                }
                                onLockReturnToChildMode?.invoke()
                                showParentModeSwitcherDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("👶", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Child Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Fun challenges, earned screen time wallet & parent PIN lock", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    preferencesManager.setUserMode(com.digitaldiscipline.spike.data.local.entities.UserMode.SELF.name)
                                }
                                showParentModeSwitcherDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🧑", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Self Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Personal habit control & earned time wallet", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF6366F1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    preferencesManager.setUserMode(com.digitaldiscipline.spike.data.local.entities.UserMode.OFFICE.name)
                                }
                                showParentModeSwitcherDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💼", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Office Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("9-to-5 work focus, deep work sprints & whitelist", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showParentModeSwitcherDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // Add Custom Target App Dialog with Quick Presets
    if (showAddCustomAppDialog) {
        var customPkg by remember { mutableStateOf("") }
        var customName by remember { mutableStateOf("") }
        var selectedMode by remember { mutableStateOf(RuleMode.EARN) }
        var modeDropdownExpanded by remember { mutableStateOf(false) }

        val popularAppPresets = listOf(
            "TikTok" to "com.zhiliaoapp.musically",
            "Snapchat" to "com.snapchat.android",
            "Reddit" to "com.reddit.frontpage",
            "Roblox" to "com.roblox.client",
            "Twitter / X" to "com.twitter.android",
            "Netflix" to "com.netflix.mediaclient",
            "Discord" to "com.discord",
            "Facebook" to "com.facebook.katana",
            "Chrome Browser" to "com.android.chrome"
        )

        AlertDialog(
            onDismissRequest = { showAddCustomAppDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("➕", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Target App to Restrict", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1-Tap Popular Apps Preset:",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularAppPresets.take(3).forEach { (name, pkg) ->
                            AssistChip(
                                onClick = {
                                    customName = name
                                    customPkg = pkg
                                },
                                leadingIcon = {
                                    com.digitaldiscipline.spike.ui.components.AppIconImage(
                                        packageName = pkg,
                                        modifier = Modifier.size(18.dp),
                                        cornerRadius = 4.dp
                                    )
                                },
                                label = { Text(name, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularAppPresets.drop(3).take(3).forEach { (name, pkg) ->
                            AssistChip(
                                onClick = {
                                    customName = name
                                    customPkg = pkg
                                },
                                leadingIcon = {
                                    com.digitaldiscipline.spike.ui.components.AppIconImage(
                                        packageName = pkg,
                                        modifier = Modifier.size(18.dp),
                                        cornerRadius = 4.dp
                                    )
                                },
                                label = { Text(name, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularAppPresets.drop(6).take(3).forEach { (name, pkg) ->
                            AssistChip(
                                onClick = {
                                    customName = name
                                    customPkg = pkg
                                },
                                leadingIcon = {
                                    com.digitaldiscipline.spike.ui.components.AppIconImage(
                                        packageName = pkg,
                                        modifier = Modifier.size(18.dp),
                                        cornerRadius = 4.dp
                                    )
                                },
                                label = { Text(name, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Or Enter App Details:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Box 1: App Display Name") },
                        placeholder = { Text("e.g. TikTok or Roblox") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customPkg,
                        onValueChange = { customPkg = it },
                        label = { Text("Box 2: Android Package Name") },
                        placeholder = { Text("e.g. com.zhiliaoapp.musically") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Initial Rule Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Initial Rule Mode:", color = Color(0xFFCBD5E1), fontSize = 12.sp)

                        Box {
                            OutlinedButton(
                                onClick = { modeDropdownExpanded = true },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("${selectedMode.name} ▾", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = modeDropdownExpanded,
                                onDismissRequest = { modeDropdownExpanded = false }
                            ) {
                                RuleMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.name, fontSize = 12.sp) },
                                        onClick = {
                                            selectedMode = mode
                                            modeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPkg.isNotBlank()) {
                            coroutineScope.launch {
                                policyRepository.saveRule(
                                    AppRuleEntity(
                                        packageName = customPkg.trim(),
                                        appDisplayName = if (customName.isNotBlank()) customName.trim() else customPkg.trim(),
                                        mode = selectedMode,
                                        isEnabled = true,
                                        unlockDurationSeconds = 600,
                                        pauseDurationSeconds = 10,
                                        breathingDurationSeconds = 30,
                                        squatsTargetCount = 10
                                    )
                                )
                                Toast.makeText(context, "Added ${customName.ifBlank { customPkg }} to restricted apps!", Toast.LENGTH_SHORT).show()
                            }
                            showAddCustomAppDialog = false
                        } else {
                            Toast.makeText(context, "Please enter or select a Package Name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Save & Enable App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomAppDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // Configure Child Challenge Catalog Dialog
    if (showChallengeCatalogDialog) {
        AlertDialog(
            onDismissRequest = { showChallengeCatalogDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Child Challenges", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose which challenges and games are available when child earns screen time:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    // Preset 1: Mind & Word Games
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val ids = setOf("IMAGE_PUZZLE_3X3", "HANGMAN_CLASSIC", "MATH_SPRINT", "MEMORY_MATRIX", "STROOP_TEST")
                                    preferencesManager.setEnabledInterventions(ids)
                                    Toast.makeText(context, "Saved Brain & Word Games preset for child!", Toast.LENGTH_SHORT).show()
                                }
                                showChallengeCatalogDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🧩", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Puzzles & Word Games Only", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("3x3 Picture Puzzle, Hangman, Math Sprint & Memory Matrix", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    // Preset 2: Physical Movement & Posture
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val ids = setOf("SQUATS", "CALF_RAISES", "PUSH_UPS", "JUMPING_JACKS", "PLANK")
                                    preferencesManager.setEnabledInterventions(ids)
                                    Toast.makeText(context, "Saved Fitness & Movement preset for child!", Toast.LENGTH_SHORT).show()
                                }
                                showChallengeCatalogDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💪", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Fitness & Movement Sprints", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Squats, Calf Raises, Push-ups with AI camera form counting", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    // Preset 3: All 50 Challenges
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val allIds = InterventionCatalog.getAllInterventions().map { it.id }.toSet()
                                    preferencesManager.setEnabledInterventions(allIds)
                                    Toast.makeText(context, "Enabled all 50 challenges for child!", Toast.LENGTH_SHORT).show()
                                }
                                showChallengeCatalogDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🌟", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("All 50 Challenges Allowed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Full catalog: puzzles, fitness, breathwork & creative", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showChallengeCatalogDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("Done", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChallengeCatalogDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}

@Composable
fun MetricBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun DashboardAppRuleRow(
    rule: AppRuleEntity,
    onUpdateMode: (RuleMode) -> Unit,
    onToggle: (Boolean) -> Unit,
    onSetDuration: (Int) -> Unit
) {
    var modeDropdownExpanded by remember { mutableStateOf(false) }
    var durationDropdownExpanded by remember { mutableStateOf(false) }

    val durationOptions = listOf(
        10 to "10s (Test)",
        60 to "1 min",
        300 to "5 min",
        600 to "10 min",
        900 to "15 min",
        1800 to "30 min",
        3600 to "60 min"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                com.digitaldiscipline.spike.ui.components.AppIconImage(
                    packageName = rule.packageName,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = rule.appDisplayName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${rule.mode} Mode • Earns ${if (rule.unlockDurationSeconds < 60) "${rule.unlockDurationSeconds}s" else "${rule.unlockDurationSeconds / 60}m"} access",
                        color = when (rule.mode) {
                            RuleMode.EARN -> Color(0xFF38BDF8)
                            RuleMode.BLOCK -> Color(0xFFEF4444)
                            RuleMode.DELAY -> Color(0xFFFBBF24)
                            RuleMode.ALLOW -> Color(0xFF10B981)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rule Mode Dropdown
                Box {
                    OutlinedButton(
                        onClick = { modeDropdownExpanded = true },
                        modifier = Modifier.height(32.dp).padding(end = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, when (rule.mode) {
                            RuleMode.EARN -> Color(0xFF38BDF8)
                            RuleMode.BLOCK -> Color(0xFFEF4444)
                            RuleMode.DELAY -> Color(0xFFFBBF24)
                            RuleMode.ALLOW -> Color(0xFF10B981)
                        })
                    ) {
                        Text(
                            text = "${rule.mode.name} ▾",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (rule.mode) {
                                RuleMode.EARN -> Color(0xFF38BDF8)
                                RuleMode.BLOCK -> Color(0xFFEF4444)
                                RuleMode.DELAY -> Color(0xFFFBBF24)
                                RuleMode.ALLOW -> Color(0xFF10B981)
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = modeDropdownExpanded,
                        onDismissRequest = { modeDropdownExpanded = false }
                    ) {
                        RuleMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (mode) {
                                            RuleMode.EARN -> "EARN (Physical / Mental Challenge)"
                                            RuleMode.BLOCK -> "BLOCK (Strict Lock Screen)"
                                            RuleMode.DELAY -> "DELAY (Mindful Pause Only)"
                                            RuleMode.ALLOW -> "ALLOW (Unrestricted)"
                                        },
                                        fontWeight = if (rule.mode == mode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = when (mode) {
                                            RuleMode.EARN -> Color(0xFF38BDF8)
                                            RuleMode.BLOCK -> Color(0xFFEF4444)
                                            RuleMode.DELAY -> Color(0xFFFBBF24)
                                            RuleMode.ALLOW -> Color(0xFF10B981)
                                        }
                                    )
                                },
                                onClick = {
                                    modeDropdownExpanded = false
                                    onUpdateMode(mode)
                                }
                            )
                        }
                    }
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2563EB)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Duration Dropdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Unlock Duration: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.width(6.dp))

            Box {
                OutlinedButton(
                    onClick = { durationDropdownExpanded = true },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    val currentLabel = durationOptions.find { it.first == rule.unlockDurationSeconds }?.second
                        ?: "${rule.unlockDurationSeconds / 60} min"
                    Text("$currentLabel ▾", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = durationDropdownExpanded,
                    onDismissRequest = { durationDropdownExpanded = false }
                ) {
                    durationOptions.forEach { (seconds, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = 12.sp, fontWeight = if (rule.unlockDurationSeconds == seconds) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                durationDropdownExpanded = false
                                onSetDuration(seconds)
                            }
                        )
                    }
                }
            }
        }
    }
}

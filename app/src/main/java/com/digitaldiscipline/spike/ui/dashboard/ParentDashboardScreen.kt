package com.digitaldiscipline.spike.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.analytics.LocalAnalyticsRepository
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.policy.PolicyEngine
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.security.ParentPinManager
import com.digitaldiscipline.spike.security.PinVerificationResult
import com.digitaldiscipline.spike.policy.profiles.ProfileTemplateManager
import com.digitaldiscipline.spike.sync.SyncManager
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.ui.dashboard.components.GeofenceBuilderCard
import com.digitaldiscipline.spike.ui.dashboard.components.ProfileSwitcherCard
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
    isA11yActive: Boolean,
    isOverlayActive: Boolean,
    onNavigateToCloudHub: () -> Unit,
    onNavigateToPairing: () -> Unit
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

    var showPinDialogForAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showAddCustomAppDialog by remember { mutableStateOf(false) }
    var showDiagnosticLogDialog by remember { mutableStateOf(false) }

    // Summary calculation
    val totalBlocks = todayUsageList.sumOf { it.blockCount }
    val totalUnlocks = todayUsageList.sumOf { it.unlockCount }
    val totalEarnedMinutes = todayUsageList.sumOf { it.earnedMinutes.coerceAtLeast(it.unlockCount * 10) }

    val isProtectionActive = isA11yActive && isOverlayActive
    val isPaired = !pairedFamilyId.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "DIGITAL DISCIPLINE",
                    color = Color(0xFF38BDF8),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isPaired) "Managed Device ($pairedChildName)" else "Admin & Device Policy Manager",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Switch to Self Mode Button
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setUserMode(com.digitaldiscipline.spike.data.local.entities.UserMode.SELF.name)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Self Mode", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

        // 0. Active Discipline Profile Card (Workplace / Family / Deep Work)
        ProfileSwitcherCard(
            activeProfileStr = activeProfileStr,
            onSelectProfile = { selectedType, appendMode ->
                showPinDialogForAction = {
                    coroutineScope.launch {
                        ProfileTemplateManager.applyProfile(
                            type = selectedType,
                            policyRepository = policyRepository,
                            preferencesManager = preferencesManager,
                            appendMode = appendMode
                        )
                        val t = ProfileTemplateManager.getTemplate(selectedType)
                        Toast.makeText(context, "${t.iconEmoji} Switched to ${t.title}!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Cloud Control Plane Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isPaired) Color(0xFF0284C7) else Color(0xFF334155), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = if (isPaired) Color(0xFF0C1E38) else Color(0xFF0F172A)),
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
                                text = if (isPaired) "PAIRED WORKSPACE: ${pairedChildName.uppercase()}" else "STANDALONE MODE (NOT PAIRED)",
                                color = if (isPaired) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isPaired) {
                                    val timeStr = if (lastPolicySync > 0) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastPolicySync)) else "Just now"
                                    "Active Policy: v$policyVersion • Last Synced: $timeStr"
                                } else {
                                    "Not paired to Admin Cloud Console yet."
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).padding(end = 4.dp).height(38.dp)
                        ) {
                            Text("🔄 1-TAP SYNC NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onNavigateToPairing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).padding(end = 4.dp).height(38.dp)
                        ) {
                            Text("🔗 PAIR WITH CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onNavigateToCloudHub,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(start = 4.dp).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Text("☁️ ADMIN CONSOLE", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Protection Health Card
        if (isProtectionActive) {
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
                    Text("✓", color = Color(0xFF34D399), fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "OFFLINE-FIRST PROTECTION ACTIVE",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enforcement is 100% local. Rules apply even with Wi-Fi/Data OFF.",
                            color = Color(0xFFA7F3D0),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDC2626), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚠️ PROTECTION DISABLED",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Required permissions are missing. Tap below to enable directly:",
                        color = Color(0xFFFECACA),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isA11yActive) {
                        Button(
                            onClick = {
                                com.digitaldiscipline.spike.ui.onboarding.PermissionGuideOverlay.show(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Text("1. ENABLE ACCESSIBILITY SERVICE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isOverlayActive) {
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("2. ENABLE DRAW OVER APPS (OVERLAY)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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

    // Admin PIN Verification Dialog
    if (showPinDialogForAction != null) {
        AlertDialog(
            onDismissRequest = {
                showPinDialogForAction = null
                enteredPin = ""
                pinError = null
            },
            title = { Text("Admin PIN Verification", color = Color.White) },
            text = {
                Column {
                    Text("Enter your 4-digit Admin PIN to authorize this change:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 6) enteredPin = it },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        isError = pinError != null,
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
                                pinError = "Admin PIN not configured."
                            }
                        }
                    }
                ) {
                    Text("Authorize")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialogForAction = null
                    enteredPin = ""
                    pinError = null
                }) {
                    Text("Cancel")
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

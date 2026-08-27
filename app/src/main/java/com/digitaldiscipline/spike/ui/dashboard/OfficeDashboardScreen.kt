package com.digitaldiscipline.spike.ui.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.digitaldiscipline.spike.behaviour.templates.GoalTemplateRepository
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.UserMode
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.policy.PolicyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BgDeep = Color(0xFF070A14)
private val BgSurface = Color(0xFF0F172A)
private val BgCard = Color(0xFF131C31)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentCyan = Color(0xFF38BDF8)
private val AccentGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)
private val BorderDefault = Color(0xFF1E293B)

@Composable
fun OfficeDashboardScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    policyRepository: PolicyRepository,
    preferencesManager: PreferencesManager,
    isA11yActive: Boolean,
    isOverlayActive: Boolean,
    onSwitchMode: (UserMode) -> Unit
) {
    val startHour by preferencesManager.officeStartHourFlow.collectAsState(initial = 9)
    val startMinute by preferencesManager.officeStartMinuteFlow.collectAsState(initial = 0)
    val endHour by preferencesManager.officeEndHourFlow.collectAsState(initial = 17)
    val endMinute by preferencesManager.officeEndMinuteFlow.collectAsState(initial = 0)
    val isDeepWorkActive by preferencesManager.officeDeepWorkActiveFlow.collectAsState(initial = false)
    val deepWorkExpiry by preferencesManager.officeDeepWorkExpiryFlow.collectAsState(initial = 0L)
    val isMeetingMode by preferencesManager.officeMeetingModeFlow.collectAsState(initial = false)

    val rules by policyRepository.getAllRulesFlow().collectAsState(initial = emptyList())

    var showEditScheduleDialog by remember { mutableStateOf(false) }
    var showAddDistractionDialog by remember { mutableStateOf(false) }
    var showModeSwitcherDialog by remember { mutableStateOf(false) }

    // Countdown calculation for deep work
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isDeepWorkActive, deepWorkExpiry) {
        while (isDeepWorkActive && deepWorkExpiry > System.currentTimeMillis()) {
            remainingSeconds = ((deepWorkExpiry - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
        if (isDeepWorkActive && deepWorkExpiry <= System.currentTimeMillis()) {
            preferencesManager.setOfficeDeepWork(false)
        }
    }

    val productiveWhitelist = listOf(
        "Slack" to "💼",
        "Microsoft Teams" to "👥",
        "Google Meet / Zoom" to "📹",
        "Gmail / Outlook" to "✉️",
        "Notion / Jira" to "📝",
        "GitHub" to "🐙"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Executive Mode Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentIndigo.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "💼 OFFICE MODE",
                            color = AccentIndigo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            letterSpacing = 1.sp
                        )
                    }
                    if (isMeetingMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentAmber.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "🎙️ MEETING ACTIVE",
                                color = AccentAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deep Work & Focus",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { showModeSwitcherDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Switch Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Deep Work Sprint Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.5.dp, if (isDeepWorkActive) AccentAmber else BorderDefault),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🎯 DEEP WORK SPRINT", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(
                            text = if (isDeepWorkActive) "Focus Sprint Active" else "Start a Focus Sprint",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isDeepWorkActive) {
                        Surface(
                            shape = CircleShape,
                            color = AccentAmber.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, AccentAmber)
                        ) {
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                color = AccentAmber,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isDeepWorkActive) "All non-work apps are strictly locked until your sprint concludes."
                    else "Lockdown non-work distractions for an uninterrupted deep focus session.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!isDeepWorkActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25 to "25m Sprint", 50 to "50m Block", 90 to "90m Deep").forEach { (mins, label) ->
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setOfficeDeepWork(true, mins)
                                        Toast.makeText(context, "$mins min Deep Work started!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                preferencesManager.setOfficeDeepWork(false)
                                Toast.makeText(context, "Deep Work sprint ended.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Text("End Sprint Early", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Work Hours Focus Schedule (9-to-5)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("⏰ 9-TO-5 WORK HOURS ENGINE", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(
                            text = String.format("%02d:%02d - %02d:%02d (Mon-Fri)", startHour, startMinute, endHour, endMinute),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = { showEditScheduleDialog = true }) {
                        Text("Edit", color = AccentCyan, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Restricts social media, shopping, trading, and games during your work hours to protect office productivity.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Meeting Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isMeetingMode) AccentAmber.copy(alpha = 0.15f) else Color(0xFF1E293B))
                        .border(1.dp, if (isMeetingMode) AccentAmber else BorderDefault, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎙️ Presentation / Meeting Mode", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Mute non-essential apps for client calls", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isMeetingMode,
                        onCheckedChange = { active ->
                            coroutineScope.launch {
                                preferencesManager.setOfficeMeetingMode(active)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentAmber, checkedTrackColor = AccentAmber.copy(alpha = 0.4f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Productivity Whitelist
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🛡️ WORKPLACE WHITELIST", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("Pre-cleared Work Tools", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Always Allowed", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    productiveWhitelist.take(3).forEach { (name, emoji) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$emoji $name",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Workplace Distraction Rules List
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🚫 WORKPLACE RESTRICTIONS", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("Restricted Work Distractions (${rules.size})", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showAddDistractionDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("+ Add App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (rules.isEmpty()) {
                    Text(
                        text = "No custom workplace apps added yet. Tap '+ Add App' to restrict specific apps during work hours.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    rules.forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.appDisplayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(rule.packageName, color = TextSecondary, fontSize = 10.sp)
                            }
                            Switch(
                                checked = rule.isEnabled,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        policyRepository.saveRule(rule.copy(isEnabled = enabled))
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentIndigo, checkedTrackColor = AccentIndigo.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Edit Schedule Dialog
    if (showEditScheduleDialog) {
        var tempStartH by remember { mutableIntStateOf(startHour) }
        var tempEndH by remember { mutableIntStateOf(endHour) }

        AlertDialog(
            onDismissRequest = { showEditScheduleDialog = false },
            title = { Text("Configure Work Hours", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose your standard working hours for automatic focus enforcement:", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Start Time:", color = TextPrimary, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (tempStartH > 6) tempStartH-- }) { Text("◀", color = AccentCyan) }
                            Text(String.format("%02d:00", tempStartH), color = TextPrimary, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (tempStartH < 12) tempStartH++ }) { Text("▶", color = AccentCyan) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("End Time:", color = TextPrimary, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (tempEndH > 14) tempEndH-- }) { Text("◀", color = AccentCyan) }
                            Text(String.format("%02d:00", tempEndH), color = TextPrimary, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (tempEndH < 22) tempEndH++ }) { Text("▶", color = AccentCyan) }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setOfficeSchedule(tempStartH, 0, tempEndH, 0)
                            Toast.makeText(context, "Work hours updated!", Toast.LENGTH_SHORT).show()
                        }
                        showEditScheduleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text("Save Hours")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditScheduleDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BgSurface
        )
    }

    // Add Distraction Dialog
    if (showAddDistractionDialog) {
        val common = remember { GoalTemplateRepository.getAllDistractionRecommendations() }
        AlertDialog(
            onDismissRequest = { showAddDistractionDialog = false },
            title = { Text("Add Workplace Distraction", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    common.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        policyRepository.saveRule(
                                            AppRuleEntity(
                                                packageName = rec.packageName,
                                                appDisplayName = rec.displayName,
                                                mode = RuleMode.BLOCK,
                                                isEnabled = true
                                            )
                                        )
                                        Toast.makeText(context, "Added ${rec.displayName} to workplace blocklist", Toast.LENGTH_SHORT).show()
                                    }
                                    showAddDistractionDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rec.icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(rec.displayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDistractionDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = BgSurface
        )
    }

    // Switch Mode Dialog
    if (showModeSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showModeSwitcherDialog = false },
            title = { Text("Switch Operating Mode", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select which mode you would like to switch to:", color = TextSecondary, fontSize = 12.sp)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSwitchMode(UserMode.SELF)
                                showModeSwitcherDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🧑", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Self Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Personal mindfulness & earned time wallet", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF059669).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSwitchMode(UserMode.FAMILY)
                                showModeSwitcherDialog = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("👨‍👩‍👧", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Family Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Parent PIN security & child protection", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showModeSwitcherDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BgSurface
        )
    }
}

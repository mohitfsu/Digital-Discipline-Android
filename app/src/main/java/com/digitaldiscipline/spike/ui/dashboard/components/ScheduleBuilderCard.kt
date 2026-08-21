package com.digitaldiscipline.spike.ui.dashboard.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.data.local.entities.ScheduleEntity
import java.util.Calendar
import java.util.Locale

@Composable
fun ScheduleBuilderCard(
    schedules: List<ScheduleEntity>,
    onSaveSchedule: (ScheduleEntity) -> Unit,
    onToggleSchedule: (ScheduleEntity, Boolean) -> Unit,
    onDeleteSchedule: (ScheduleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleEntity?>(null) }

    val activeNowCount = remember(schedules) {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentMin = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        schedules.count { sched ->
            if (!sched.isEnabled) return@count false
            val matchesDay = if (sched.daysOfWeekCsv.isNotBlank()) {
                val days = sched.daysOfWeekCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
                days.contains(currentDay)
            } else {
                sched.dayOfWeek == currentDay
            }
            val start = sched.startHour * 60 + sched.startMinute
            val end = sched.endHour * 60 + sched.endMinute
            val inWindow = if (start <= end) currentMin in start..end else (currentMin >= start || currentMin <= end)
            matchesDay && inWindow
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📅", fontSize = 18.sp)
                        Text(
                            text = "TIME WINDOWS & SCHEDULES",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = if (activeNowCount > 0) "🟢 $activeNowCount time window currently active" else "Configurable office, study & focus hours",
                        color = if (activeNowCount > 0) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (activeNowCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Button(
                    onClick = {
                        editingSchedule = null
                        showDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+ Add Window", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (schedules.isEmpty()) {
                // Empty state card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🕒", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No Scheduled Restrictions",
                            color = Color(0xFFF1F5F9),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add configurable office hours, study windows, or bedtime focus blocks to automatically lock distracting apps.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Schedule Cards List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    schedules.forEach { schedule ->
                        ScheduleItemCard(
                            schedule = schedule,
                            onToggle = { isEnabled -> onToggleSchedule(schedule, isEnabled) },
                            onEdit = {
                                editingSchedule = schedule
                                showDialog = true
                            },
                            onDelete = { onDeleteSchedule(schedule) }
                        )
                    }
                }
            }
        }
    }

    // Configurable Schedule Dialog
    if (showDialog) {
        ConfigurableScheduleDialog(
            initialSchedule = editingSchedule,
            onDismiss = { showDialog = false },
            onSave = { saved ->
                onSaveSchedule(saved)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleItemCard(
    schedule: ScheduleEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
    val currentMin = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

    val isActiveNow = remember(schedule, currentDay, currentMin) {
        if (!schedule.isEnabled) return@remember false
        val matchesDay = if (schedule.daysOfWeekCsv.isNotBlank()) {
            val days = schedule.daysOfWeekCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            days.contains(currentDay)
        } else {
            schedule.dayOfWeek == currentDay
        }
        val start = schedule.startHour * 60 + schedule.startMinute
        val end = schedule.endHour * 60 + schedule.endMinute
        val inWindow = if (start <= end) currentMin in start..end else (currentMin >= start || currentMin <= end)
        matchesDay && inWindow
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActiveNow) Color(0xFF0C243B) else Color(0xFF1E293B).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (isActiveNow) Color(0xFF0284C7) else Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = schedule.label.ifBlank { "Schedule" },
                        color = Color(0xFFF8FAFC),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActiveNow) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE NOW",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time range display
            val timeText = formatTimeRange(schedule.startHour, schedule.startMinute, schedule.endHour, schedule.endMinute)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🕒", fontSize = 13.sp)
                Text(
                    text = timeText,
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("•", color = Color(0xFF64748B), fontSize = 12.sp)
                Text(
                    text = if (schedule.restrictionMode == "BLOCK") "⛔ Strict Block" else "📹 30s Reset",
                    color = if (schedule.restrictionMode == "BLOCK") Color(0xFFF87171) else Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days of week chips & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Days row
                val activeDays = remember(schedule.daysOfWeekCsv, schedule.dayOfWeek) {
                    if (schedule.daysOfWeekCsv.isNotBlank()) {
                        schedule.daysOfWeekCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                    } else {
                        setOf(schedule.dayOfWeek)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val dayLabels = listOf(2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S", 1 to "S")
                    dayLabels.forEach { (dayInt, label) ->
                        val isSelected = activeDays.contains(dayInt)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF334155).copy(alpha = 0.4f))
                                .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Edit & Delete icons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurableScheduleDialog(
    initialSchedule: ScheduleEntity?,
    onDismiss: () -> Unit,
    onSave: (ScheduleEntity) -> Unit
) {
    var label by remember { mutableStateOf(initialSchedule?.label ?: "Office Hours") }
    var startHour by remember { mutableIntStateOf(initialSchedule?.startHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(initialSchedule?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialSchedule?.endHour ?: 17) }
    var endMinute by remember { mutableIntStateOf(initialSchedule?.endMinute ?: 0) }
    var restrictionMode by remember { mutableStateOf(initialSchedule?.restrictionMode ?: "BLOCK") }

    val initialDays = remember {
        if (initialSchedule != null && initialSchedule.daysOfWeekCsv.isNotBlank()) {
            initialSchedule.daysOfWeekCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
        } else if (initialSchedule != null) {
            mutableSetOf(initialSchedule.dayOfWeek)
        } else {
            // Default Mon-Fri (2,3,4,5,6)
            mutableSetOf(2, 3, 4, 5, 6)
        }
    }
    val selectedDays = remember { mutableStateListOf<Int>().apply { addAll(initialDays) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSchedule == null) "Add Configurable Time Window" else "Edit Time Window",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quick Presets Selector
                Text("Quick Presets (Customizable):", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PresetChip(
                        label = "🏢 Office",
                        onClick = {
                            label = "Office Hours"
                            startHour = 9; startMinute = 0
                            endHour = 17; endMinute = 0
                            selectedDays.clear()
                            selectedDays.addAll(listOf(2, 3, 4, 5, 6)) // Mon-Fri
                            restrictionMode = "BLOCK"
                        }
                    )
                    PresetChip(
                        label = "📚 Study",
                        onClick = {
                            label = "Study Hours"
                            startHour = 17; startMinute = 0
                            endHour = 20; endMinute = 30
                            selectedDays.clear()
                            selectedDays.addAll(listOf(2, 3, 4, 5, 6, 7)) // Mon-Sat
                            restrictionMode = "INTERVENE"
                        }
                    )
                    PresetChip(
                        label = "🌙 Bedtime",
                        onClick = {
                            label = "Bedtime Lock"
                            startHour = 22; startMinute = 0
                            endHour = 6; endMinute = 0
                            selectedDays.clear()
                            selectedDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7)) // All Days
                            restrictionMode = "BLOCK"
                        }
                    )
                }

                // Label input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Schedule Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Start Time & End Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimePickerCard(
                        title = "Start Time",
                        hour = startHour,
                        minute = startMinute,
                        onTimeChanged = { h, m ->
                            startHour = h
                            startMinute = m
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerCard(
                        title = "End Time",
                        hour = endHour,
                        minute = endMinute,
                        onTimeChanged = { h, m ->
                            endHour = h
                            endMinute = m
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Configurable Days Selection
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Days:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Workdays",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    selectedDays.clear()
                                    selectedDays.addAll(listOf(2, 3, 4, 5, 6))
                                }
                            )
                            Text("•", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(
                                text = "All Days",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    selectedDays.clear()
                                    selectedDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf(
                            2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu",
                            6 to "Fri", 7 to "Sat", 1 to "Sun"
                        )
                        days.forEach { (dayInt, name) ->
                            val isSel = selectedDays.contains(dayInt)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    if (isSel) {
                                        if (selectedDays.size > 1) selectedDays.remove(dayInt)
                                    } else {
                                        selectedDays.add(dayInt)
                                    }
                                },
                                label = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSel) Color(0xFF38BDF8) else Color(0xFF334155),
                                    enabled = true,
                                    selected = isSel
                                )
                            )
                        }
                    }
                }

                // Restriction Action Mode
                Column {
                    Text("Restriction Mode:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (restrictionMode == "BLOCK") Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (restrictionMode == "BLOCK") Color(0xFFEF4444) else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { restrictionMode = "BLOCK" }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⛔ Strict Block", color = if (restrictionMode == "BLOCK") Color.White else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Admin PIN required", color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (restrictionMode == "INTERVENE") Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (restrictionMode == "INTERVENE") Color(0xFF10B981) else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { restrictionMode = "INTERVENE" }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🧘 30s Reset", color = if (restrictionMode == "INTERVENE") Color.White else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Camera AI / Mindful", color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val csv = selectedDays.sorted().joinToString(",")
                    val firstDay = selectedDays.firstOrNull() ?: 2
                    val scheduleToSave = (initialSchedule ?: ScheduleEntity()).copy(
                        label = label.ifBlank { "Schedule" },
                        packageName = "ALL_RESTRICTED",
                        dayOfWeek = firstDay,
                        daysOfWeekCsv = csv,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        isBlocked = true,
                        restrictionMode = restrictionMode,
                        isEnabled = true
                    )
                    onSave(scheduleToSave)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (initialSchedule == null) "Create Schedule" else "Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = Color(0xFF38BDF8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun TimePickerCard(
    title: String,
    hour: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimeDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier.clickable { showTimeDialog = true }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTime(hour, minute),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showTimeDialog) {
        SimpleTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimeDialog = false },
            onConfirm = { h, m ->
                onTimeChanged(h, m)
                showTimeDialog = false
            }
        )
    }
}

@Composable
private fun SimpleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = formatTime(hour, minute),
                    color = Color(0xFF38BDF8),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour (0-23)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) {
                                Text("◀", color = Color.White, fontSize = 16.sp)
                            }
                            Text(String.format(Locale.US, "%02d", hour), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                Text("▶", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }

                    // Minute picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute (0-59)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { minute = (minute - 5 + 60) % 60 }) {
                                Text("◀", color = Color.White, fontSize = 16.sp)
                            }
                            Text(String.format(Locale.US, "%02d", minute), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { minute = (minute + 5) % 60 }) {
                                Text("▶", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hour, minute) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Set Time", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp)
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val isPm = hour >= 12
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (isPm) "PM" else "AM"
    return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
}

private fun formatTimeRange(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): String {
    return "${formatTime(startHour, startMinute)} – ${formatTime(endHour, endMinute)}"
}

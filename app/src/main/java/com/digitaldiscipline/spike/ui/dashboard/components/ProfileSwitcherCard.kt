package com.digitaldiscipline.spike.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.policy.profiles.PolicyProfileTemplate
import com.digitaldiscipline.spike.policy.profiles.PolicyProfileType
import com.digitaldiscipline.spike.policy.profiles.ProfileTemplateManager

@Composable
fun ProfileSwitcherCard(
    activeProfileStr: String,
    onSelectProfile: (PolicyProfileType, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProfileModal by remember { mutableStateOf(false) }

    val currentType = remember(activeProfileStr) {
        try {
            PolicyProfileType.valueOf(activeProfileStr.uppercase())
        } catch (_: Exception) {
            PolicyProfileType.CORPORATE
        }
    }

    val currentTemplate = remember(currentType) {
        ProfileTemplateManager.getTemplate(currentType)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F38)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(currentTemplate.iconEmoji, fontSize = 22.sp)
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = currentTemplate.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = currentTemplate.badgeText,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = currentTemplate.subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showProfileModal = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Switch", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showProfileModal) {
        ProfileSelectionDialog(
            currentType = currentType,
            onDismiss = { showProfileModal = false },
            onApply = { selectedType, appendMode ->
                onSelectProfile(selectedType, appendMode)
                showProfileModal = false
            }
        )
    }
}

@Composable
private fun ProfileSelectionDialog(
    currentType: PolicyProfileType,
    onDismiss: () -> Unit,
    onApply: (PolicyProfileType, Boolean) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var appendMode by remember { mutableStateOf(false) }

    val templates = remember { ProfileTemplateManager.getTemplates() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Discipline Profile", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Quick-start templates tailored for Workplace, Family, or Deep Work. 100% customizable.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                templates.forEach { template ->
                    val isSelected = selectedType == template.type
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF0C274A) else Color(0xFF1E293B).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = template.type }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(template.iconEmoji, fontSize = 20.sp)
                                    Text(
                                        text = template.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedType = template.type },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF38BDF8),
                                        unselectedColor = Color(0xFF64748B)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.description,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Highlight Badges
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                template.defaultSchedules.forEach { sched ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "🕒 ${sched.label}",
                                            color = Color(0xFFBAE6FD),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Append Mode Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { appendMode = !appendMode }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = appendMode,
                        onCheckedChange = { appendMode = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF0284C7),
                            uncheckedColor = Color(0xFF64748B)
                        )
                    )
                    Text(
                        text = "Keep existing rules & append profile schedules",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedType, appendMode) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Profile", color = Color.White, fontWeight = FontWeight.Bold)
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

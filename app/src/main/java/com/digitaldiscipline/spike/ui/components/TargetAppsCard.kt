package com.digitaldiscipline.spike.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode

@Composable
fun TargetAppsCard(
    rules: List<AppRuleEntity>,
    onToggleRule: (packageName: String, isEnabled: Boolean) -> Unit,
    onAddCustomPackage: (packageName: String, appName: String) -> Unit,
    onRemovePackage: (packageName: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TARGET APPLICATIONS (ROOM DB)",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Custom Target",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            rules.forEach { rule ->
                val statusText = when {
                    !rule.isEnabled -> "DISABLED"
                    rule.mode == RuleMode.ALLOW -> "ALLOWED"
                    rule.mode == RuleMode.DELAY -> "DELAY (${rule.unlockDurationSeconds}s)"
                    rule.mode == RuleMode.EARN -> "EARN (Squats)"
                    else -> "RESTRICTED (Intervention on launch)"
                }

                val statusColor = when {
                    !rule.isEnabled -> Color(0xFF64748B)
                    rule.mode == RuleMode.ALLOW -> Color(0xFF10B981)
                    else -> Color(0xFFEF4444)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        AppIconImage(
                            packageName = rule.packageName,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = rule.appDisplayName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = rule.packageName,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Mode: $statusText",
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { isChecked ->
                                onToggleRule(rule.packageName, isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2563EB)
                            )
                        )

                        if (rule.packageName != "com.instagram.android" && rule.packageName != "com.google.android.youtube") {
                            IconButton(
                                onClick = { onRemovePackage(rule.packageName) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)
            }
        }
    }

    if (showAddDialog) {
        var customPkg by remember { mutableStateOf("") }
        var customName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Target App", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("App Name (e.g. TikTok)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customPkg,
                        onValueChange = { customPkg = it },
                        label = { Text("Package (e.g. com.zhiliaoapp.musically)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPkg.isNotBlank()) {
                            onAddCustomPackage(
                                customPkg.trim(),
                                if (customName.isNotBlank()) customName.trim() else customPkg.trim()
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}

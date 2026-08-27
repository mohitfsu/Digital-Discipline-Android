package com.digitaldiscipline.spike.ui.dashboard

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.data.local.entities.UserMode
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.security.ParentPinManager
import com.digitaldiscipline.spike.security.PinVerificationResult
import com.digitaldiscipline.spike.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    preferencesManager: PreferencesManager,
    pinManager: ParentPinManager,
    syncManager: SyncManager,
    isA11yActive: Boolean,
    isOverlayActive: Boolean,
    onNavigateToPairing: () -> Unit,
    onOpenParentAdmin: () -> Unit,
    onSwitchMode: (UserMode) -> Unit
) {
    val pairedFamilyId by preferencesManager.pairedFamilyIdFlow.collectAsState(initial = null)
    val pairedChildName by preferencesManager.pairedChildNameFlow.collectAsState(initial = "")
    val isPaired = !pairedFamilyId.isNullOrBlank()
    val lastSync by preferencesManager.lastPolicySyncFlow.collectAsState(initial = 0L)
    val autoBlockGames by preferencesManager.autoBlockGamesFlow.collectAsState(initial = true)
    val autoBlockSocial by preferencesManager.autoBlockSocialFlow.collectAsState(initial = true)
    val autoBlockStreaming by preferencesManager.autoBlockStreamingFlow.collectAsState(initial = true)

    val isProtectionActive = isA11yActive && isOverlayActive

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Minimal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FAMILY PROTECTION",
                    color = Color(0xFF10B981),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isPaired) "Child: $pairedChildName" else "Child Device",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isProtectionActive) Color(0xFF059669).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (isProtectionActive) "🛡️" else "⚠️", fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF059669).copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.25f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🛡️", fontSize = 36.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Device Protection Active",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Screen time and distractions are governed by your parent's policy.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isProtectionActive) Color(0xFF059669).copy(alpha = 0.3f) else Color(0xFF7F1D1D).copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (isProtectionActive) Color(0xFF10B981) else Color(0xFFEF4444))
                ) {
                    Text(
                        text = if (isProtectionActive) "🟢 Offline Enforcement Running" else "⚠️ Permissions Needed (Ask Parent)",
                        color = if (isProtectionActive) Color(0xFF34D399) else Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Active Rules Card (Read-Only)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ACTIVE PARENTAL RULES",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Rule item 1: Games
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
                        Text("🎮", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Gaming Apps", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = if (autoBlockGames) "⛔ Blocked" else "✓ Allowed",
                        color = if (autoBlockGames) Color(0xFFF87171) else Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rule item 2: Social
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
                        Text("📸", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Social & Short Video", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = if (autoBlockSocial) "⛔ Blocked" else "✓ Allowed",
                        color = if (autoBlockSocial) Color(0xFFF87171) else Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rule item 3: Streaming
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
                        Text("🎬", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Streaming & Entertainment", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = if (autoBlockStreaming) "⛔ Blocked" else "✓ Allowed",
                        color = if (autoBlockStreaming) Color(0xFFF87171) else Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pairing & Sync Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isPaired) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isPaired) "☁️" else "🔗", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isPaired) "Paired to Parent" else "Not Paired to Parent",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPaired) {
                                    val timeStr = if (lastSync > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSync)) else "Recent"
                                    "Last synced: $timeStr"
                                } else {
                                    "Enter 6-digit pairing code from Parent phone"
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isPaired) {
                    Button(
                        onClick = {
                            syncManager.triggerImmediateSync()
                            Toast.makeText(context, "Checking for updated parent policies...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("🔄 1-Tap Sync Rules Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onNavigateToPairing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔗 Re-Pair / Enter New Pairing Code", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onNavigateToPairing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("🔗 Enter 6-Digit Pairing Code", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Discreet Parent PIN Gateway Button at bottom
        OutlinedButton(
            onClick = {
                enteredPin = ""
                pinError = null
                showPinDialog = true
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text("🔑 Parent Controls (PIN Required)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Parent PIN Verification Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Parent PIN Verification", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your 4-digit Parent PIN to open full controls:", color = Color(0xFF94A3B8), fontSize = 13.sp)

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) enteredPin = it },
                        placeholder = { Text("••••") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
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

                    if (pinError != null) {
                        Text(pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val verifyRes = pinManager.verifyPin(enteredPin)
                        if (verifyRes is PinVerificationResult.Success) {
                            showPinDialog = false
                            onOpenParentAdmin()
                        } else {
                            pinError = "Incorrect PIN. Please try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Unlock", color = Color.White, fontWeight = FontWeight.Bold)
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
}

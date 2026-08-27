package com.digitaldiscipline.spike.ui.onboarding

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.security.ParentPinManager

@Composable
fun OnboardingScreen(
    context: Context,
    policyRepository: PolicyRepository,
    pinManager: ParentPinManager,
    isAccessibilityGranted: Boolean = true,
    isOverlayGranted: Boolean = true,
    isUsageStatsGranted: Boolean = true,
    onCompleteOnboarding: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    var pinText by remember { mutableStateOf("") }
    var pinConfirmText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

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
            // Header Progress
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PARENT CONTROL SETUP",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { step / 2f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Step $step of 2",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step Content
            when (step) {
                1 -> {
                    // Step 1: Overview
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF059669).copy(alpha = 0.2f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👨‍👩‍👧", fontSize = 38.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Parent & Guardian Control",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "This phone acts as the remote management hub. No device-level monitoring permissions are required on this phone.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("WHAT YOU CAN DO FROM THIS PHONE:", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("• Auto-block games, social media, and video apps on child devices.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Configure school hours, bedtime, and study schedules.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Create single-use 6-digit pairing codes to link child phones.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Guard all settings with your custom 4-digit Parent PIN.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                            }
                        }
                    }
                }

                2 -> {
                    // Step 2: Parent PIN Creation
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF059669).copy(alpha = 0.2f),
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔒", fontSize = 32.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Create Your Parent PIN",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Choose a 4-digit PIN to secure all policies, schedules, and pairing controls.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = pinText,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinText = it },
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
                            value = pinConfirmText,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinConfirmText = it },
                            label = { Text("Confirm 4-Digit Parent PIN") },
                            placeholder = { Text("••••") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = pinError != null,
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

                        if (pinError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Actions
            Row(modifier = Modifier.fillMaxWidth()) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).height(48.dp).padding(end = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("Back", color = Color(0xFF94A3B8))
                    }
                }

                Button(
                    onClick = {
                        when (step) {
                            1 -> step = 2
                            2 -> {
                                if (pinText.length < 4) {
                                    pinError = "PIN must be exactly 4 digits"
                                } else if (pinText != pinConfirmText) {
                                    pinError = "PINs do not match"
                                } else {
                                    pinManager.setPin(pinText)
                                    Toast.makeText(context, "Parent PIN set successfully!", Toast.LENGTH_SHORT).show()
                                    onCompleteOnboarding()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp).padding(start = if (step > 1) 6.dp else 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (step == 2) "Finish & Open Dashboard" else "Continue",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

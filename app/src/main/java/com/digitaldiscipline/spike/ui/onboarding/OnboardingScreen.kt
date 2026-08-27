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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.security.ParentPinManager

@Composable
fun OnboardingScreen(
    context: Context,
    policyRepository: PolicyRepository,
    pinManager: ParentPinManager,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    isUsageStatsGranted: Boolean,
    onCompleteOnboarding: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var userConsented by remember { mutableStateOf(false) }

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
                        text = "FAMILY MODE SETUP",
                        color = Color(0xFF10B981),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { step / 5f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Step $step of 5",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step Content
            when (step) {
                1 -> {
                    // Step 1: Welcome & Mission
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF059669).copy(alpha = 0.2f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👨‍👩‍👧", fontSize = 40.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Family Protection &\nParental Boundaries",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Set study hours, auto-block distracting games & entertainment on your child's phone, and safeguard all rules with your custom Parent PIN.",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                2 -> {
                    // Step 2: Prominent Disclosure & Affirmative Consent
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📋", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Prominent Disclosure",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Digital Discipline uses the Android AccessibilityService API solely to detect when selected restricted apps (e.g. Instagram, YouTube, games) transition to the foreground so it can display a visible intervention screen.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🔒 Privacy Guarantee:", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Zero message or chat content is accessed.\n• Zero passwords or keystrokes are recorded.\n• Zero screen recordings or screenshots are taken.\n• Zero microphone or camera footage is collected.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Step 3: Permission Activation Wizard
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Enable Device Permissions",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Grant these 3 Android permissions to activate local protection:",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        PermissionCardItem(
                            title = "1. Accessibility Service",
                            desc = "Required for real-time app launch detection.",
                            isGranted = isAccessibilityGranted,
                            onGrant = {
                                PermissionGuideOverlay.show(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PermissionCardItem(
                            title = "2. Draw Over Apps (Overlay)",
                            desc = "Required to display the intervention screen.",
                            isGranted = isOverlayGranted,
                            onGrant = {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PermissionCardItem(
                            title = "3. Usage Access (Optional)",
                            desc = "Used for daily screen-time summaries.",
                            isGranted = isUsageStatsGranted,
                            onGrant = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        )
                    }
                }

                4 -> {
                    // Step 4: Default Target Apps Confirmation
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Default Managed Apps",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The system comes pre-configured with 3 targets (Earn Mode):",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        DefaultAppItem("com.instagram.android", "Instagram", "EARN Mode • 10 mins access")
                        Spacer(modifier = Modifier.height(8.dp))
                        DefaultAppItem("com.google.android.youtube", "YouTube", "EARN Mode • 15 mins access")
                        Spacer(modifier = Modifier.height(8.dp))
                        DefaultAppItem("com.dts.freefireth", "Gaming App (Free Fire)", "EARN Mode • 15 mins access")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You can customize or add apps anytime from the Admin Policy Dashboard.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                5 -> {
                    // Step 5: Admin PIN Creation
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Create Admin PIN",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This 4-digit PIN secures policy configuration and authorizes admin overrides.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = pinText,
                            onValueChange = { if (it.length <= 4) pinText = it },
                            label = { Text("Enter 4-Digit Admin PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pinConfirmText,
                            onValueChange = { if (it.length <= 4) pinConfirmText = it },
                            label = { Text("Confirm 4-Digit Admin PIN") },
                            singleLine = true,
                            isError = pinError != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pinError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp)
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
                                userConsented = true
                                step = 3
                            }
                            3 -> step = 4
                            4 -> step = 5
                            5 -> {
                                if (pinText.length < 4) {
                                    pinError = "PIN must be 4 digits"
                                } else if (pinText != pinConfirmText) {
                                    pinError = "PINs do not match"
                                } else {
                                    pinManager.setPin(pinText)
                                    onCompleteOnboarding()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp).padding(start = if (step > 1) 6.dp else 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = when (step) {
                            2 -> "I Agree & Continue"
                            5 -> "Finish & Activate"
                            else -> "Continue"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionCardItem(
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = desc, color = Color(0xFF64748B), fontSize = 11.sp)
            }
            if (isGranted) {
                Text("✓ ACTIVE", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("ENABLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DefaultAppItem(packageName: String, title: String, modeDesc: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.digitaldiscipline.spike.ui.components.AppIconImage(
                packageName = packageName,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(text = modeDesc, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

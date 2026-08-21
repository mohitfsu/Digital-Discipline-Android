package com.digitaldiscipline.spike.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusCard(
    isUsageStatsGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    devicePolicyAuthority: String,
    onRequestUsageStats: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestDeviceAdmin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DETECTION & PERMISSION STATUS",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                title = "UsageStats (Usage Access)",
                isGranted = isUsageStatsGranted,
                actionLabel = if (isUsageStatsGranted) "ENABLED" else "GRANT ACCESS",
                onClick = onRequestUsageStats
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                title = "Accessibility Service",
                isGranted = isAccessibilityGranted,
                actionLabel = if (isAccessibilityGranted) "ENABLED" else "ENABLE SERVICE",
                onClick = onRequestAccessibility
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                title = "Overlay (Draw Over Apps)",
                isGranted = isOverlayGranted,
                actionLabel = if (isOverlayGranted) "ENABLED" else "GRANT OVERLAY",
                onClick = onRequestOverlay
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Policy Authority",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = devicePolicyAuthority,
                        color = if (devicePolicyAuthority.contains("DEVICE_OWNER")) Color(0xFF10B981) else Color(0xFFFBBF24),
                        fontSize = 12.sp
                    )
                }
                TextButton(
                    onClick = onRequestDeviceAdmin,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Text("MANAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    isGranted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Badge(
                containerColor = if (isGranted) Color(0xFF059669) else Color(0xFFEF4444)
            ) {
                Text(
                    text = if (isGranted) "✓" else "!",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (isGranted) {
            Text(
                text = actionLabel,
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

package com.digitaldiscipline.spike.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.detection.DetectorType

@Composable
fun TestControlPanel(
    activeDetectorType: DetectorType,
    onSelectDetector: (DetectorType) -> Unit,
    onSelectPollingInterval: (Long) -> Unit,
    onTriggerTestOverlay: () -> Unit,
    onTriggerTestUnlock: () -> Unit,
    onTriggerTestBlock: () -> Unit,
    onTestDevicePolicy: () -> Unit
) {
    var selectedInterval by remember { mutableLongStateOf(500L) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TEST CONTROLS & EXPERIMENTS",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Active Detection Engine:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onSelectDetector(DetectorType.ACCESSIBILITY) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeDetectorType == DetectorType.ACCESSIBILITY) Color(0xFF2563EB) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("Accessibility (Push)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onSelectDetector(DetectorType.USAGE_STATS) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeDetectorType == DetectorType.USAGE_STATS) Color(0xFF2563EB) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("UsageStats (Poll)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeDetectorType == DetectorType.USAGE_STATS) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "UsageStats Polling Interval:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(250L, 500L, 1000L, 2000L).forEach { interval ->
                        FilterChip(
                            selected = selectedInterval == interval,
                            onClick = {
                                selectedInterval = interval
                                onSelectPollingInterval(interval)
                            },
                            label = { Text("${interval}ms", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Enforcement & State Triggers:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onTriggerTestOverlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("Test Overlay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onTriggerTestUnlock,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("60s Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onTriggerTestBlock,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Text("Force Block", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTestDevicePolicy,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Text("Test Device Policy", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.digitaldiscipline.spike.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.detection.AppLaunchEvent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ForegroundMonitorCard(
    currentEvent: AppLaunchEvent?,
    lastLatencyMs: Long
) {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val timeStr = if (currentEvent != null) sdf.format(Date(currentEvent.detectionTimestamp)) else "None"
    val pkgName = currentEvent?.packageName ?: "No app detected yet"
    val sourceStr = currentEvent?.source?.name ?: "N/A"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LIVE FOREGROUND MONITOR",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Current Top Package:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Text(
                        text = pkgName,
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Source", value = sourceStr, valueColor = Color.White)
                MetricItem(label = "Last Detected", value = timeStr, valueColor = Color.White)
                MetricItem(
                    label = "Latency",
                    value = "${lastLatencyMs}ms",
                    valueColor = if (lastLatencyMs < 100) Color(0xFF10B981) else Color(0xFFFBBF24)
                )
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

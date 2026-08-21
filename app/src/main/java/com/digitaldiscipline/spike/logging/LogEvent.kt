package com.digitaldiscipline.spike.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEvent(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,
    val packageName: String,
    val eventType: String,
    val latencyMs: Long? = null,
    val details: String = ""
) {
    fun toFormattedString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timeStr = sdf.format(Date(timestamp))
        val latencyStr = if (latencyMs != null) " | LATENCY=${latencyMs}ms" else ""
        val detailsStr = if (details.isNotEmpty()) " | $details" else ""
        return "$timeStr | SOURCE=$source | PACKAGE=$packageName | EVENT=$eventType$latencyStr$detailsStr"
    }
}

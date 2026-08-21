package com.digitaldiscipline.spike.detection

enum class DetectorType {
    USAGE_STATS,
    ACCESSIBILITY
}

data class AppLaunchEvent(
    val packageName: String,
    val className: String? = null,
    val eventTimestamp: Long,
    val detectionTimestamp: Long = System.currentTimeMillis(),
    val source: DetectorType,
    val eventType: String = "FOREGROUND"
) {
    val latencyMs: Long
        get() = (detectionTimestamp - eventTimestamp).coerceAtLeast(0)
}

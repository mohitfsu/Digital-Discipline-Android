package com.digitaldiscipline.spike.detection

interface AppLaunchDetector {
    fun startMonitoring(callback: (AppLaunchEvent) -> Unit)
    fun stopMonitoring()
    fun isPermissionGranted(): Boolean
    fun getDetectorType(): DetectorType
    fun isRunning(): Boolean
}

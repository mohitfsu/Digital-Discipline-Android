package com.digitaldiscipline.spike.detection

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.digitaldiscipline.spike.logging.EventLogger

class AccessibilityLaunchDetector(
    private val context: Context
) : AppLaunchDetector {

    private var activeCallback: ((AppLaunchEvent) -> Unit)? = null
    private var isRegistered = false

    override fun startMonitoring(callback: (AppLaunchEvent) -> Unit) {
        activeCallback = callback
        if (!isRegistered) {
            DigitalDisciplineAccessibilityService.registerListener(eventDispatcher)
            isRegistered = true
        }

        EventLogger.log(
            source = "ACCESSIBILITY",
            packageName = "system",
            eventType = "LISTENER_ATTACHED",
            details = "Service active=${DigitalDisciplineAccessibilityService.isServiceRunning()}"
        )
    }

    override fun stopMonitoring() {
        if (isRegistered) {
            DigitalDisciplineAccessibilityService.unregisterListener(eventDispatcher)
            isRegistered = false
        }
        activeCallback = null

        EventLogger.log(
            source = "ACCESSIBILITY",
            packageName = "system",
            eventType = "LISTENER_DETACHED"
        )
    }

    override fun isPermissionGranted(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val serviceName = "${context.packageName}/${DigitalDisciplineAccessibilityService::class.java.canonicalName}"
        return enabledServices.any { service ->
            service.resolveInfo.serviceInfo.packageName == context.packageName
        } || DigitalDisciplineAccessibilityService.isServiceRunning()
    }

    override fun getDetectorType(): DetectorType = DetectorType.ACCESSIBILITY

    override fun isRunning(): Boolean = isRegistered && DigitalDisciplineAccessibilityService.isServiceRunning()

    private val eventDispatcher: (AppLaunchEvent) -> Unit = { event ->
        activeCallback?.invoke(event)
    }
}

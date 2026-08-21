package com.digitaldiscipline.spike.security

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.ui.onboarding.PermissionGuideOverlay

object SettingsWatchdogGuard {

    val SETTINGS_PACKAGES = setOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.vivo.permissionmanager",
        "com.samsung.android.settings",
        "com.miui.securitycenter",
        "com.coloros.safecenter",
        "com.iqoo.secure"
    )

    private val DESTRUCTIVE_TAMPER_KEYWORDS = listOf(
        "force stop",
        "uninstall",
        "clear data",
        "clear storage",
        "delete data"
    )

    private val ALLOWED_SETTINGS_KEYWORDS = listOf(
        "accessibility",
        "downloaded",
        "display over",
        "overlay",
        "special app access",
        "draw over other apps",
        "turn on",
        "allow"
    )

    private var lastInterceptTime: Long = 0L

    fun isSettingsPackage(packageName: String?): Boolean {
        return packageName != null && SETTINGS_PACKAGES.contains(packageName)
    }

    fun checkAndInterceptTamperAttempt(
        service: AccessibilityService,
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        isProtectionEnabled: Boolean
    ): Boolean {
        if (!isProtectionEnabled || !isSettingsPackage(packageName) || rootNode == null) {
            return false
        }

        // Never intercept while the user is actively following the permission guide overlay
        if (PermissionGuideOverlay.isShowing()) {
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastInterceptTime < 2000L) {
            return false // Deduplicate within 2s
        }

        val allWindowText = collectAllNodeText(rootNode).lowercase()

        // If this is an accessibility or permission setup screen, allow user free access
        for (allowed in ALLOWED_SETTINGS_KEYWORDS) {
            if (allWindowText.contains(allowed)) {
                return false
            }
        }

        // Only intercept if screen explicitly mentions Digital Discipline AND a destructive action (Force stop, Uninstall, Clear data)
        if (allWindowText.contains("digital discipline")) {
            for (destructive in DESTRUCTIVE_TAMPER_KEYWORDS) {
                if (allWindowText.contains(destructive)) {
                    lastInterceptTime = now
                    EventLogger.log(
                        source = "SECURITY_WATCHDOG",
                        packageName = packageName,
                        eventType = "TAMPER_ATTEMPT_BLOCKED",
                        details = "Blocked attempt to $destructive Digital Discipline in settings"
                    )

                    // Redirect safely to home screen to prevent tampering
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    return true
                }
            }
        }

        return false
    }

    private fun collectAllNodeText(node: AccessibilityNodeInfo, depth: Int = 0): String {
        if (depth > 12) return "" // Guard against excessive recursion
        val builder = StringBuilder()
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (!text.isNullOrBlank()) builder.append(text).append(" ")
        if (!desc.isNullOrBlank()) builder.append(desc).append(" ")

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            builder.append(collectAllNodeText(child, depth + 1)).append(" ")
            try {
                child.recycle()
            } catch (_: Throwable) {}
        }
        return builder.toString()
    }
}

package com.digitaldiscipline.spike.detection

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.security.SettingsWatchdogGuard
import com.digitaldiscipline.spike.ui.onboarding.PermissionGuideOverlay
import java.util.concurrent.CopyOnWriteArrayList

class DigitalDisciplineAccessibilityService : AccessibilityService() {

    companion object {
        var instance: DigitalDisciplineAccessibilityService? = null
            private set

        private val listeners = CopyOnWriteArrayList<(AppLaunchEvent) -> Unit>()
        var onPeriodicPulse: ((String) -> Unit)? = null
        var isTamperProtectionEnabled: Boolean = true

        fun registerListener(listener: (AppLaunchEvent) -> Unit) {
            listeners.add(listener)
        }

        fun unregisterListener(listener: (AppLaunchEvent) -> Unit) {
            listeners.remove(listener)
        }

        fun isServiceRunning(): Boolean = instance != null

        fun resetLastPackage() {
            instance?.lastPackageName = null
            instance?.lastEventTime = 0L
        }

        val SYSTEM_OVERLAY_PACKAGES = setOf(
            "com.android.systemui",
            "android",
            "com.android.keyguard",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.touchtype.swiftkey",
            "com.android.inputmethod",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )
    }

    private var lastPackageName: String? = null
    private var lastEventTime: Long = 0L
    private var lastBrowserDomain: String? = null
    private var lastBrowserScanTime: Long = 0L

    private val pulseHandler = Handler(Looper.getMainLooper())
    private val periodicPulse = object : Runnable {
        override fun run() {
            try {
                val currentPkg = lastPackageName
                if (currentPkg != null && !SYSTEM_OVERLAY_PACKAGES.contains(currentPkg) && currentPkg != packageName) {
                    onPeriodicPulse?.invoke(currentPkg)
                }
            } catch (t: Throwable) {
                // Catch all to prevent crashing the accessibility service
            } finally {
                pulseHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onServiceConnected() {
        try {
            super.onServiceConnected()
            instance = this

            // Dismiss floating permission guidance overlay once connected
            PermissionGuideOverlay.dismiss(this)

            pulseHandler.removeCallbacks(periodicPulse)
            pulseHandler.post(periodicPulse)
            com.digitaldiscipline.spike.logging.DiagnosticLogger.logServiceStarted("DigitalDisciplineAccessibilityService")
            EventLogger.log(
                source = "ACCESSIBILITY",
                packageName = packageName,
                eventType = "SERVICE_CONNECTED",
                details = "Real-time window state listener active with browser scanner and tamper watchdog"
            )
        } catch (t: Throwable) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return

            val pkgName = event.packageName?.toString() ?: return
            val clsName = event.className?.toString()
            val eventType = event.eventType
            val receiptTime = System.currentTimeMillis()

            // 1. Settings Tamper Watchdog
            if (SettingsWatchdogGuard.isSettingsPackage(pkgName)) {
                val intercepted = SettingsWatchdogGuard.checkAndInterceptTamperAttempt(
                    service = this,
                    rootNode = rootInActiveWindow,
                    packageName = pkgName,
                    isProtectionEnabled = isTamperProtectionEnabled
                )
                if (intercepted) {
                    return
                }
            }

            // 2. Mobile Web / Browser URL Domain Scanner
            if (BrowserDomainScanner.isBrowserPackage(pkgName)) {
                if (receiptTime - lastBrowserScanTime > 350L) {
                    lastBrowserScanTime = receiptTime
                    val matchedDomain = BrowserDomainScanner.scanForRestrictedDomain(rootInActiveWindow, pkgName)
                    if (matchedDomain != null && matchedDomain != lastBrowserDomain) {
                        lastBrowserDomain = matchedDomain
                        val mappedPackage = mapDomainToDistractionPackage(matchedDomain)
                        val webLaunchEvent = AppLaunchEvent(
                            packageName = mappedPackage,
                            className = "WebRestricted:$matchedDomain",
                            eventTimestamp = receiptTime,
                            detectionTimestamp = receiptTime,
                            source = DetectorType.ACCESSIBILITY,
                            eventType = "TYPE_BROWSER_RESTRICTED_DOMAIN"
                        )
                        EventLogger.log(
                            source = "BROWSER_SCANNER",
                            packageName = mappedPackage,
                            eventType = "RESTRICTED_WEB_DOMAIN",
                            details = "Domain=$matchedDomain via $pkgName"
                        )
                        notifyListeners(webLaunchEvent)
                        return
                    }
                }
            }

            // 3. Window State Changes (Standard App Launch Detection)
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // Calculate approximate event time using SystemClock and event.eventTime
                val eventTime = if (event.eventTime > 0) {
                    val deltaUptime = SystemClock.uptimeMillis() - event.eventTime
                    receiptTime - deltaUptime
                } else {
                    receiptTime
                }

                val latency = (receiptTime - eventTime).coerceAtLeast(0)

                // Ignore our own app UI transitions and system overlays/keyboards without poisoning lastPackageName
                if (pkgName == packageName || SYSTEM_OVERLAY_PACKAGES.contains(pkgName)) {
                    return
                }

                // Deduplicate consecutive events for identical package within short burst (60ms)
                if (pkgName != lastPackageName || (receiptTime - lastEventTime) > 60L) {
                    lastPackageName = pkgName
                    lastEventTime = receiptTime
                    lastBrowserDomain = null

                    val launchEvent = AppLaunchEvent(
                        packageName = pkgName,
                        className = clsName,
                        eventTimestamp = eventTime,
                        detectionTimestamp = receiptTime,
                        source = DetectorType.ACCESSIBILITY,
                        eventType = "TYPE_WINDOW_STATE_CHANGED"
                    )

                    EventLogger.log(
                        source = "ACCESSIBILITY",
                        packageName = pkgName,
                        eventType = "FOREGROUND",
                        latencyMs = latency,
                        details = "Class=${clsName ?: "N/A"}"
                    )

                    notifyListeners(launchEvent)
                }
            }
        } catch (t: Throwable) {
            // Absolute guard against crashing the accessibility service
        }
    }

    private fun mapDomainToDistractionPackage(domain: String): String {
        return when {
            domain.contains("instagram") -> "com.instagram.android"
            domain.contains("youtube") -> "com.google.android.youtube"
            domain.contains("tiktok") -> "com.zhiliaoapp.musically"
            domain.contains("facebook") -> "com.facebook.katana"
            domain.contains("twitter") || domain.contains("x.com") -> "com.twitter.android"
            domain.contains("reddit") -> "com.reddit.frontpage"
            domain.contains("snapchat") -> "com.snapchat.android"
            domain.contains("pinterest") -> "com.pinterest"
            else -> "com.instagram.android"
        }
    }

    private fun notifyListeners(event: AppLaunchEvent) {
        listeners.forEach { listener ->
            try {
                listener(event)
            } catch (_: Throwable) {}
        }
    }

    override fun onInterrupt() {
        EventLogger.log(
            source = "ACCESSIBILITY",
            packageName = packageName,
            eventType = "SERVICE_INTERRUPTED"
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        pulseHandler.removeCallbacks(periodicPulse)
        instance = null
        com.digitaldiscipline.spike.logging.DiagnosticLogger.logAccessibilityDisabled()
        EventLogger.log(
            source = "ACCESSIBILITY",
            packageName = packageName,
            eventType = "SERVICE_UNBOUND"
        )
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseHandler.removeCallbacks(periodicPulse)
        instance = null
        com.digitaldiscipline.spike.logging.DiagnosticLogger.logServiceStopped("DigitalDisciplineAccessibilityService")
        EventLogger.log(
            source = "ACCESSIBILITY",
            packageName = packageName,
            eventType = "SERVICE_DESTROYED"
        )
    }
}

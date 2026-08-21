package com.digitaldiscipline.spike.tamper

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.digitaldiscipline.spike.data.local.dao.ProtectionStateDao
import com.digitaldiscipline.spike.data.local.entities.ProtectionStateEntity
import com.digitaldiscipline.spike.detection.DigitalDisciplineAccessibilityService
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.flow.Flow

class TamperDetector(
    private val context: Context,
    private val protectionStateDao: ProtectionStateDao
) {

    fun getProtectionStateFlow(): Flow<ProtectionStateEntity?> = protectionStateDao.getProtectionStateFlow()

    fun isAccessibilityActive(): Boolean {
        if (DigitalDisciplineAccessibilityService.isServiceRunning()) {
            return true
        }

        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            if (enabledServices?.any { it.resolveInfo?.serviceInfo?.packageName == context.packageName } == true) {
                return true
            }
        } catch (_: Throwable) {}

        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            if (enabledServicesSetting.contains(context.packageName)) {
                return true
            }
        } catch (_: Throwable) {}

        return false
    }

    suspend fun verifyProtectionHealth(): ProtectionStateEntity {
        val isA11yActive = isAccessibilityActive()
        val isOverlayActive = Settings.canDrawOverlays(context)

        val existing = protectionStateDao.getProtectionState()
        val isEnabledByParent = existing?.isProtectionEnabledByParent ?: true

        val updated = ProtectionStateEntity(
            id = 1,
            isAccessibilityActive = isA11yActive,
            isOverlayActive = isOverlayActive,
            isUsageStatsActive = true,
            isProtectionEnabledByParent = isEnabledByParent,
            lastHeartbeatElapsedRealtime = android.os.SystemClock.elapsedRealtime(),
            lastTamperCheckTimestamp = System.currentTimeMillis()
        )

        protectionStateDao.updateProtectionState(updated)

        if (isEnabledByParent && !isA11yActive) {
            EventLogger.log(
                source = "TAMPER_DETECTOR",
                packageName = context.packageName,
                eventType = "PROTECTION_DISABLED_ALERT",
                details = "Accessibility permission disabled while parental protection is active."
            )
        }

        return updated
    }
}

package com.digitaldiscipline.spike.policy

import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.overlay.OverlayManager

class OverlayEnforcementStrategy(
    private val overlayManager: OverlayManager
) : AppEnforcementStrategy {

    override fun enforceRestriction(
        packageName: String,
        appDisplayName: String,
        unlockDurationSeconds: Int,
        attemptNumber: Int,
        ruleMode: RuleMode,
        pauseDurationSeconds: Int,
        breathingDurationSeconds: Int,
        squatsTargetCount: Int
    ) {
        overlayManager.showInterventionOverlay(
            targetPackage = packageName,
            targetAppName = appDisplayName,
            unlockDurationSeconds = unlockDurationSeconds,
            attemptNumber = attemptNumber,
            ruleMode = ruleMode,
            pauseDurationSeconds = pauseDurationSeconds,
            breathingDurationSeconds = breathingDurationSeconds,
            squatsTargetCount = squatsTargetCount
        )
    }

    override fun liftRestriction(packageName: String) {
        overlayManager.hideOverlay()
    }

    override fun isEnforcing(): Boolean = overlayManager.isOverlayActive()

    override fun getStrategyName(): String = "OVERLAY_WINDOW"
}

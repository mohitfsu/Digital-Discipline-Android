package com.digitaldiscipline.spike.policy

import com.digitaldiscipline.spike.data.local.entities.RuleMode

class DevicePolicyEnforcementStrategy(
    private val dpmWrapper: DevicePolicyManagerWrapper
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
        dpmWrapper.suspendPackage(packageName)
    }

    override fun liftRestriction(packageName: String) {
        dpmWrapper.unsuspendPackage(packageName)
    }

    override fun isEnforcing(): Boolean = dpmWrapper.isDeviceOwner()

    override fun getStrategyName(): String = "DEVICE_POLICY_SUSPENSION"
}

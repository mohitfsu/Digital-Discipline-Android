package com.digitaldiscipline.spike.policy

import com.digitaldiscipline.spike.data.local.entities.RuleMode

interface AppEnforcementStrategy {
    fun enforceRestriction(
        packageName: String,
        appDisplayName: String,
        unlockDurationSeconds: Int,
        attemptNumber: Int = 1,
        ruleMode: RuleMode = RuleMode.EARN,
        pauseDurationSeconds: Int = 10,
        breathingDurationSeconds: Int = 30,
        squatsTargetCount: Int = 10
    )
    fun liftRestriction(packageName: String)
    fun isEnforcing(): Boolean
    fun getStrategyName(): String
}

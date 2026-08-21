package com.digitaldiscipline.spike.intervention.adaptive

import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService

class TriggerContextBuilder(
    private val adaptiveStore: InterventionAdaptiveStore,
    private val walletService: EarnedTimeWalletService? = null
) {
    fun build(
        triggerId: String,
        targetPackage: String,
        policySource: PolicySource = PolicySource.SELF,
        configuredInterventionId: String? = null,
        timestampMs: Long = System.currentTimeMillis()
    ): InterventionContext {
        val recentIds = adaptiveStore.getRecentInterventionIds(5)
        val balanceSec = 0 // Wallet balance can be read asynchronously if needed

        return InterventionContext(
            triggerId = triggerId,
            targetPackage = targetPackage,
            timestampMs = timestampMs,
            policySource = policySource,
            configuredInterventionId = configuredInterventionId,
            recentInterventionIds = recentIds,
            walletBalanceSeconds = balanceSec
        )
    }
}

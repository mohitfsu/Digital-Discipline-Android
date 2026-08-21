package com.digitaldiscipline.spike.intervention.adaptive

import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.intervention.session.SessionState

enum class HelpfulnessFeedback {
    HELPED,
    NEUTRAL,
    DID_NOT_HELP,
    NOT_ASKED
}

data class InterventionOutcome(
    val sessionId: String,
    val interventionId: String,
    val targetPackage: String,
    val policySource: PolicySource = PolicySource.SELF,
    val startedAtRealtimeMs: Long,
    val completedAtRealtimeMs: Long,
    val status: SessionState,
    val rewardSeconds: Int,
    val helpfulness: HelpfulnessFeedback = HelpfulnessFeedback.NOT_ASKED,
    val timestampMs: Long = System.currentTimeMillis()
)

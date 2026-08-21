package com.digitaldiscipline.spike.intervention.policy

import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.intervention.model.InterventionDifficulty
import com.digitaldiscipline.spike.intervention.session.PolicySource

data class InterventionPolicy(
    val policyId: String,
    val policySource: PolicySource,
    val targetPackage: String,
    val intervention: InterventionDefinition,
    val difficulty: InterventionDifficulty = InterventionDifficulty.STANDARD,
    val rewardSeconds: Int = 600,
    val cooldownSeconds: Int = 0,
    val isHardBlock: Boolean = false,
    val isDelayOnly: Boolean = false,
    val enabled: Boolean = true
)

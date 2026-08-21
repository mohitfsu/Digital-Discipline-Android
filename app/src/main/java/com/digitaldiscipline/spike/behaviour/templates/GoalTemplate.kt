package com.digitaldiscipline.spike.behaviour.templates

import com.digitaldiscipline.spike.data.local.entities.GoalCategory
import com.digitaldiscipline.spike.data.local.entities.ReplacementBehaviourEntity
import com.digitaldiscipline.spike.data.local.entities.TriggerCategory

enum class RewardPreset(
    val id: String,
    val displayName: String,
    val rewardMinutes: Int,
    val rewardSeconds: Int,
    val dailyCapMinutes: Int,
    val dailyCapSeconds: Int,
    val sessionCapMinutes: Int,
    val sessionCapSeconds: Int,
    val description: String
) {
    LIGHT(
        id = "LIGHT",
        displayName = "Light Focus",
        rewardMinutes = 5,
        rewardSeconds = 300,
        dailyCapMinutes = 20,
        dailyCapSeconds = 1200,
        sessionCapMinutes = 15,
        sessionCapSeconds = 900,
        description = "5 min reward • 20 min daily cap • 15 min session cap"
    ),
    STANDARD(
        id = "STANDARD",
        displayName = "Standard Balance",
        rewardMinutes = 10,
        rewardSeconds = 600,
        dailyCapMinutes = 30,
        dailyCapSeconds = 1800,
        sessionCapMinutes = 15,
        sessionCapSeconds = 900,
        description = "10 min reward • 30 min daily cap • 15 min session cap"
    ),
    STRONG(
        id = "STRONG",
        displayName = "Strong Discipline",
        rewardMinutes = 10,
        rewardSeconds = 600,
        dailyCapMinutes = 30,
        dailyCapSeconds = 1800,
        sessionCapMinutes = 10,
        sessionCapSeconds = 600,
        description = "10 min reward • 30 min daily cap • 10 min session cap"
    ),
    GENEROUS(
        id = "GENEROUS",
        displayName = "Generous Access",
        rewardMinutes = 15,
        rewardSeconds = 900,
        dailyCapMinutes = 45,
        dailyCapSeconds = 2700,
        sessionCapMinutes = 20,
        sessionCapSeconds = 1200,
        description = "15 min reward • 45 min daily cap • 20 min session cap"
    )
}

data class DistractionAppRecommendation(
    val packageName: String,
    val displayName: String,
    val icon: String,
    val category: TriggerCategory
)

data class GoalTemplate(
    val templateId: String,
    val category: GoalCategory,
    val name: String,
    val shortDescription: String,
    val icon: String,
    val defaultUnit: String,
    val defaultDailyTarget: Int,
    val defaultWeeklyTarget: Int,
    val recommendedReplacementBehaviours: List<ReplacementBehaviourEntity>,
    val recommendedTriggerCategories: List<TriggerCategory>,
    val defaultRewardPreset: RewardPreset = RewardPreset.STANDARD,
    val defaultDailyEarnCapSeconds: Int = 1800,
    val defaultWalletCapSeconds: Int = 3600,
    val defaultSessionCapSeconds: Int = 900,
    val recommendedSchedule: String? = null,
    val onboardingCopy: String
)

package com.digitaldiscipline.spike.policy.profiles

import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.ScheduleEntity

enum class PolicyProfileType {
    CORPORATE,
    FAMILY,
    DEEP_WORK,
    CUSTOM
}

data class PolicyProfileTemplate(
    val type: PolicyProfileType,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val badgeText: String,
    val description: String,
    val defaultRules: List<AppRuleEntity>,
    val defaultSchedules: List<ScheduleEntity>
)

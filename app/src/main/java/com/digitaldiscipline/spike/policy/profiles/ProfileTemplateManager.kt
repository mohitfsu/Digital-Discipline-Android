package com.digitaldiscipline.spike.policy.profiles

import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.ScheduleEntity
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.policy.PolicyRepository

object ProfileTemplateManager {

    fun getTemplates(): List<PolicyProfileTemplate> {
        return listOf(
            getCorporateTemplate(),
            getFamilyTemplate(),
            getDeepWorkTemplate()
        )
    }

    fun getTemplate(type: PolicyProfileType): PolicyProfileTemplate {
        return when (type) {
            PolicyProfileType.CORPORATE -> getCorporateTemplate()
            PolicyProfileType.FAMILY -> getFamilyTemplate()
            PolicyProfileType.DEEP_WORK -> getDeepWorkTemplate()
            PolicyProfileType.CUSTOM -> getCustomTemplate()
        }
    }

    private fun getCorporateTemplate(): PolicyProfileTemplate {
        return PolicyProfileTemplate(
            type = PolicyProfileType.CORPORATE,
            title = "Corporate & Workplace",
            subtitle = "Professional focus during business hours",
            iconEmoji = "🏢",
            badgeText = "OFFICE MODE",
            description = "Enforces strict distraction blocking during office hours (9 AM – 5 PM Mon–Fri). Ideal for managed company devices and workplace compliance.",
            defaultRules = listOf(
                AppRuleEntity(
                    packageName = "com.instagram.android",
                    appDisplayName = "Instagram",
                    mode = RuleMode.BLOCK,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 300,
                    interventionType = "PAUSE",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                ),
                AppRuleEntity(
                    packageName = "com.google.android.youtube",
                    appDisplayName = "YouTube",
                    mode = RuleMode.DELAY,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 600,
                    interventionType = "BREATHING",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                ),
                AppRuleEntity(
                    packageName = "com.dts.freefireth",
                    appDisplayName = "Gaming (Free Fire)",
                    mode = RuleMode.BLOCK,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 300,
                    interventionType = "SQUATS",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                )
            ),
            defaultSchedules = listOf(
                ScheduleEntity(
                    label = "Office Hours",
                    packageName = "ALL_RESTRICTED",
                    dayOfWeek = 2,
                    daysOfWeekCsv = "2,3,4,5,6", // Mon-Fri
                    startHour = 9,
                    startMinute = 0,
                    endHour = 17,
                    endMinute = 0,
                    isBlocked = true,
                    restrictionMode = "BLOCK",
                    isEnabled = true
                )
            )
        )
    }

    private fun getFamilyTemplate(): PolicyProfileTemplate {
        return PolicyProfileTemplate(
            type = PolicyProfileType.FAMILY,
            title = "Family & Parenting",
            subtitle = "Healthy screen habits, homework & sleep lock",
            iconEmoji = "👨‍👩‍👧",
            badgeText = "FAMILY MODE",
            description = "Balances study time (5 PM – 8:30 PM), school hours (8 AM – 2 PM), and bedtime lock (10 PM – 6 AM). Rewards screen time through mindful exercise.",
            defaultRules = listOf(
                AppRuleEntity(
                    packageName = "com.instagram.android",
                    appDisplayName = "Instagram",
                    mode = RuleMode.EARN,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 600,
                    interventionType = "PAUSE",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                ),
                AppRuleEntity(
                    packageName = "com.google.android.youtube",
                    appDisplayName = "YouTube",
                    mode = RuleMode.EARN,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 900,
                    interventionType = "BREATHING",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                ),
                AppRuleEntity(
                    packageName = "com.dts.freefireth",
                    appDisplayName = "Gaming (Free Fire)",
                    mode = RuleMode.EARN,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 900,
                    interventionType = "SQUATS",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                )
            ),
            defaultSchedules = listOf(
                ScheduleEntity(
                    label = "School Hours",
                    packageName = "ALL_RESTRICTED",
                    dayOfWeek = 2,
                    daysOfWeekCsv = "2,3,4,5,6", // Mon-Fri
                    startHour = 8,
                    startMinute = 0,
                    endHour = 14,
                    endMinute = 0,
                    isBlocked = true,
                    restrictionMode = "BLOCK",
                    isEnabled = true
                ),
                ScheduleEntity(
                    label = "Evening Study",
                    packageName = "ALL_RESTRICTED",
                    dayOfWeek = 2,
                    daysOfWeekCsv = "2,3,4,5,6,7", // Mon-Sat
                    startHour = 17,
                    startMinute = 0,
                    endHour = 20,
                    endMinute = 30,
                    isBlocked = true,
                    restrictionMode = "INTERVENE",
                    isEnabled = true
                ),
                ScheduleEntity(
                    label = "Bedtime Lock",
                    packageName = "ALL_RESTRICTED",
                    dayOfWeek = 1,
                    daysOfWeekCsv = "1,2,3,4,5,6,7", // Everyday
                    startHour = 22,
                    startMinute = 0,
                    endHour = 6,
                    endMinute = 0,
                    isBlocked = true,
                    restrictionMode = "BLOCK",
                    isEnabled = true
                )
            )
        )
    }

    private fun getDeepWorkTemplate(): PolicyProfileTemplate {
        return PolicyProfileTemplate(
            type = PolicyProfileType.DEEP_WORK,
            title = "Personal & Deep Work",
            subtitle = "Flow state blocks with physical micro-resets",
            iconEmoji = "🎯",
            badgeText = "DEEP WORK",
            description = "Creates structured morning and afternoon focus blocks with 30s Camera AI movement micro-resets to break dopamine loops.",
            defaultRules = listOf(
                AppRuleEntity(
                    packageName = "com.instagram.android",
                    appDisplayName = "Instagram",
                    mode = RuleMode.DELAY,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 600,
                    interventionType = "PAUSE",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                ),
                AppRuleEntity(
                    packageName = "com.google.android.youtube",
                    appDisplayName = "YouTube",
                    mode = RuleMode.EARN,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 600,
                    interventionType = "BREATHING",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                ),
                AppRuleEntity(
                    packageName = "com.dts.freefireth",
                    appDisplayName = "Gaming (Free Fire)",
                    mode = RuleMode.BLOCK,
                    isEnabled = true,
                    dailyLimitMinutes = 0,
                    unlockDurationSeconds = 300,
                    interventionType = "SQUATS",
                    pauseDurationSeconds = 30,
                    breathingDurationSeconds = 30,
                    squatsTargetCount = 15
                )
            ),
            defaultSchedules = listOf(
                ScheduleEntity(
                    label = "Morning Focus Block",
                    packageName = "ALL_RESTRICTED",
                    dayOfWeek = 2,
                    daysOfWeekCsv = "2,3,4,5,6", // Mon-Fri
                    startHour = 9,
                    startMinute = 0,
                    endHour = 12,
                    endMinute = 30,
                    isBlocked = true,
                    restrictionMode = "INTERVENE",
                    isEnabled = true
                ),
                ScheduleEntity(
                    label = "Afternoon Sprint",
                    packageName = "ALL_RESTRICTED",
                    dayOfWeek = 2,
                    daysOfWeekCsv = "2,3,4,5,6", // Mon-Fri
                    startHour = 14,
                    startMinute = 30,
                    endHour = 17,
                    endMinute = 30,
                    isBlocked = true,
                    restrictionMode = "INTERVENE",
                    isEnabled = true
                )
            )
        )
    }

    private fun getCustomTemplate(): PolicyProfileTemplate {
        return PolicyProfileTemplate(
            type = PolicyProfileType.CUSTOM,
            title = "Custom Workspace",
            subtitle = "Personalized rules and time windows",
            iconEmoji = "⚙️",
            badgeText = "CUSTOM",
            description = "Customized policy with individual rules and tailored schedules.",
            defaultRules = emptyList(),
            defaultSchedules = emptyList()
        )
    }

    suspend fun applyProfile(
        type: PolicyProfileType,
        policyRepository: PolicyRepository,
        preferencesManager: PreferencesManager,
        appendMode: Boolean = false
    ) {
        val template = getTemplate(type)
        if (template.defaultRules.isNotEmpty() || template.defaultSchedules.isNotEmpty()) {
            if (appendMode) {
                // Insert without clearing existing custom rules
                template.defaultRules.forEach { policyRepository.saveRule(it) }
                template.defaultSchedules.forEach { policyRepository.saveSchedule(it) }
            } else {
                // Transactional Atomic Replacement
                policyRepository.transactionalUpdatePolicy(template.defaultRules, template.defaultSchedules)
            }
        }
        preferencesManager.setActivePolicyProfile(type.name)
    }
}

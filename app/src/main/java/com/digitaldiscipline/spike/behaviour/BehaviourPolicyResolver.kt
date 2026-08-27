package com.digitaldiscipline.spike.behaviour

import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.policy.PolicyRepository
import java.util.Calendar

sealed class PolicyResolutionResult {
    data class ParentPolicyMatch(
        val appRule: AppRuleEntity
    ) : PolicyResolutionResult()

    data class BehaviourPolicyMatch(
        val goal: GoalEntity,
        val trigger: TriggerEntity,
        val replacementBehaviour: ReplacementBehaviourEntity,
        val behaviourPolicy: BehaviourPolicyEntity,
        val resolvedAppRule: AppRuleEntity
    ) : PolicyResolutionResult()

    object NoMatch : PolicyResolutionResult()
}

/**
 * Deterministic Behaviour Policy Resolver.
 * Resolves user goals, distraction triggers, replacement behaviours, and reward policies
 * into concrete enforcement rules.
 *
 * MANDATORY RULE: Parent Mode policies take absolute precedence over Self Mode policies.
 */
class BehaviourPolicyResolver(
    private val policyRepository: PolicyRepository,
    private val behaviourRepository: BehaviourRepository
) {

    suspend fun resolvePolicy(
        packageName: String,
        userMode: UserMode = UserMode.PARENT,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): PolicyResolutionResult {
        // 1. Check Existing Parent Enforcement Policy (Mandatory Precedence)
        val parentRule = policyRepository.getRuleForPackage(packageName)
        if (userMode == UserMode.PARENT) {
            return if (parentRule != null && parentRule.isEnabled) {
                PolicyResolutionResult.ParentPolicyMatch(parentRule)
            } else {
                PolicyResolutionResult.NoMatch
            }
        }

        // 2. SELF MODE: Evaluate Active Behaviour Policies (Strictly user-selected apps only)
        val activeTriggers = behaviourRepository.getActiveTriggersForPackage(packageName)
        if (activeTriggers.isEmpty()) {
            return PolicyResolutionResult.NoMatch
        }

        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentDay = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat

        for (trigger in activeTriggers) {
            // Check day of week
            val daysList = trigger.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (daysList.isNotEmpty() && !daysList.contains(currentDay)) {
                continue
            }

            // Check time window
            val currentMinutesOfDay = currentHour * 60 + currentMinute
            val startMinutesOfDay = trigger.startHour * 60 + trigger.startMinute
            val endMinutesOfDay = trigger.endHour * 60 + trigger.endMinute

            val isInWindow = if (startMinutesOfDay <= endMinutesOfDay) {
                currentMinutesOfDay in startMinutesOfDay..endMinutesOfDay
            } else {
                // Crosses midnight (e.g. 22:00 to 06:00)
                currentMinutesOfDay >= startMinutesOfDay || currentMinutesOfDay <= endMinutesOfDay
            }

            if (!isInWindow) continue

            // Find active BehaviourPolicy for this trigger
            val policies = behaviourRepository.getActivePoliciesForTrigger(trigger.triggerId)
            if (policies.isEmpty()) continue

            val policy = policies.first()
            val goal = behaviourRepository.getGoalById(policy.goalId) ?: continue
            if (!goal.active) continue

            val behaviour = behaviourRepository.getBehaviourById(policy.replacementBehaviourId) ?: continue

            // Synthesize mapped AppRuleEntity for instant PolicyEngine execution
            val ruleMode = try {
                RuleMode.valueOf(policy.interventionMode)
            } catch (e: Exception) {
                RuleMode.EARN
            }

            val syntheticRule = AppRuleEntity(
                packageName = packageName,
                appDisplayName = trigger.appDisplayName.ifBlank { packageName },
                mode = ruleMode,
                isEnabled = true,
                dailyLimitMinutes = 0,
                unlockDurationSeconds = policy.earnedSeconds,
                interventionType = behaviour.category, // PHYSICAL, MINDFUL, STUDY
                pauseDurationSeconds = if (behaviour.type == BehaviourType.MINDFUL_PAUSE.name) behaviour.durationSeconds else 10,
                breathingDurationSeconds = if (behaviour.type == BehaviourType.BOX_BREATHING.name) behaviour.durationSeconds else 30,
                squatsTargetCount = if (behaviour.type == BehaviourType.SQUATS.name) behaviour.targetCount else 10
            )

            return PolicyResolutionResult.BehaviourPolicyMatch(
                goal = goal,
                trigger = trigger,
                replacementBehaviour = behaviour,
                behaviourPolicy = policy,
                resolvedAppRule = syntheticRule
            )
        }

        // If no self-mode trigger matched, fallback to parent rule if present
        return if (parentRule != null && parentRule.isEnabled) {
            PolicyResolutionResult.ParentPolicyMatch(parentRule)
        } else {
            PolicyResolutionResult.NoMatch
        }
    }
}

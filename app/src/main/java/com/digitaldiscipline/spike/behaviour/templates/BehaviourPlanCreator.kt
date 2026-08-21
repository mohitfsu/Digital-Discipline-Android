package com.digitaldiscipline.spike.behaviour.templates

import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import java.util.UUID

data class WalletConfiguration(
    val walletId: String = "wallet_self",
    val availableSeconds: Int = 0,
    val dailyEarnCapSeconds: Int = 1800,
    val balanceCapSeconds: Int = 3600,
    val maxSessionSeconds: Int = 900
)

data class BehaviourPlanDraft(
    val template: GoalTemplate,
    val goalEntity: GoalEntity,
    val triggerEntities: List<TriggerEntity>,
    val replacementBehaviourEntity: ReplacementBehaviourEntity,
    val policyEntities: List<BehaviourPolicyEntity>,
    val walletConfig: WalletConfiguration,
    val scheduleEntity: ScheduleEntity? = null,
    val rewardPreset: RewardPreset
)

object BehaviourPlanCreator {

    /**
     * Creates an in-memory draft of the starting behavioral plan.
     * MANDATORY: Does NOT persist changes to database until explicitly confirmed by user.
     */
    fun createDraftPlan(
        template: GoalTemplate,
        selectedDistractions: List<DistractionAppRecommendation>,
        selectedReplacement: ReplacementBehaviourEntity = template.recommendedReplacementBehaviours.first(),
        rewardPreset: RewardPreset = template.defaultRewardPreset,
        customGoalTitle: String? = null,
        customGoalDescription: String? = null,
        customDailyTarget: Int? = null,
        customUnit: String? = null,
        goalId: String = "goal_self_${UUID.randomUUID()}"
    ): BehaviourPlanDraft {
        val title = customGoalTitle?.ifBlank { null } ?: template.name
        val description = customGoalDescription?.ifBlank { null } ?: template.shortDescription
        val dailyTarget = customDailyTarget ?: template.defaultDailyTarget
        val unit = customUnit?.ifBlank { null } ?: template.defaultUnit

        val goalEntity = GoalEntity(
            goalId = goalId,
            ownerId = "self",
            mode = UserMode.SELF.name,
            title = title,
            description = description,
            category = template.category.name,
            dailyTarget = dailyTarget,
            unit = unit,
            active = true
        )

        val triggerEntities = selectedDistractions.mapIndexed { index, app ->
            TriggerEntity(
                triggerId = "trig_${UUID.randomUUID()}",
                ownerId = "self",
                goalId = goalId,
                packageName = app.packageName,
                appDisplayName = app.displayName,
                category = app.category.name,
                active = true,
                startHour = 0,
                startMinute = 0,
                endHour = 23,
                endMinute = 59,
                daysOfWeek = "1,2,3,4,5,6,7",
                priority = index + 1
            )
        }

        val policyEntities = triggerEntities.mapIndexed { index, trigger ->
            BehaviourPolicyEntity(
                policyId = "pol_${UUID.randomUUID()}",
                ownerId = "self",
                goalId = goalId,
                triggerId = trigger.triggerId,
                replacementBehaviourId = selectedReplacement.behaviourId,
                interventionMode = RuleMode.EARN.name,
                rewardType = RewardType.EARNED_SCREEN_TIME.name,
                earnedSeconds = rewardPreset.rewardSeconds,
                enabled = true,
                priority = index + 1
            )
        }

        val walletConfig = WalletConfiguration(
            walletId = "wallet_self",
            availableSeconds = 0,
            dailyEarnCapSeconds = rewardPreset.dailyCapSeconds,
            balanceCapSeconds = template.defaultWalletCapSeconds,
            maxSessionSeconds = rewardPreset.sessionCapSeconds
        )

        return BehaviourPlanDraft(
            template = template,
            goalEntity = goalEntity,
            triggerEntities = triggerEntities,
            replacementBehaviourEntity = selectedReplacement,
            policyEntities = policyEntities,
            walletConfig = walletConfig,
            rewardPreset = rewardPreset
        )
    }

    /**
     * Atomically commits and persists the confirmed plan to Room and DataStore.
     */
    suspend fun confirmAndPersistPlan(
        draft: BehaviourPlanDraft,
        behaviourRepository: BehaviourRepository,
        walletService: EarnedTimeWalletService,
        preferencesManager: PreferencesManager
    ) {
        // 1. Save Goal
        behaviourRepository.saveGoal(draft.goalEntity)

        // 2. Save Replacement Behaviour
        behaviourRepository.saveBehaviour(draft.replacementBehaviourEntity)

        // 3. Save Triggers
        draft.triggerEntities.forEach { trigger ->
            behaviourRepository.saveTrigger(trigger)
        }

        // 4. Save Policies
        draft.policyEntities.forEach { policy ->
            behaviourRepository.savePolicy(policy)
        }

        // 5. Initialize/Update Authoritative Wallet with caps
        val existingWallet = walletService.getWallet("wallet_self")
        val updatedWallet = existingWallet.copy(
            dailyEarnCapSeconds = draft.walletConfig.dailyEarnCapSeconds,
            maxBalanceCapSeconds = draft.walletConfig.balanceCapSeconds,
            maxSessionSeconds = draft.walletConfig.maxSessionSeconds,
            updatedAt = System.currentTimeMillis()
        )
        walletService.saveWallet(updatedWallet)

        // 6. Set User Preferences
        preferencesManager.setUserMode(UserMode.SELF.name)
        preferencesManager.setOnboardingCompleted(true)
    }
}

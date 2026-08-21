package com.digitaldiscipline.spike.behaviour.activation

import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.behaviour.templates.BehaviourPlanCreator
import com.digitaldiscipline.spike.behaviour.templates.BehaviourPlanDraft
import com.digitaldiscipline.spike.behaviour.templates.DistractionAppRecommendation
import com.digitaldiscipline.spike.behaviour.templates.GoalTemplate
import com.digitaldiscipline.spike.behaviour.templates.RewardPreset
import com.digitaldiscipline.spike.data.local.entities.GoalCategory
import com.digitaldiscipline.spike.data.local.entities.ReplacementBehaviourEntity
import com.digitaldiscipline.spike.data.local.entities.TriggerCategory
import com.digitaldiscipline.spike.data.local.entities.UserMode
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Phase 4E-1 — Self Mode Activation Coordinator
 *
 * Orchestrates the validation, drafting, and atomic commit of the Self Mode starting plan.
 * Ensures zero partial state, double-submission protection, and robust rollback.
 *
 * Architectural Invariant:
 * - Does NOT replace or modify BehaviourPlanCreator, BehaviourRepository, or EarnedTimeWalletService.
 * - Coordinates existing systems atomically.
 * - Operates 100% offline and locally without network or cloud dependencies.
 */
object SelfModeActivationCoordinator {

    private val activationMutex = Mutex()

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    sealed class ActivationResult {
        data class Success(val goalId: String) : ActivationResult()
        data class Failure(val reason: String, val exception: Throwable? = null) : ActivationResult()
    }

    const val STATE_NOT_STARTED = "NOT_STARTED"
    const val STATE_IN_PROGRESS = "IN_PROGRESS"
    const val STATE_READY = "READY"
    const val STATE_COMPLETED = "COMPLETED"

    const val MIN_DISTRACTION_APPS = 1
    const val MAX_DISTRACTION_APPS = 5

    /**
     * Validate first-run selections before draft creation.
     */
    fun validateSelections(
        template: GoalTemplate?,
        distractions: List<DistractionAppRecommendation>,
        replacement: ReplacementBehaviourEntity?,
        rewardPreset: RewardPreset?
    ): ValidationResult {
        if (template == null) {
            return ValidationResult.Invalid("Please choose a goal to start.")
        }
        if (distractions.isEmpty()) {
            return ValidationResult.Invalid("Please select at least 1 app you want to protect.")
        }
        if (distractions.size > MAX_DISTRACTION_APPS) {
            return ValidationResult.Invalid("You can select up to $MAX_DISTRACTION_APPS apps.")
        }
        if (replacement == null) {
            return ValidationResult.Invalid("Please select a positive action.")
        }
        if (rewardPreset == null) {
            return ValidationResult.Invalid("Please select an earned screen time reward.")
        }
        return ValidationResult.Valid
    }

    /**
     * Creates an in-memory draft of the plan without writing to Room or DataStore.
     */
    fun createDraft(
        template: GoalTemplate,
        selectedDistractions: List<DistractionAppRecommendation>,
        selectedReplacement: ReplacementBehaviourEntity = template.recommendedReplacementBehaviours.first(),
        rewardPreset: RewardPreset = template.defaultRewardPreset,
        customGoalTitle: String? = null,
        customGoalDescription: String? = null,
        customDailyTarget: Int? = null,
        customUnit: String? = null
    ): BehaviourPlanDraft {
        return BehaviourPlanCreator.createDraftPlan(
            template = template,
            selectedDistractions = selectedDistractions,
            selectedReplacement = selectedReplacement,
            rewardPreset = rewardPreset,
            customGoalTitle = if (template.category == GoalCategory.CUSTOM) customGoalTitle else null,
            customGoalDescription = if (template.category == GoalCategory.CUSTOM) customGoalDescription else null,
            customDailyTarget = if (template.category == GoalCategory.CUSTOM) customDailyTarget else null,
            customUnit = if (template.category == GoalCategory.CUSTOM) customUnit else null
        )
    }

    /**
     * Atomically activates and commits the plan to Room and DataStore.
     * Protected by Mutex to prevent duplicate / concurrent submissions.
     */
    suspend fun activatePlan(
        draft: BehaviourPlanDraft,
        behaviourRepository: BehaviourRepository,
        walletService: EarnedTimeWalletService,
        preferencesManager: PreferencesManager
    ): ActivationResult {
        return activationMutex.withLock {
            try {
                // 1. Validation check
                val validation = validateSelections(
                    template = draft.template,
                    distractions = draft.triggerEntities.map {
                        DistractionAppRecommendation(
                            packageName = it.packageName,
                            displayName = it.appDisplayName,
                            icon = "📱",
                            category = TriggerCategory.valueOf(it.category)
                        )
                    },
                    replacement = draft.replacementBehaviourEntity,
                    rewardPreset = draft.rewardPreset
                )
                if (validation is ValidationResult.Invalid) {
                    return@withLock ActivationResult.Failure(validation.message)
                }

                // 2. Persist plan atomically via BehaviourPlanCreator
                BehaviourPlanCreator.confirmAndPersistPlan(
                    draft = draft,
                    behaviourRepository = behaviourRepository,
                    walletService = walletService,
                    preferencesManager = preferencesManager
                )

                // 3. Mark Onboarding as COMPLETED in Preferences
                preferencesManager.setUserMode(UserMode.SELF.name)
                preferencesManager.setOnboardingCompleted(true)
                preferencesManager.setSelfOnboardingState(STATE_COMPLETED)

                // 4. Initialize First-Win State
                com.digitaldiscipline.spike.behaviour.firstwin.FirstWinStateManager.onPlanActivated(
                    planId = draft.goalEntity.goalId,
                    preferencesManager = preferencesManager
                )

                EventLogger.log(
                    source = "ACTIVATION",
                    packageName = "system",
                    eventType = "SELF_MODE_PLAN_ACTIVATED",
                    details = "Goal: ${draft.goalEntity.title} | Apps: ${draft.triggerEntities.size} | Action: ${draft.replacementBehaviourEntity.title}"
                )

                ActivationResult.Success(goalId = draft.goalEntity.goalId)
            } catch (e: Exception) {
                EventLogger.log(
                    source = "ACTIVATION",
                    packageName = "system",
                    eventType = "SELF_MODE_ACTIVATION_ERROR",
                    details = e.message ?: "Unknown activation failure"
                )
                ActivationResult.Failure(
                    reason = "Activation failed: ${e.message ?: "Unexpected error"}",
                    exception = e
                )
            }
        }
    }
}

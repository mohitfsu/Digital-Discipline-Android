package com.digitaldiscipline.spike.intervention.engine

import android.content.Context
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.UserMode
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.intervention.adaptive.*
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.intervention.model.InterventionDifficulty
import com.digitaldiscipline.spike.intervention.model.ValidationType
import com.digitaldiscipline.spike.intervention.policy.InterventionPolicy
import com.digitaldiscipline.spike.intervention.session.InterventionSession
import com.digitaldiscipline.spike.intervention.session.PolicySource
import com.digitaldiscipline.spike.intervention.session.SessionState
import com.digitaldiscipline.spike.intervention.validation.*
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

sealed class InterventionDecision {
    data class Block(val reason: String, val targetPackage: String) : InterventionDecision()
    data class ParentDelay(val targetPackage: String, val delaySeconds: Int) : InterventionDecision()
    data class Challenge(val session: InterventionSession, val policy: InterventionPolicy) : InterventionDecision()
    object Allow : InterventionDecision()
}

/**
 * Unified Intervention Engine for Digital Discipline.
 *
 * Enforces Architectural Rule #1: Single unified engine for both Self and Parent policies.
 * Enforces Architectural Rule #4: Absolute Parent Mode Precedence (BLOCK > DELAY > SELF).
 * Enforces Architectural Rule #3: Authoritative wallet integration without direct balance manipulation.
 * Enforces Architectural Rule #5: Zero surveillance & on-device transient validation.
 * Phase 6A: Adaptive intervention loop with 5-factor scoring selector & outcome recording.
 */
class InterventionEngine(
    private val context: Context? = null,
    private val policyRepository: PolicyRepository,
    private val behaviourRepository: BehaviourRepository,
    private val walletService: EarnedTimeWalletService? = null,
    val adaptiveStore: InterventionAdaptiveStore = InterventionAdaptiveStore(),
    val selector: InterventionSelector = InterventionSelector(adaptiveStore),
    val preferencesManager: PreferencesManager? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _activeSessionFlow = MutableStateFlow<InterventionSession?>(null)
    val activeSessionFlow: StateFlow<InterventionSession?> = _activeSessionFlow.asStateFlow()

    private var activeValidator: InterventionValidator? = null
    private var enabledInterventionIds: Set<String>? = null

    init {
        if (preferencesManager != null) {
            coroutineScope.launch {
                preferencesManager.enabledInterventionsFlow.collect { ids: Set<String> ->
                    enabledInterventionIds = if (ids.isNotEmpty()) ids else null
                }
            }
        }
    }

    /**
     * Resolves policy and creates an intervention decision for the target package.
     */
    suspend fun evaluateTargetPackage(
        packageName: String,
        userMode: UserMode = UserMode.SELF,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): InterventionDecision {
        // 1. Check Parent Mode Rules (Absolute Precedence)
        val parentRule = policyRepository.getRuleForPackage(packageName)
        if (parentRule != null && parentRule.isEnabled) {
            when (parentRule.mode) {
                RuleMode.BLOCK -> {
                    return InterventionDecision.Block(
                        reason = "Parent Mode Hard Block",
                        targetPackage = packageName
                    )
                }
                RuleMode.DELAY -> {
                    if (userMode == UserMode.PARENT) {
                        return InterventionDecision.ParentDelay(
                            targetPackage = packageName,
                            delaySeconds = parentRule.pauseDurationSeconds.coerceAtLeast(10)
                        )
                    }
                }
                RuleMode.EARN -> {
                    if (userMode == UserMode.PARENT) {
                        val intervention = mapParentInterventionToDefinition(parentRule.interventionType)
                        val policy = InterventionPolicy(
                            policyId = "parent_pol_${parentRule.packageName}",
                            policySource = PolicySource.PARENT,
                            targetPackage = packageName,
                            intervention = intervention,
                            difficulty = InterventionDifficulty.STANDARD,
                            rewardSeconds = parentRule.unlockDurationSeconds
                        )
                        val session = createSession(intervention, packageName, PolicySource.PARENT)
                        return InterventionDecision.Challenge(session, policy)
                    }
                }
                RuleMode.ALLOW -> {
                    if (userMode == UserMode.PARENT) {
                        return InterventionDecision.Allow
                    }
                }
            }
        }

        if (userMode == UserMode.PARENT) {
            return if (parentRule != null && parentRule.isEnabled) {
                InterventionDecision.Block("Parent Policy Active", packageName)
            } else {
                InterventionDecision.Allow
            }
        }

        // 2. SELF MODE Evaluation
        val activeTriggers = behaviourRepository.getActiveTriggersForPackage(packageName)
        if (activeTriggers.isEmpty()) {
            return InterventionDecision.Allow
        }

        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)

        for (trigger in activeTriggers) {
            val daysList = trigger.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (daysList.isNotEmpty() && !daysList.contains(currentDay)) {
                continue
            }

            val currentMinutesOfDay = currentHour * 60 + currentMinute
            val startMinutesOfDay = trigger.startHour * 60 + trigger.startMinute
            val endMinutesOfDay = trigger.endHour * 60 + trigger.endMinute

            val isInWindow = if (startMinutesOfDay <= endMinutesOfDay) {
                currentMinutesOfDay in startMinutesOfDay..endMinutesOfDay
            } else {
                currentMinutesOfDay >= startMinutesOfDay || currentMinutesOfDay <= endMinutesOfDay
            }

            if (!isInWindow) continue

            val policies = behaviourRepository.getActivePoliciesForTrigger(trigger.triggerId)
            if (policies.isEmpty()) continue

            val policy = policies.first()
            val goal = behaviourRepository.getGoalById(policy.goalId) ?: continue
            if (!goal.active) continue

            val configuredIntervention = mapSelfBehaviourToDefinition(policy.replacementBehaviourId)

            // Phase 6A: Adaptive Selection
            val triggerContext = InterventionContext(
                triggerId = trigger.triggerId,
                targetPackage = packageName,
                timestampMs = currentTimeMillis,
                dayOfWeek = currentDay,
                policySource = PolicySource.SELF,
                configuredInterventionId = configuredIntervention.id,
                recentInterventionIds = adaptiveStore.getRecentInterventionIds(5),
                walletBalanceSeconds = 0
            )

            val selection = selector.select(triggerContext, enabledInterventionIds)
            val finalIntervention = selection.selectedIntervention

            val interventionPolicy = InterventionPolicy(
                policyId = policy.policyId,
                policySource = PolicySource.SELF,
                targetPackage = packageName,
                intervention = finalIntervention,
                difficulty = InterventionDifficulty.STANDARD,
                rewardSeconds = policy.earnedSeconds.coerceAtLeast(60)
            )

            val session = createSession(finalIntervention, packageName, PolicySource.SELF)
            return InterventionDecision.Challenge(session, interventionPolicy)
        }

        return InterventionDecision.Allow
    }

    fun createSession(
        intervention: InterventionDefinition,
        targetPackage: String,
        policySource: PolicySource = PolicySource.SELF,
        difficulty: InterventionDifficulty = InterventionDifficulty.STANDARD
    ): InterventionSession {
        // Cancel any previous session
        cancelCurrentSession("Starting new intervention")

        val session = InterventionSession(
            intervention = intervention,
            targetPackage = targetPackage,
            policySource = policySource,
            difficulty = difficulty,
            rewardSeconds = intervention.rewardSeconds
        )
        session.prepare()
        _activeSessionFlow.value = session
        return session
    }

    fun startSession(session: InterventionSession, onResult: (ValidationResult) -> Unit) {
        val validator = createValidatorForType(session.intervention.validationType)
        startSessionWithValidator(session, validator, onResult)
    }

    fun startSessionWithValidator(
        session: InterventionSession,
        validator: InterventionValidator,
        onResult: (ValidationResult) -> Unit
    ) {
        session.start()
        _activeSessionFlow.value = session
        activeValidator = validator

        validator.startValidation(session) { result ->
            when (result) {
                is ValidationResult.Progress -> {
                    _activeSessionFlow.value = session
                    onResult(result)
                }
                is ValidationResult.Completed -> {
                    onSessionCompleted(session)
                    onResult(result)
                }
                is ValidationResult.Failed -> {
                    session.fail(result.reason)
                    _activeSessionFlow.value = session
                    recordOutcome(session, SessionState.FAILED, 0)
                    cleanupValidator()
                    onResult(result)
                }
            }
        }
    }

    private fun onSessionCompleted(session: InterventionSession) {
        _activeSessionFlow.value = session
        cleanupValidator()

        // Credit authoritative wallet
        val durationSec = session.rewardSeconds
        val idempotencyKey = "earn_intervention_${session.sessionId}"

        // Record adaptive outcome
        recordOutcome(session, SessionState.COMPLETED, durationSec)

        coroutineScope.launch(Dispatchers.IO) {
            walletService?.earnTime(
                amountSeconds = durationSec,
                source = "INTERVENTION_${session.intervention.id}",
                triggerPackage = session.targetPackage,
                idempotencyKey = idempotencyKey
            )
        }
    }

    fun cancelCurrentSession(reason: String = "User dismissed") {
        val current = _activeSessionFlow.value
        if (current != null && !current.isTerminal) {
            current.cancel(reason)
            _activeSessionFlow.value = current
            recordOutcome(current, SessionState.CANCELLED, 0)
        }
        cleanupValidator()
    }

    private fun recordOutcome(session: InterventionSession, status: SessionState, rewardSec: Int) {
        val outcome = InterventionOutcome(
            sessionId = session.sessionId,
            interventionId = session.intervention.id,
            targetPackage = session.targetPackage,
            policySource = session.policySource,
            startedAtRealtimeMs = session.startedAtRealtimeMs,
            completedAtRealtimeMs = session.completedAtRealtimeMs,
            status = status,
            rewardSeconds = rewardSec
        )
        adaptiveStore.recordOutcome(outcome)
    }

    fun recordFeedback(sessionId: String, feedback: HelpfulnessFeedback) {
        adaptiveStore.recordFeedback(sessionId, feedback)
    }

    private fun cleanupValidator() {
        activeValidator?.stopValidation()
        activeValidator = null
    }

    private fun createValidatorForType(validationType: ValidationType): InterventionValidator {
        return when (validationType) {
            ValidationType.CAMERA_VALIDATED -> CameraPoseValidator("PUSH_UPS")
            ValidationType.SENSOR_VALIDATED -> if (context != null) MovementSensorValidator(context) else TimerValidator()
            ValidationType.TIMER_VALIDATED -> TimerValidator()
            ValidationType.INTERACTION_VALIDATED -> CognitiveInteractionValidator()
            ValidationType.MANUAL_CONFIRMATION -> ManualConfirmationValidator()
        }
    }

    private fun mapSelfBehaviourToDefinition(behaviourId: String): InterventionDefinition {
        return when (behaviourId.lowercase()) {
            "beh_squats_10", "squats" -> InterventionCatalog.getIntervention("SQUATS")
            "beh_pushups_10", "pushups" -> InterventionCatalog.getIntervention("PUSH_UPS")
            "beh_pause_10s", "pause" -> InterventionCatalog.getIntervention("MINDFUL_PAUSE")
            "beh_breathing_30s", "breathing" -> InterventionCatalog.getIntervention("BOX_BREATHING")
            "beh_meditation_1m", "meditation" -> InterventionCatalog.getIntervention("ONE_MINUTE_MEDITATION")
            "beh_math_quick", "math" -> InterventionCatalog.getIntervention("SIMPLE_MATH")
            else -> InterventionCatalog.getIntervention(behaviourId.uppercase())
        } ?: InterventionCatalog.getDefaultIntervention()
    }

    private fun mapParentInterventionToDefinition(message: String?): InterventionDefinition {
        return InterventionCatalog.getIntervention("MINDFUL_PAUSE") ?: InterventionCatalog.getDefaultIntervention()
    }
}

package com.digitaldiscipline.spike.behaviour.adaptive

import com.digitaldiscipline.spike.behaviour.BehaviourInsightsEngine
import com.digitaldiscipline.spike.data.local.entities.*
import java.util.UUID

enum class PlanHealth(val displayName: String, val badge: String) {
    WORKING("Working well", "🟢"),
    NEEDS_ADJUSTMENT("Needs adjustment", "🟡"),
    NOT_WORKING("Not working", "🔴"),
    INSUFFICIENT_DATA("Building baseline", "⚪")
}

enum class RecommendationType {
    KEEP_PLAN,
    CHANGE_INTERVENTION,
    REDUCE_REWARD,
    INCREASE_REWARD,
    SHORTER_INTERVENTION,
    LONGER_INTERVENTION,
    ADD_COOLDOWN,
    REMOVE_COOLDOWN,
    ADJUST_TARGET,
    CHANGE_DISTRACTION_WINDOW,
    INSUFFICIENT_DATA
}

enum class ConfidenceLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class BehaviourRecommendation(
    val recommendationId: String = "rec_${UUID.randomUUID()}",
    val type: RecommendationType,
    val title: String,
    val explanation: String,
    val currentConfiguration: String,
    val suggestedConfiguration: String,
    val confidenceLevel: ConfidenceLevel,
    val evidence: String,
    val cooldownSeconds: Int = 0,
    val suggestedRewardSeconds: Int? = null,
    val suggestedInterventionType: String? = null,
    val suggestedInterventionCount: Int? = null,
    val generatedAt: Long = System.currentTimeMillis()
)

data class InterventionPerformance(
    val type: String,
    val attempts: Int,
    val completions: Int,
    val exits: Int,
    val rapidReopens5m: Int,
    val habitInterruptionRate: Float,
    val completionRate: Float
)

data class RewardLoopAnalysis(
    val hasSufficientData: Boolean,
    val isRewardLoopDetected: Boolean,
    val immediateReopenRatio: Float,
    val description: String
)

object AdaptivePlanEngine {

    const val MIN_SAMPLE_TRIALS = 10
    const val MIN_TREND_DAYS = 3

    /**
     * Evaluates current plan health deterministically.
     */
    fun evaluatePlanHealth(
        events: List<InterventionEventEntity>,
        transactions: List<WalletTransactionEntity> = emptyList(),
        sessions: List<WalletSessionEntity> = emptyList()
    ): PlanHealth {
        if (events.size < MIN_SAMPLE_TRIALS) {
            return PlanHealth.INSUFFICIENT_DATA
        }

        val hir = BehaviourInsightsEngine.calculateHabitInterruptionRate(events)
        val completedCount = events.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
        val completionRate = (completedCount.toFloat() / events.size.toFloat()) * 100f

        return when {
            hir >= 65.0f && completionRate >= 60.0f -> PlanHealth.WORKING
            hir in 40.0f..64.99f || completionRate in 35.0f..59.99f -> PlanHealth.NEEDS_ADJUSTMENT
            else -> PlanHealth.NOT_WORKING
        }
    }

    /**
     * Calculates effectiveness metrics for each intervention type.
     */
    fun evaluateInterventionEffectiveness(
        events: List<InterventionEventEntity>
    ): List<InterventionPerformance> {
        val grouped = events.filter { it.interventionType != "PARENT_OVERRIDE" }
            .groupBy { it.interventionType.uppercase() }

        return grouped.map { (type, list) ->
            val attempts = list.size
            val completions = list.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
            val exits = list.count { it.status == "EXITED" }
            val reopens = list.count { it.reopenWithin5Minutes }
            val hir = if (attempts > 0) ((attempts - reopens).toFloat() / attempts.toFloat()) * 100f else 100f
            val compRate = if (attempts > 0) (completions.toFloat() / attempts.toFloat()) * 100f else 0f

            InterventionPerformance(
                type = type,
                attempts = attempts,
                completions = completions,
                exits = exits,
                rapidReopens5m = reopens,
                habitInterruptionRate = hir,
                completionRate = compRate
            )
        }.sortedByDescending { it.habitInterruptionRate }
    }

    /**
     * Detects potential reward-loop/binge patterns without clinical diagnosis.
     */
    fun evaluateRewardEffectiveness(
        events: List<InterventionEventEntity>,
        transactions: List<WalletTransactionEntity>,
        sessions: List<WalletSessionEntity>
    ): RewardLoopAnalysis {
        if (sessions.size < MIN_SAMPLE_TRIALS) {
            return RewardLoopAnalysis(
                hasSufficientData = false,
                isRewardLoopDetected = false,
                immediateReopenRatio = 0f,
                description = "Baseline reward data is still accumulating."
            )
        }

        // Check sessions followed immediately (within 60s) by another intervention attempt
        var immediateReopens = 0
        sessions.forEach { sess ->
            val sessionEnd = sess.startedWallClock + (sess.consumedSeconds * 1000L)
            val hasReopen = events.any { ev ->
                ev.timestamp in sessionEnd..(sessionEnd + 60_000L)
            }
            if (hasReopen) immediateReopens++
        }

        val ratio = immediateReopens.toFloat() / sessions.size.toFloat()
        val loopDetected = ratio >= 0.35f

        return RewardLoopAnalysis(
            hasSufficientData = true,
            isRewardLoopDetected = loopDetected,
            immediateReopenRatio = ratio,
            description = if (loopDetected) {
                "Your current reward pattern may be encouraging repeated back-to-back sessions."
            } else {
                "Your earned sessions are spaced reasonably."
            }
        )
    }

    /**
     * Determines whether adding a cooldown would prevent rapid successive sessions.
     */
    fun evaluateCooldownNeed(
        sessions: List<WalletSessionEntity>,
        events: List<InterventionEventEntity>
    ): Boolean {
        if (sessions.size < MIN_SAMPLE_TRIALS) return false
        var rapidChainCount = 0
        sessions.forEach { sess ->
            val sessionEnd = sess.startedWallClock + (sess.consumedSeconds * 1000L)
            if (events.any { it.timestamp in sessionEnd..(sessionEnd + 60_000L) }) {
                rapidChainCount++
            }
        }
        return (rapidChainCount.toFloat() / sessions.size.toFloat()) >= 0.30f
    }

    /**
     * Generates a primary deterministic plan recommendation.
     */
    fun generatePrimaryRecommendation(
        currentGoal: GoalEntity?,
        currentPolicies: List<BehaviourPolicyEntity>,
        currentBehaviours: List<ReplacementBehaviourEntity>,
        events: List<InterventionEventEntity>,
        transactions: List<WalletTransactionEntity>,
        sessions: List<WalletSessionEntity>,
        previousAdjustments: List<PlanAdjustmentEntity> = emptyList()
    ): BehaviourRecommendation {
        if (events.size < MIN_SAMPLE_TRIALS) {
            return BehaviourRecommendation(
                type = RecommendationType.INSUFFICIENT_DATA,
                title = "Gathering Baseline",
                explanation = "Continue with your current plan. We'll generate personalized recommendations once you reach 10 sessions.",
                currentConfiguration = "Current Plan",
                suggestedConfiguration = "Current Plan",
                confidenceLevel = ConfidenceLevel.LOW,
                evidence = "${events.size}/$MIN_SAMPLE_TRIALS trials observed."
            )
        }

        val activePolicy = currentPolicies.firstOrNull { it.enabled }
        val activeBehaviour = currentBehaviours.firstOrNull { it.behaviourId == activePolicy?.replacementBehaviourId }
        val performances = evaluateInterventionEffectiveness(events)
        val rewardAnalysis = evaluateRewardEffectiveness(events, transactions, sessions)

        // 1. High exit rate on physical challenges (>40% exit rate) -> SHORTER_INTERVENTION
        if (activeBehaviour != null && (activeBehaviour.type == BehaviourType.SQUATS.name || activeBehaviour.type == BehaviourType.PUSHUPS.name)) {
            val currentPerf = performances.firstOrNull { it.type.equals(activeBehaviour.type, ignoreCase = true) }
            if (currentPerf != null && currentPerf.attempts >= MIN_SAMPLE_TRIALS) {
                val exitRatio = currentPerf.exits.toFloat() / currentPerf.attempts.toFloat()
                if (exitRatio >= 0.40f && activeBehaviour.targetCount > 5) {
                    val newCount = 5
                    return BehaviourRecommendation(
                        type = RecommendationType.SHORTER_INTERVENTION,
                        title = "Reduce Challenge Size",
                        explanation = "You often exit before finishing ${activeBehaviour.targetCount} ${activeBehaviour.unit}. A shorter challenge makes it easier to stay consistent.",
                        currentConfiguration = "${activeBehaviour.targetCount} ${activeBehaviour.title}",
                        suggestedConfiguration = "$newCount ${activeBehaviour.title.replace(activeBehaviour.targetCount.toString(), newCount.toString())}",
                        confidenceLevel = ConfidenceLevel.HIGH,
                        evidence = "Exit rate is ${(exitRatio * 100).toInt()}% across ${currentPerf.attempts} trials.",
                        suggestedInterventionType = activeBehaviour.type,
                        suggestedInterventionCount = newCount
                    )
                }
            }
        }

        // 2. Significant intervention superiority (alternative is >=20% better HIR) -> CHANGE_INTERVENTION
        if (performances.size >= 2) {
            val validPerfs = performances.filter { it.attempts >= MIN_SAMPLE_TRIALS }
            if (validPerfs.size >= 2) {
                val best = validPerfs.first()
                val currentType = activeBehaviour?.type ?: ""
                val currentPerf = validPerfs.firstOrNull { it.type.equals(currentType, ignoreCase = true) }
                if (currentPerf != null && best.type != currentPerf.type && (best.habitInterruptionRate - currentPerf.habitInterruptionRate) >= 20.0f) {
                    return BehaviourRecommendation(
                        type = RecommendationType.CHANGE_INTERVENTION,
                        title = "Switch to ${best.type.lowercase().replaceFirstChar { it.uppercase() }}",
                        explanation = "${best.type.lowercase().replaceFirstChar { it.uppercase() }} has an interruption rate of ${best.habitInterruptionRate.toInt()}%, which is noticeably higher than your current habit.",
                        currentConfiguration = activeBehaviour?.title ?: currentType,
                        suggestedConfiguration = best.type,
                        confidenceLevel = ConfidenceLevel.HIGH,
                        evidence = "${best.type} (${best.habitInterruptionRate.toInt()}% HIR) vs $currentType (${currentPerf.habitInterruptionRate.toInt()}% HIR).",
                        suggestedInterventionType = best.type
                    )
                }
            }
        }

        // 3. Repeated rapid back-to-back sessions -> ADD_COOLDOWN
        if (evaluateCooldownNeed(sessions, events)) {
            val rejectedRecently = previousAdjustments.any {
                it.recommendationType == RecommendationType.ADD_COOLDOWN.name &&
                it.status == AdjustmentStatus.REJECTED.name &&
                (System.currentTimeMillis() - it.rejectedAt) < 7 * 86400000L
            }
            if (!rejectedRecently) {
                return BehaviourRecommendation(
                    type = RecommendationType.ADD_COOLDOWN,
                    title = "Add a 2-Minute Cooldown",
                    explanation = "Several sessions are followed immediately by another distraction attempt. A 2-minute buffer helps break the cycle.",
                    currentConfiguration = "No cooldown between sessions",
                    suggestedConfiguration = "2-minute cooldown (120s)",
                    confidenceLevel = ConfidenceLevel.MEDIUM,
                    evidence = "${(rewardAnalysis.immediateReopenRatio * 100).toInt()}% of sessions are followed by an immediate reopen.",
                    cooldownSeconds = 120
                )
            }
        }

        // 4. Excessive reward duration leading to high reopen -> REDUCE_REWARD
        if (rewardAnalysis.isRewardLoopDetected && activePolicy != null && activePolicy.earnedSeconds >= 600) {
            val suggestedSec = 300 // 5 minutes
            return BehaviourRecommendation(
                type = RecommendationType.REDUCE_REWARD,
                title = "Try 5 Minutes Earned Time",
                explanation = "Your current ${activePolicy.earnedSeconds / 60}-minute reward may be longer than needed, leading to repeated sessions.",
                currentConfiguration = "${activePolicy.earnedSeconds / 60} minutes earned time",
                suggestedConfiguration = "${suggestedSec / 60} minutes earned time",
                confidenceLevel = ConfidenceLevel.MEDIUM,
                evidence = "Immediate reopen rate after sessions is ${(rewardAnalysis.immediateReopenRatio * 100).toInt()}%.",
                suggestedRewardSeconds = suggestedSec
            )
        }

        // 5. Default -> KEEP_PLAN
        val hir = BehaviourInsightsEngine.calculateHabitInterruptionRate(events)
        return BehaviourRecommendation(
            type = RecommendationType.KEEP_PLAN,
            title = "Keep Your Current Plan",
            explanation = "Your current plan is functioning well with a ${hir.toInt()}% Habit Interruption Rate. No adjustments needed.",
            currentConfiguration = "Current Plan",
            suggestedConfiguration = "Current Plan",
            confidenceLevel = ConfidenceLevel.HIGH,
            evidence = "Habit Interruption Rate is ${hir.toInt()}% across ${events.size} trials."
        )
    }

    /**
     * Calculates the deterministic Personalization Profile snapshot.
     */
    fun calculatePersonalizationProfile(
        events: List<InterventionEventEntity>,
        transactions: List<WalletTransactionEntity>,
        sessions: List<WalletSessionEntity>,
        progressList: List<GoalProgressEntity> = emptyList()
    ): PersonalizationProfileEntity {
        val health = evaluatePlanHealth(events, transactions, sessions)
        val bestIntervention = BehaviourInsightsEngine.calculateBestIntervention(events)?.interventionType ?: "SQUATS"
        val pattern = BehaviourInsightsEngine.calculateDistractionPattern(events)

        val totalAttempts = events.size
        val completedAttempts = events.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
        val compRate = if (totalAttempts > 0) (completedAttempts.toFloat() / totalAttempts.toFloat()) * 100f else 0f

        val reopens = events.count { it.reopenWithin5Minutes }
        val reopenRate = if (totalAttempts > 0) (reopens.toFloat() / totalAttempts.toFloat()) * 100f else 0f

        val avgSessionSec = if (sessions.isNotEmpty()) {
            sessions.map { it.consumedSeconds }.average().toInt()
        } else {
            0
        }

        val rewardAnalysis = evaluateRewardEffectiveness(events, transactions, sessions)
        val consistency = if (progressList.isNotEmpty()) {
            (progressList.take(7).count { it.completedCount > 0 }.toFloat() / 7f) * 100f
        } else {
            0f
        }

        return PersonalizationProfileEntity(
            profileId = "profile_self",
            preferredIntervention = bestIntervention,
            peakStartHour = pattern.peakHourStart ?: 20,
            peakEndHour = pattern.peakHourEnd ?: 22,
            challengeCompletionRate = compRate,
            rapidReopenRate = reopenRate,
            averageSessionDurationSeconds = avgSessionSec,
            rewardEffectiveness = if (rewardAnalysis.isRewardLoopDetected) "NEEDS_COOLDOWN" else "BALANCED",
            consistencyScore = consistency,
            currentPlanHealth = health.name,
            lastCalculatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Generates a reproducible, deterministic Weekly Review summary.
     */
    fun generateWeeklyReview(
        goal: GoalEntity?,
        events: List<InterventionEventEntity>,
        transactions: List<WalletTransactionEntity>,
        sessions: List<WalletSessionEntity>,
        weekStart: Long,
        weekEnd: Long
    ): WeeklyReviewEntity {
        val attempts = events.size
        val completed = events.count { it.status == "COMPLETED" || it.outcome == "EARNED_ACCESS" }
        val earnedSec = transactions.filter { it.type == WalletTransactionType.EARN.name }.sumOf { it.amountSeconds }
        val consumedSec = sessions.sumOf { it.consumedSeconds }
        val hir = BehaviourInsightsEngine.calculateHabitInterruptionRate(events)
        val reopens = events.count { it.reopenWithin5Minutes }
        val reopenRate = if (attempts > 0) (reopens.toFloat() / attempts.toFloat()) * 100f else 0f
        val best = BehaviourInsightsEngine.calculateBestIntervention(events)?.displayName ?: "Squats"
        val health = evaluatePlanHealth(events, transactions, sessions)

        val topApp = events.firstOrNull()?.appDisplayName ?: "distraction"
        val biggestWin = if (attempts >= 10 && hir >= 60f) {
            "$topApp reopen attempts were successfully interrupted ${hir.toInt()}% of the time."
        } else if (completed >= 5) {
            "Completed $completed positive replacement actions this week."
        } else {
            "Started building your baseline discipline routine."
        }

        val suggestedNextStep = when (health) {
            PlanHealth.WORKING -> "Keep your current habit and protect your evening focus hours."
            PlanHealth.NEEDS_ADJUSTMENT -> "Consider a shorter challenge or adding a 2-minute cooldown between sessions."
            PlanHealth.NOT_WORKING -> "Switch to a lighter physical or breathing challenge to restart momentum."
            PlanHealth.INSUFFICIENT_DATA -> "Complete more daily sessions to reveal your personal distraction patterns."
        }

        return WeeklyReviewEntity(
            reviewId = "rev_${UUID.randomUUID()}",
            goalId = goal?.goalId ?: "self_goal",
            weekStart = weekStart,
            weekEnd = weekEnd,
            attempts = attempts,
            completed = completed,
            earnedSeconds = earnedSec,
            consumedSeconds = consumedSec,
            habitInterruptionRate = hir,
            rapidReopenRate = reopenRate,
            bestIntervention = best,
            planHealth = health.name,
            biggestWin = biggestWin,
            suggestedNextStep = suggestedNextStep,
            generatedAt = System.currentTimeMillis()
        )
    }
}

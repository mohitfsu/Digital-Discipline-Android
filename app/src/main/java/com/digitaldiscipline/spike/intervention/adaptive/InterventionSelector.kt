package com.digitaldiscipline.spike.intervention.adaptive

import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.intervention.session.PolicySource

data class InterventionSelection(
    val selectedIntervention: InterventionDefinition,
    val scoreBreakdown: ScoreBreakdown,
    val rank: Int = 1,
    val alternativeCandidatesCount: Int = 0
)

class InterventionSelector(
    private val adaptiveStore: InterventionAdaptiveStore
) {

    fun select(
        context: InterventionContext,
        enabledInterventionIds: Set<String>? = null
    ): InterventionSelection {
        val allInterventions = InterventionCatalog.getAllInterventions()

        // 1. Filter candidates
        val candidates = allInterventions.filter { def ->
            if (enabledInterventionIds != null && enabledInterventionIds.isNotEmpty()) {
                if (!enabledInterventionIds.contains(def.id)) return@filter false
            }
            isEligible(def, context)
        }

        val effectiveCandidates = if (candidates.isNotEmpty()) {
            candidates
        } else {
            allInterventions.filter { isEligible(it, context) }.ifEmpty {
                listOf(InterventionCatalog.getDefaultIntervention())
            }
        }

        // 2. Score all candidates
        val scoredList = effectiveCandidates.map { candidate ->
            val scoreBreakdown = calculateScore(candidate, context)
            Pair(candidate, scoreBreakdown)
        }.sortedByDescending { it.second.totalScore }

        val best = scoredList.first()
        return InterventionSelection(
            selectedIntervention = best.first,
            scoreBreakdown = best.second,
            rank = 1,
            alternativeCandidatesCount = scoredList.size - 1
        )
    }

    private fun isEligible(def: InterventionDefinition, context: InterventionContext): Boolean {
        // Night suitability check: avoid violent noisy cardio late at night (22:00 - 05:00)
        if (context.timeBucket == TimeBucket.NIGHT) {
            if (def.id == "JUMPING_JACKS" || def.id == "HIGH_KNEES") {
                return false
            }
        }
        return true
    }

    private fun calculateScore(
        candidate: InterventionDefinition,
        context: InterventionContext
    ): ScoreBreakdown {
        val globalStats = adaptiveStore.getStats(candidate.id)
        val catStats = adaptiveStore.getCategoryStats(candidate.category)
        val trigStats = if (context.targetPackage.isNotBlank()) {
            adaptiveStore.getStatsForTrigger(candidate.id, context.targetPackage)
        } else {
            null
        }
        val ctxStats = if (context.targetPackage.isNotBlank()) {
            adaptiveStore.getStatsForContext(candidate.id, context.targetPackage, context.timeBucket)
        } else {
            null
        }

        // --- Hierarchical Confidence-Weighted Helpfulness ---
        val coldStartBaseline = InterventionScoringParameters.DEFAULT_COLD_START_SCORE // 0.50f

        // Category Baseline
        val catConfidence = catStats.confidence
        val catRate = if (catStats.totalFeedbackCount > 0) catStats.helpfulnessRate else coldStartBaseline
        val catBaseline = (catConfidence * catRate) + ((1.0f - catConfidence) * coldStartBaseline)

        // Level 1: Global User / Item Level
        val itemConfidence = globalStats.confidence
        val itemRate = if (globalStats.totalFeedbackCount > 0) globalStats.helpfulnessRate else catBaseline
        val userLevelEstimate = (itemConfidence * itemRate) + ((1.0f - itemConfidence) * catBaseline)

        // Level 2: Trigger Level
        val trigConfidence = trigStats?.confidence ?: 0.0f
        val trigRate = if (trigStats != null && trigStats.totalFeedbackCount > 0) trigStats.helpfulnessRate else userLevelEstimate
        val trigLevelEstimate = (trigConfidence * trigRate) + ((1.0f - trigConfidence) * userLevelEstimate)

        // Level 3: Context Level (Trigger + TimeBucket)
        val ctxConfidence = ctxStats?.confidence ?: 0.0f
        val ctxRate = if (ctxStats != null && ctxStats.totalFeedbackCount > 0) ctxStats.helpfulnessRate else trigLevelEstimate
        val finalHelpfulnessEstimate = (ctxConfidence * ctxRate) + ((1.0f - ctxConfidence) * trigLevelEstimate)

        val helpfulnessScore = finalHelpfulnessEstimate

        // 2. Completion Rate (0.0 .. 1.0)
        val completionScore = if (globalStats.startedCount > 0) {
            globalStats.completionRate
        } else {
            InterventionScoringParameters.DEFAULT_COLD_START_SCORE
        }

        // 3. User Preference (1.0 if configured, 0.5 otherwise)
        val userPrefScore = if (candidate.id == context.configuredInterventionId) {
            1.0f
        } else {
            0.5f
        }

        // 4. Contextual Suitability (0.0 .. 1.0)
        val contextScore = calculateContextualSuitability(candidate, context)

        // 5. Novelty / Freshness (0.0 .. 1.0)
        val recentIds = context.recentInterventionIds
        val noveltyScore = when {
            recentIds.isEmpty() -> 1.0f
            !recentIds.contains(candidate.id) -> 1.0f
            recentIds.firstOrNull() == candidate.id -> 0.1f // Used immediately prior
            else -> 0.5f
        }

        // Repetition Penalties
        var penalty = 0.0f
        if (recentIds.isNotEmpty() && recentIds.first() == candidate.id) {
            penalty += InterventionScoringParameters.IMMEDIATE_REPEAT_PENALTY
        }

        val lastIntervention = recentIds.firstOrNull()?.let { InterventionCatalog.getIntervention(it) }
        if (lastIntervention != null && lastIntervention.category == candidate.category && candidate.id != context.configuredInterventionId) {
            penalty += InterventionScoringParameters.SAME_CATEGORY_PENALTY
        }

        val weightedSum = (helpfulnessScore * InterventionScoringParameters.WEIGHT_HISTORICAL_HELPFULNESS) +
                (completionScore * InterventionScoringParameters.WEIGHT_COMPLETION_RATE) +
                (userPrefScore * InterventionScoringParameters.WEIGHT_USER_PREFERENCE) +
                (contextScore * InterventionScoringParameters.WEIGHT_CONTEXTUAL_SUITABILITY) +
                (noveltyScore * InterventionScoringParameters.WEIGHT_NOVELTY)

        val total = (weightedSum - penalty).coerceIn(0.0f, 1.0f)

        val explanation = "Helpful: ${"%.2f".format(helpfulnessScore)} (conf: user=${"%.2f".format(itemConfidence)}, trig=${"%.2f".format(trigConfidence)}, ctx=${"%.2f".format(ctxConfidence)}, cat=${"%.2f".format(catConfidence)}), " +
                "Completion: ${"%.2f".format(completionScore)}, " +
                "Pref: ${"%.2f".format(userPrefScore)}, " +
                "Context: ${"%.2f".format(contextScore)}, " +
                "Novelty: ${"%.2f".format(noveltyScore)}, " +
                "Penalty: -${"%.2f".format(penalty)} -> Total: ${"%.2f".format(total)}"

        return ScoreBreakdown(
            historicalHelpfulnessScore = helpfulnessScore,
            completionRateScore = completionScore,
            userPreferenceScore = userPrefScore,
            contextualSuitabilityScore = contextScore,
            noveltyScore = noveltyScore,
            repetitionPenalty = penalty,
            totalScore = total,
            explanation = explanation,
            userLevelConfidence = itemConfidence,
            triggerLevelConfidence = trigConfidence,
            contextLevelConfidence = ctxConfidence,
            categoryLevelConfidence = catConfidence,
            hierarchicalHelpfulnessEstimate = finalHelpfulnessEstimate
        )
    }

    private fun calculateContextualSuitability(
        candidate: InterventionDefinition,
        context: InterventionContext
    ): Float {
        return when (context.timeBucket) {
            TimeBucket.MORNING -> {
                // Morning favors movement & physical energization
                if (candidate.category == InterventionCategory.MOVEMENT || candidate.category == InterventionCategory.PHYSICAL_RESET) 0.9f else 0.6f
            }
            TimeBucket.AFTERNOON -> {
                // Afternoon favors quick posture reset, eye relief, cognitive micro-challenges
                if (candidate.category == InterventionCategory.PHYSICAL_RESET || candidate.category == InterventionCategory.COGNITIVE) 0.9f else 0.7f
            }
            TimeBucket.EVENING -> {
                // Evening favors breathing, mindful pause, yoga/mobility
                if (candidate.category == InterventionCategory.BREATHING || candidate.category == InterventionCategory.YOGA_MOBILITY) 0.9f else 0.6f
            }
            TimeBucket.NIGHT -> {
                // Night favors meditation & calming breathing
                if (candidate.category == InterventionCategory.BREATHING || candidate.category == InterventionCategory.MEDITATION) 1.0f else 0.3f
            }
        }
    }
}

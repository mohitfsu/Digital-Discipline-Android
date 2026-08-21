package com.digitaldiscipline.spike.intervention.adaptive

data class ScoreBreakdown(
    val historicalHelpfulnessScore: Float, // 0.0 .. 1.0 (weighted 40%)
    val completionRateScore: Float,        // 0.0 .. 1.0 (weighted 25%)
    val userPreferenceScore: Float,        // 0.0 .. 1.0 (weighted 15%)
    val contextualSuitabilityScore: Float, // 0.0 .. 1.0 (weighted 10%)
    val noveltyScore: Float,               // 0.0 .. 1.0 (weighted 10%)
    val repetitionPenalty: Float,          // 0.0 .. 0.7 (subtracted)
    val totalScore: Float,
    val explanation: String,
    val userLevelConfidence: Float = 0.0f,
    val triggerLevelConfidence: Float = 0.0f,
    val contextLevelConfidence: Float = 0.0f,
    val categoryLevelConfidence: Float = 0.0f,
    val hierarchicalHelpfulnessEstimate: Float = 0.5f
)

object InterventionScoringParameters {
    const val WEIGHT_HISTORICAL_HELPFULNESS = 0.40f
    const val WEIGHT_COMPLETION_RATE = 0.25f
    const val WEIGHT_USER_PREFERENCE = 0.15f
    const val WEIGHT_CONTEXTUAL_SUITABILITY = 0.10f
    const val WEIGHT_NOVELTY = 0.10f

    const val IMMEDIATE_REPEAT_PENALTY = 0.50f
    const val SAME_CATEGORY_PENALTY = 0.20f
    const val DEFAULT_COLD_START_SCORE = 0.50f
}


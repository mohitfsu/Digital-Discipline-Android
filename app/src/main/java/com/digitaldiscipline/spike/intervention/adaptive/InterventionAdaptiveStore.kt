package com.digitaldiscipline.spike.intervention.adaptive

import com.digitaldiscipline.spike.data.local.dao.InterventionAdaptiveAggregateDao
import com.digitaldiscipline.spike.data.local.entities.InterventionAdaptiveAggregateEntity
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Statistical summary model supporting 30-day half-life decay.
 */
data class InterventionStats(
    val key: String,
    val startedCount: Int = 0,
    val completedCount: Int = 0,
    val helpedCount: Int = 0,
    val didNotHelpCount: Int = 0,
    val totalFeedbackCount: Int = 0,
    val lastUsedTimestampMs: Long = 0L
) {
    val completionRate: Float
        get() {
            if (startedCount <= 0) return 0.5f
            val decayedCompleted = calculateDecayed(completedCount)
            val decayedStarted = calculateDecayed(startedCount)
            return if (decayedStarted > 0f) (decayedCompleted / decayedStarted).coerceIn(0.0f, 1.0f) else 0.5f
        }

    val helpfulnessRate: Float
        get() {
            if (totalFeedbackCount <= 0) return 0.5f
            val decayedHelped = calculateDecayed(helpedCount)
            val decayedTotal = calculateDecayed(totalFeedbackCount)
            return if (decayedTotal > 0f) (decayedHelped / decayedTotal).coerceIn(0.0f, 1.0f) else 0.5f
        }

    // Explainable confidence: scales smoothly from 0.0 to 1.0 based on decayed observations (max at 10 observations)
    val confidence: Float
        get() {
            val decayedFeedback = calculateDecayed(totalFeedbackCount)
            val decayedCompleted = calculateDecayed(completedCount)
            val observations = decayedFeedback + (decayedCompleted / 2f)
            return (observations / 10.0f).coerceIn(0.0f, 1.0f)
        }

    fun calculateDecayed(raw: Int, nowMs: Long = System.currentTimeMillis()): Float {
        if (lastUsedTimestampMs <= 0L) return raw.toFloat()
        val ageMillis = (nowMs - lastUsedTimestampMs).coerceAtLeast(0L)
        val ageDays = ageMillis.toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)
        val decayFactor = Math.pow(2.0, -ageDays / 30.0) // 30-day half-life
        return (raw.toDouble() * decayFactor).toFloat()
    }
}

class InterventionAdaptiveStore(
    private val dao: InterventionAdaptiveAggregateDao? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val feedbackSamplingRatePercent: Int = 20 // 20% sampling
) {
    private val outcomesHistory = CopyOnWriteArrayList<InterventionOutcome>()

    // Level 1: Global stats by intervention ID
    private val globalStatsMap = ConcurrentHashMap<String, InterventionStats>()

    // Category-level stats
    private val categoryStatsMap = ConcurrentHashMap<InterventionCategory, InterventionStats>()

    // Level 2: Trigger-specific stats ("$targetPackage:$interventionId")
    private val triggerStatsMap = ConcurrentHashMap<String, InterventionStats>()

    // Level 3: Context-specific stats ("$targetPackage:${timeBucket.name}:$interventionId")
    private val contextStatsMap = ConcurrentHashMap<String, InterventionStats>()

    private val sessionCounter = AtomicInteger(0)
    private val isLoadedFromDb = AtomicBoolean(false)

    init {
        if (dao != null) {
            scope.launch {
                loadFromDatabase()
            }
        }
    }

    suspend fun loadFromDatabase() {
        if (dao == null) return
        try {
            val entities = dao.getAllAggregates()
            for (entity in entities) {
                val stats = InterventionStats(
                    key = entity.aggregateKey,
                    startedCount = entity.startedCount,
                    completedCount = entity.completedCount,
                    helpedCount = entity.helpedCount,
                    didNotHelpCount = entity.didNotHelpCount,
                    totalFeedbackCount = entity.totalFeedbackCount,
                    lastUsedTimestampMs = entity.lastUpdatedTimestampMs
                )
                when (entity.evidenceLevel) {
                    "GLOBAL" -> globalStatsMap[entity.interventionId] = stats
                    "CATEGORY" -> {
                        try {
                            val cat = InterventionCategory.valueOf(entity.interventionId)
                            categoryStatsMap[cat] = stats
                        } catch (_: Exception) {}
                    }
                    "TRIGGER" -> {
                        if (entity.targetPackage != null) {
                            val key = "${entity.targetPackage}:${entity.interventionId}"
                            triggerStatsMap[key] = stats
                        }
                    }
                    "CONTEXT" -> {
                        if (entity.targetPackage != null && entity.timeBucket != null) {
                            val key = "${entity.targetPackage}:${entity.timeBucket}:${entity.interventionId}"
                            contextStatsMap[key] = stats
                        }
                    }
                }
            }
            isLoadedFromDb.set(true)
        } catch (_: Exception) {
            // Failure-isolated: if loading fails, memory fallback is used
        }
    }

    fun recordOutcome(outcome: InterventionOutcome) {
        outcomesHistory.add(outcome)
        if (outcomesHistory.size > 200) {
            outcomesHistory.removeAt(0)
        }

        val isCompleted = outcome.status == SessionState.COMPLETED
        val timeBucket = InterventionContext.calculateTimeBucket(outcome.timestampMs)
        val category = InterventionCatalog.getIntervention(outcome.interventionId)?.category

        var helpedInc = 0
        var didNotHelpInc = 0
        var feedbackInc = 0

        when (outcome.helpfulness) {
            HelpfulnessFeedback.HELPED -> {
                helpedInc = 1
                feedbackInc = 1
            }
            HelpfulnessFeedback.DID_NOT_HELP -> {
                didNotHelpInc = 1
                feedbackInc = 1
            }
            HelpfulnessFeedback.NEUTRAL -> {
                feedbackInc = 1
            }
            HelpfulnessFeedback.NOT_ASKED -> {}
        }

        fun updateAndPersist(
            map: ConcurrentHashMap<String, InterventionStats>,
            key: String,
            evidenceLevel: String,
            interventionId: String,
            targetPkg: String? = null,
            bucket: String? = null
        ): InterventionStats {
            val current = map[key] ?: InterventionStats(key = key)
            val updated = current.copy(
                startedCount = current.startedCount + 1,
                completedCount = current.completedCount + (if (isCompleted) 1 else 0),
                helpedCount = current.helpedCount + helpedInc,
                didNotHelpCount = current.didNotHelpCount + didNotHelpInc,
                totalFeedbackCount = current.totalFeedbackCount + feedbackInc,
                lastUsedTimestampMs = outcome.timestampMs
            )
            map[key] = updated
            enqueuePersistence(updated, evidenceLevel, interventionId, targetPkg, bucket)
            return updated
        }

        // 1. Global
        updateAndPersist(globalStatsMap, outcome.interventionId, "GLOBAL", outcome.interventionId)

        // 2. Category
        if (category != null) {
            val curCat = categoryStatsMap[category] ?: InterventionStats(key = category.name)
            val updatedCat = curCat.copy(
                startedCount = curCat.startedCount + 1,
                completedCount = curCat.completedCount + (if (isCompleted) 1 else 0),
                helpedCount = curCat.helpedCount + helpedInc,
                didNotHelpCount = curCat.didNotHelpCount + didNotHelpInc,
                totalFeedbackCount = curCat.totalFeedbackCount + feedbackInc,
                lastUsedTimestampMs = outcome.timestampMs
            )
            categoryStatsMap[category] = updatedCat
            enqueuePersistence(updatedCat, "CATEGORY", category.name)
        }

        // 3. Trigger
        if (outcome.targetPackage.isNotBlank()) {
            val triggerKey = "${outcome.targetPackage}:${outcome.interventionId}"
            updateAndPersist(triggerStatsMap, triggerKey, "TRIGGER", outcome.interventionId, outcome.targetPackage)

            // 4. Context
            val contextKey = "${outcome.targetPackage}:${timeBucket.name}:${outcome.interventionId}"
            updateAndPersist(contextStatsMap, contextKey, "CONTEXT", outcome.interventionId, outcome.targetPackage, timeBucket.name)
        }
    }

    fun recordFeedback(sessionId: String, feedback: HelpfulnessFeedback) {
        val index = outcomesHistory.indexOfLast { it.sessionId == sessionId }
        if (index >= 0) {
            val old = outcomesHistory[index]
            val updated = old.copy(helpfulness = feedback)
            outcomesHistory[index] = updated

            val helpedInc = if (feedback == HelpfulnessFeedback.HELPED) 1 else 0
            val didNotHelpInc = if (feedback == HelpfulnessFeedback.DID_NOT_HELP) 1 else 0
            val timeBucket = InterventionContext.calculateTimeBucket(old.timestampMs)
            val category = InterventionCatalog.getIntervention(old.interventionId)?.category

            fun applyAndPersist(
                map: ConcurrentHashMap<String, InterventionStats>,
                key: String,
                evidenceLevel: String,
                interventionId: String,
                targetPkg: String? = null,
                bucket: String? = null
            ) {
                val current = map[key] ?: InterventionStats(key = key)
                val updatedStats = current.copy(
                    helpedCount = current.helpedCount + helpedInc,
                    didNotHelpCount = current.didNotHelpCount + didNotHelpInc,
                    totalFeedbackCount = current.totalFeedbackCount + 1
                )
                map[key] = updatedStats
                enqueuePersistence(updatedStats, evidenceLevel, interventionId, targetPkg, bucket)
            }

            // 1. Global
            applyAndPersist(globalStatsMap, old.interventionId, "GLOBAL", old.interventionId)

            // 2. Category
            if (category != null) {
                val curCat = categoryStatsMap[category] ?: InterventionStats(key = category.name)
                val updatedCat = curCat.copy(
                    helpedCount = curCat.helpedCount + helpedInc,
                    didNotHelpCount = curCat.didNotHelpCount + didNotHelpInc,
                    totalFeedbackCount = curCat.totalFeedbackCount + 1
                )
                categoryStatsMap[category] = updatedCat
                enqueuePersistence(updatedCat, "CATEGORY", category.name)
            }

            // 3. Trigger
            if (old.targetPackage.isNotBlank()) {
                val triggerKey = "${old.targetPackage}:${old.interventionId}"
                applyAndPersist(triggerStatsMap, triggerKey, "TRIGGER", old.interventionId, old.targetPackage)

                // 4. Context
                val contextKey = "${old.targetPackage}:${timeBucket.name}:${old.interventionId}"
                applyAndPersist(contextStatsMap, contextKey, "CONTEXT", old.interventionId, old.targetPackage, timeBucket.name)
            }
        }
    }

    private fun enqueuePersistence(
        stats: InterventionStats,
        evidenceLevel: String,
        interventionId: String,
        targetPkg: String? = null,
        bucket: String? = null
    ) {
        if (dao == null) return
        scope.launch {
            try {
                val entity = InterventionAdaptiveAggregateEntity(
                    aggregateKey = stats.key,
                    evidenceLevel = evidenceLevel,
                    interventionId = interventionId,
                    targetPackage = targetPkg,
                    timeBucket = bucket,
                    startedCount = stats.startedCount,
                    completedCount = stats.completedCount,
                    helpedCount = stats.helpedCount,
                    didNotHelpCount = stats.didNotHelpCount,
                    totalFeedbackCount = stats.totalFeedbackCount,
                    lastUpdatedTimestampMs = stats.lastUsedTimestampMs
                )
                dao.upsert(entity)
            } catch (_: Exception) {
                // Failure-isolated: database write exceptions never disrupt runtime enforcement
            }
        }
    }

    suspend fun resetAdaptiveMemory() {
        clear()
        try {
            dao?.deleteAll()
        } catch (_: Exception) {}
    }

    suspend fun purgeStaleAggregates(cutoffTimestampMs: Long = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000L)): Int {
        return try {
            dao?.deleteOlderThan(cutoffTimestampMs) ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun getStats(interventionId: String): InterventionStats {
        return globalStatsMap[interventionId] ?: InterventionStats(key = interventionId)
    }

    fun getCategoryStats(category: InterventionCategory): InterventionStats {
        return categoryStatsMap[category] ?: InterventionStats(key = category.name)
    }

    fun getStatsForTrigger(interventionId: String, targetPackage: String): InterventionStats {
        val triggerKey = "$targetPackage:$interventionId"
        return triggerStatsMap[triggerKey] ?: InterventionStats(key = triggerKey)
    }

    fun getStatsForContext(interventionId: String, targetPackage: String, timeBucket: TimeBucket): InterventionStats {
        val contextKey = "$targetPackage:${timeBucket.name}:$interventionId"
        return contextStatsMap[contextKey] ?: InterventionStats(key = contextKey)
    }

    fun getRecentOutcomes(limit: Int = 10): List<InterventionOutcome> {
        return outcomesHistory.takeLast(limit).reversed()
    }

    fun getRecentInterventionIds(limit: Int = 5): List<String> {
        return outcomesHistory.takeLast(limit).map { it.interventionId }.reversed()
    }

    fun shouldSampleFeedback(): Boolean {
        val count = sessionCounter.incrementAndGet()
        val samplingInterval = if (feedbackSamplingRatePercent > 0) 100 / feedbackSamplingRatePercent else 5
        return (count % samplingInterval) == 0
    }

    fun isPersistenceLoaded(): Boolean = isLoadedFromDb.get()

    fun clear() {
        outcomesHistory.clear()
        globalStatsMap.clear()
        categoryStatsMap.clear()
        triggerStatsMap.clear()
        contextStatsMap.clear()
        sessionCounter.set(0)
    }
}

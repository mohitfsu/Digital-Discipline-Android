package com.digitaldiscipline.spike.behaviour.intelligence

import com.digitaldiscipline.spike.data.local.dao.BehaviourExperimentDao
import com.digitaldiscipline.spike.data.local.entities.BehaviourExperimentEntity
import com.digitaldiscipline.spike.data.local.entities.ExperimentStatus
import com.digitaldiscipline.spike.data.local.entities.InterventionEventEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ExperimentRepository(
    private val experimentDao: BehaviourExperimentDao
) {

    fun getAllExperimentsFlow(): Flow<List<BehaviourExperimentEntity>> = experimentDao.getAllExperimentsFlow()

    fun getActiveExperimentFlow(): Flow<BehaviourExperimentEntity?> = experimentDao.getActiveExperimentFlow()

    suspend fun getActiveExperiment(): BehaviourExperimentEntity? = experimentDao.getActiveExperiment()

    suspend fun getExperimentById(experimentId: String): BehaviourExperimentEntity? = experimentDao.getExperimentById(experimentId)

    suspend fun createExperiment(
        goalId: String,
        title: String,
        hypothesis: String,
        interventionConfig: String = "{}"
    ): BehaviourExperimentEntity {
        val now = System.currentTimeMillis()
        val exp = BehaviourExperimentEntity(
            experimentId = "exp_${UUID.randomUUID()}",
            goalId = goalId,
            title = title,
            hypothesis = hypothesis,
            baselineStartDate = now - (7 * 86400000L),
            baselineEndDate = now,
            experimentStartDate = now,
            experimentEndDate = now + (7 * 86400000L),
            interventionConfiguration = interventionConfig,
            status = ExperimentStatus.DRAFT.name,
            createdAt = now
        )
        experimentDao.insertExperiment(exp)
        return exp
    }

    suspend fun startExperiment(experiment: BehaviourExperimentEntity) {
        val now = System.currentTimeMillis()
        val updated = experiment.copy(
            status = ExperimentStatus.ACTIVE.name,
            experimentStartDate = now,
            experimentEndDate = now + (7 * 86400000L)
        )
        experimentDao.updateExperiment(updated)
    }

    suspend fun completeExperiment(
        experiment: BehaviourExperimentEntity,
        experimentMetrics: String,
        conclusion: String
    ) {
        val updated = experiment.copy(
            status = ExperimentStatus.COMPLETED.name,
            experimentMetrics = experimentMetrics,
            conclusion = conclusion,
            completedAt = System.currentTimeMillis()
        )
        experimentDao.updateExperiment(updated)
    }

    suspend fun cancelExperiment(experiment: BehaviourExperimentEntity) {
        val updated = experiment.copy(
            status = ExperimentStatus.CANCELLED.name
        )
        experimentDao.updateExperiment(updated)
    }

    /**
     * Generates curated experiment options for the user to try.
     */
    fun getRecommendedExperiments(goalId: String = "self_goal"): List<BehaviourExperimentEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            BehaviourExperimentEntity(
                experimentId = "rec_exp_evening",
                goalId = goalId,
                title = "Protect Instagram After 9 PM",
                hypothesis = "Adding strict positive friction after 9 PM will reduce late-night screen time.",
                baselineStartDate = now - 7 * 86400000L,
                baselineEndDate = now,
                experimentStartDate = now,
                experimentEndDate = now + 7 * 86400000L,
                status = ExperimentStatus.DRAFT.name
            ),
            BehaviourExperimentEntity(
                experimentId = "rec_exp_breathing",
                goalId = goalId,
                title = "Use Box Breathing for 7 Days",
                hypothesis = "Mindful breathing may provide greater urge delay than short pauses.",
                baselineStartDate = now - 7 * 86400000L,
                baselineEndDate = now,
                experimentStartDate = now,
                experimentEndDate = now + 7 * 86400000L,
                status = ExperimentStatus.DRAFT.name
            ),
            BehaviourExperimentEntity(
                experimentId = "rec_exp_shorter_reward",
                goalId = goalId,
                title = "Reduce Earned Time to 5 Minutes",
                hypothesis = "Shorter screen-time sessions may prevent continuous browsing loops.",
                baselineStartDate = now - 7 * 86400000L,
                baselineEndDate = now,
                experimentStartDate = now,
                experimentEndDate = now + 7 * 86400000L,
                status = ExperimentStatus.DRAFT.name
            )
        )
    }
}

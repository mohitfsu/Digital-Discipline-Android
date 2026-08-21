package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.BehaviourExperimentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviourExperimentDao {

    @Query("SELECT * FROM behaviour_experiments ORDER BY createdAt DESC")
    fun getAllExperimentsFlow(): Flow<List<BehaviourExperimentEntity>>

    @Query("SELECT * FROM behaviour_experiments WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getExperimentsForGoalFlow(goalId: String): Flow<List<BehaviourExperimentEntity>>

    @Query("SELECT * FROM behaviour_experiments WHERE status = :status ORDER BY createdAt DESC")
    fun getExperimentsByStatusFlow(status: String): Flow<List<BehaviourExperimentEntity>>

    @Query("SELECT * FROM behaviour_experiments WHERE status = 'ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    fun getActiveExperimentFlow(): Flow<BehaviourExperimentEntity?>

    @Query("SELECT * FROM behaviour_experiments WHERE status = 'ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveExperiment(): BehaviourExperimentEntity?

    @Query("SELECT * FROM behaviour_experiments WHERE experimentId = :experimentId LIMIT 1")
    suspend fun getExperimentById(experimentId: String): BehaviourExperimentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperiment(experiment: BehaviourExperimentEntity)

    @Update
    suspend fun updateExperiment(experiment: BehaviourExperimentEntity)

    @Query("DELETE FROM behaviour_experiments WHERE experimentId = :experimentId")
    suspend fun deleteExperimentById(experimentId: String)

    @Query("DELETE FROM behaviour_experiments")
    suspend fun clearAllExperiments()
}

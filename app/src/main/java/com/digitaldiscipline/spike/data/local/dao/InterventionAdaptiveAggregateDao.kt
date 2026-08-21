package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.InterventionAdaptiveAggregateEntity

@Dao
interface InterventionAdaptiveAggregateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(aggregate: InterventionAdaptiveAggregateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(aggregates: List<InterventionAdaptiveAggregateEntity>)

    @Query("SELECT * FROM intervention_adaptive_aggregates")
    suspend fun getAllAggregates(): List<InterventionAdaptiveAggregateEntity>

    @Query("SELECT * FROM intervention_adaptive_aggregates WHERE aggregateKey = :key LIMIT 1")
    suspend fun getByKey(key: String): InterventionAdaptiveAggregateEntity?

    @Query("DELETE FROM intervention_adaptive_aggregates")
    suspend fun deleteAll()

    @Query("DELETE FROM intervention_adaptive_aggregates WHERE lastUpdatedTimestampMs < :cutoffTimestampMs")
    suspend fun deleteOlderThan(cutoffTimestampMs: Long): Int
}

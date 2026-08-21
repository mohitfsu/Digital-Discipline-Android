package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.InterventionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionEventDao {

    @Query("SELECT * FROM intervention_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int = 100): Flow<List<InterventionEventEntity>>

    @Query("SELECT * FROM intervention_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<InterventionEventEntity>

    @Query("SELECT * FROM intervention_events WHERE packageName = :packageName AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getEventsForPackage(packageName: String, sinceTimestamp: Long): List<InterventionEventEntity>

    @Query("SELECT * FROM intervention_events WHERE packageName = :packageName AND outcome IN ('COMPLETED', 'EARNED_ACCESS') ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestCompletedEventForPackage(packageName: String): InterventionEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logEvent(event: InterventionEventEntity): Long

    @Update
    suspend fun updateEvent(event: InterventionEventEntity)

    @Query("DELETE FROM intervention_events WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeOlderEvents(cutoffTimestamp: Long)

    @Query("DELETE FROM intervention_events")
    suspend fun clearAllEvents()

    @Query("SELECT COUNT(*) FROM intervention_events WHERE timestamp >= :sinceTimestamp")
    suspend fun getEventCountSince(sinceTimestamp: Long): Int
}

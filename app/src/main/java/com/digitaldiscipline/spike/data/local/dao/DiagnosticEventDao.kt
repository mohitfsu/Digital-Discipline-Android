package com.digitaldiscipline.spike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digitaldiscipline.spike.data.local.entities.DiagnosticEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DiagnosticEventEntity): Long

    @Query("SELECT * FROM diagnostic_events ORDER BY timestamp_ms DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int = 100): Flow<List<DiagnosticEventEntity>>

    @Query("SELECT * FROM diagnostic_events ORDER BY timestamp_ms DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 100): List<DiagnosticEventEntity>

    @Query("DELETE FROM diagnostic_events WHERE id NOT IN (SELECT id FROM diagnostic_events ORDER BY timestamp_ms DESC LIMIT :keepCount)")
    suspend fun pruneOldEvents(keepCount: Int = 200)

    @Query("DELETE FROM diagnostic_events")
    suspend fun clearAllEvents()

    @Query("SELECT COUNT(*) FROM diagnostic_events")
    suspend fun getEventCount(): Int
}

package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.TriggerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerDao {
    @Query("SELECT * FROM triggers ORDER BY priority ASC")
    fun getAllTriggersFlow(): Flow<List<TriggerEntity>>

    @Query("SELECT * FROM triggers WHERE packageName = :packageName AND active = 1")
    suspend fun getActiveTriggersForPackage(packageName: String): List<TriggerEntity>

    @Query("SELECT * FROM triggers WHERE goalId = :goalId")
    suspend fun getTriggersForGoal(goalId: String): List<TriggerEntity>

    @Query("SELECT * FROM triggers WHERE triggerId = :triggerId LIMIT 1")
    suspend fun getTriggerById(triggerId: String): TriggerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrigger(trigger: TriggerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(triggers: List<TriggerEntity>)

    @Update
    suspend fun updateTrigger(trigger: TriggerEntity)

    @Delete
    suspend fun deleteTrigger(trigger: TriggerEntity)

    @Query("DELETE FROM triggers WHERE triggerId = :triggerId")
    suspend fun deleteTriggerById(triggerId: String)

    @Query("DELETE FROM triggers")
    suspend fun clearAllTriggers()
}

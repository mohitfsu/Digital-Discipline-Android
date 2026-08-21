package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.ReplacementBehaviourEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplacementBehaviourDao {
    @Query("SELECT * FROM replacement_behaviours ORDER BY category ASC, title ASC")
    fun getAllBehavioursFlow(): Flow<List<ReplacementBehaviourEntity>>

    @Query("SELECT * FROM replacement_behaviours")
    suspend fun getAllBehaviours(): List<ReplacementBehaviourEntity>

    @Query("SELECT * FROM replacement_behaviours WHERE category = :category")
    suspend fun getBehavioursByCategory(category: String): List<ReplacementBehaviourEntity>

    @Query("SELECT * FROM replacement_behaviours WHERE behaviourId = :behaviourId LIMIT 1")
    suspend fun getBehaviourById(behaviourId: String): ReplacementBehaviourEntity?

    @Query("SELECT * FROM replacement_behaviours WHERE type = :type LIMIT 1")
    suspend fun getBehaviourByType(type: String): ReplacementBehaviourEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviour(behaviour: ReplacementBehaviourEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(behaviours: List<ReplacementBehaviourEntity>)

    @Update
    suspend fun updateBehaviour(behaviour: ReplacementBehaviourEntity)

    @Delete
    suspend fun deleteBehaviour(behaviour: ReplacementBehaviourEntity)

    @Query("DELETE FROM replacement_behaviours")
    suspend fun clearAllBehaviours()
}

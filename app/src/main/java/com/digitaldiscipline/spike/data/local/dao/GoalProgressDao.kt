package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.GoalProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalProgressDao {
    @Query("SELECT * FROM goal_progress WHERE goalId = :goalId ORDER BY dateString DESC")
    fun getProgressForGoalFlow(goalId: String): Flow<List<GoalProgressEntity>>

    @Query("SELECT * FROM goal_progress WHERE goalId = :goalId AND dateString = :dateString LIMIT 1")
    suspend fun getProgressForDate(goalId: String, dateString: String): GoalProgressEntity?

    @Query("SELECT * FROM goal_progress WHERE dateString = :dateString")
    suspend fun getAllProgressForDate(dateString: String): List<GoalProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: GoalProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<GoalProgressEntity>)

    @Query("UPDATE goal_progress SET completedCount = completedCount + 1, completedDurationSeconds = completedDurationSeconds + :durationSec, completionPercentage = MIN(100.0, ((completedCount + 1) * 100.0 / targetCount)), lastUpdated = :now WHERE goalId = :goalId AND dateString = :dateString")
    suspend fun incrementGoalCompletion(goalId: String, dateString: String, durationSec: Int, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM goal_progress")
    suspend fun clearAllProgress()
}

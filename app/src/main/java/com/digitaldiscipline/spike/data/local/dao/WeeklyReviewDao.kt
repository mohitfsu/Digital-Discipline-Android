package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.WeeklyReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReviewDao {

    @Query("SELECT * FROM weekly_reviews ORDER BY weekStart DESC")
    fun getAllWeeklyReviewsFlow(): Flow<List<WeeklyReviewEntity>>

    @Query("SELECT * FROM weekly_reviews WHERE goalId = :goalId ORDER BY weekStart DESC")
    fun getWeeklyReviewsForGoalFlow(goalId: String): Flow<List<WeeklyReviewEntity>>

    @Query("SELECT * FROM weekly_reviews ORDER BY weekStart DESC LIMIT 1")
    fun getLatestWeeklyReviewFlow(): Flow<WeeklyReviewEntity?>

    @Query("SELECT * FROM weekly_reviews ORDER BY weekStart DESC LIMIT 1")
    suspend fun getLatestWeeklyReview(): WeeklyReviewEntity?

    @Query("SELECT * FROM weekly_reviews WHERE reviewId = :reviewId LIMIT 1")
    suspend fun getWeeklyReviewById(reviewId: String): WeeklyReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyReview(review: WeeklyReviewEntity)

    @Query("DELETE FROM weekly_reviews WHERE reviewId = :reviewId")
    suspend fun deleteWeeklyReview(reviewId: String)

    @Query("DELETE FROM weekly_reviews")
    suspend fun clearAllWeeklyReviews()
}

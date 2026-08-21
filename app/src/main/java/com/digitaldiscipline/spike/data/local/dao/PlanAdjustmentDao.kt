package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.PlanAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanAdjustmentDao {

    @Query("SELECT * FROM plan_adjustments ORDER BY createdAt DESC")
    fun getAllAdjustmentsFlow(): Flow<List<PlanAdjustmentEntity>>

    @Query("SELECT * FROM plan_adjustments WHERE status = :status ORDER BY createdAt DESC")
    fun getAdjustmentsByStatusFlow(status: String): Flow<List<PlanAdjustmentEntity>>

    @Query("SELECT * FROM plan_adjustments WHERE status = 'PENDING' ORDER BY createdAt DESC LIMIT 1")
    fun getLatestPendingAdjustmentFlow(): Flow<PlanAdjustmentEntity?>

    @Query("SELECT * FROM plan_adjustments WHERE status = 'PENDING' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestPendingAdjustment(): PlanAdjustmentEntity?

    @Query("SELECT * FROM plan_adjustments WHERE adjustmentId = :adjustmentId")
    suspend fun getAdjustmentById(adjustmentId: String): PlanAdjustmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: PlanAdjustmentEntity)

    @Update
    suspend fun updateAdjustment(adjustment: PlanAdjustmentEntity)

    @Query("UPDATE plan_adjustments SET status = 'EXPIRED' WHERE status = 'PENDING' AND adjustmentId != :keepId")
    suspend fun expireOtherPendingAdjustments(keepId: String)

    @Query("DELETE FROM plan_adjustments WHERE adjustmentId = :adjustmentId")
    suspend fun deleteAdjustmentById(adjustmentId: String)

    @Query("DELETE FROM plan_adjustments")
    suspend fun clearAllAdjustments()
}

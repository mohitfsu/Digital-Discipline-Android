package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.TemporaryUnlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemporaryUnlockDao {
    @Query("SELECT * FROM temporary_unlocks WHERE packageName = :packageName LIMIT 1")
    suspend fun getUnlock(packageName: String): TemporaryUnlockEntity?

    @Query("SELECT * FROM temporary_unlocks WHERE packageName = :packageName LIMIT 1")
    fun getUnlockFlow(packageName: String): Flow<TemporaryUnlockEntity?>

    @Query("SELECT * FROM temporary_unlocks")
    suspend fun getAllUnlocks(): List<TemporaryUnlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(unlock: TemporaryUnlockEntity)

    @Query("DELETE FROM temporary_unlocks WHERE packageName = :packageName")
    suspend fun deleteUnlock(packageName: String)

    @Query("DELETE FROM temporary_unlocks WHERE unlockExpiryElapsedRealtime <= :currentElapsedRealtime")
    suspend fun purgeExpiredUnlocks(currentElapsedRealtime: Long)

    @Query("DELETE FROM temporary_unlocks")
    suspend fun clearAllUnlocks()
}

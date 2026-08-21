package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
    @Query("SELECT * FROM daily_usage WHERE dateString = :dateString ORDER BY attempts DESC, totalForegroundSeconds DESC")
    fun getUsageForDateFlow(dateString: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE dateString = :dateString ORDER BY attempts DESC")
    suspend fun getUsageForDateDirect(dateString: String): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE dateString = :dateString AND packageName = :packageName LIMIT 1")
    suspend fun getUsageEntry(dateString: String, packageName: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage ORDER BY dateString DESC LIMIT 100")
    suspend fun getAllDailyUsage(): List<DailyUsageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: DailyUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DailyUsageEntity>)

    @Query("UPDATE daily_usage SET totalForegroundSeconds = totalForegroundSeconds + :additionalSeconds, lastUpdated = :now WHERE dateString = :dateString AND packageName = :packageName")
    suspend fun incrementUsageSeconds(dateString: String, packageName: String, additionalSeconds: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_usage SET openCount = openCount + 1, lastUpdated = :now WHERE dateString = :dateString AND packageName = :packageName")
    suspend fun incrementOpenCount(dateString: String, packageName: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_usage SET blockCount = blockCount + 1, attempts = attempts + 1, lastUpdated = :now WHERE dateString = :dateString AND packageName = :packageName")
    suspend fun incrementBlockCount(dateString: String, packageName: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_usage SET unlockCount = unlockCount + 1, completed = completed + 1, earnedAccess = earnedAccess + 1, earnedMinutes = earnedMinutes + :earnedMinutes, lastUpdated = :now WHERE dateString = :dateString AND packageName = :packageName")
    suspend fun incrementUnlockCount(dateString: String, packageName: String, earnedMinutes: Int = 10, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM daily_usage")
    suspend fun clearAllDailyUsage()
}

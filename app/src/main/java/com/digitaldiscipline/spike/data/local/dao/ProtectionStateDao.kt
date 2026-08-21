package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.ProtectionStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectionStateDao {
    @Query("SELECT * FROM protection_state WHERE id = 1 LIMIT 1")
    suspend fun getProtectionState(): ProtectionStateEntity?

    @Query("SELECT * FROM protection_state WHERE id = 1 LIMIT 1")
    fun getProtectionStateFlow(): Flow<ProtectionStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProtectionState(state: ProtectionStateEntity)
}

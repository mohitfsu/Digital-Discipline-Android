package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.GeofenceZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceZoneDao {
    @Query("SELECT * FROM geofence_zones ORDER BY isEnabled DESC, createdAtMs DESC")
    fun getAllZonesFlow(): Flow<List<GeofenceZoneEntity>>

    @Query("SELECT * FROM geofence_zones WHERE isEnabled = 1")
    suspend fun getEnabledZones(): List<GeofenceZoneEntity>

    @Query("SELECT * FROM geofence_zones WHERE id = :id")
    suspend fun getZoneById(id: String): GeofenceZoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: GeofenceZoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(zones: List<GeofenceZoneEntity>)

    @Update
    suspend fun updateZone(zone: GeofenceZoneEntity)

    @Delete
    suspend fun deleteZone(zone: GeofenceZoneEntity)

    @Query("DELETE FROM geofence_zones WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM geofence_zones")
    suspend fun deleteAll()
}

package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "geofence_zones")
data class GeofenceZoneEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "Workplace HQ", // e.g. "Main Office", "Greenwood School", "Library"
    val zoneType: String = "WORKPLACE", // "WORKPLACE", "SCHOOL", "LIBRARY", "CUSTOM"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 200f,
    val restrictionMode: String = "BLOCK", // "BLOCK" (strict admin pin) or "INTERVENE" (30s physical/mindful reset)
    val isEnabled: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis()
)

package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.PersonalizationProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalizationProfileDao {

    @Query("SELECT * FROM personalization_profiles WHERE profileId = :profileId LIMIT 1")
    fun getProfileFlow(profileId: String = "profile_self"): Flow<PersonalizationProfileEntity?>

    @Query("SELECT * FROM personalization_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfile(profileId: String = "profile_self"): PersonalizationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PersonalizationProfileEntity)

    @Query("DELETE FROM personalization_profiles WHERE profileId = :profileId")
    suspend fun deleteProfile(profileId: String = "profile_self")

    @Query("DELETE FROM personalization_profiles")
    suspend fun clearAllProfiles()
}

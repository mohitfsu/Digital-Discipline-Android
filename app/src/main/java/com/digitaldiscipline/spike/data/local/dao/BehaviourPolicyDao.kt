package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.BehaviourPolicyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviourPolicyDao {
    @Query("SELECT * FROM behaviour_policies ORDER BY priority ASC, createdAt DESC")
    fun getAllPoliciesFlow(): Flow<List<BehaviourPolicyEntity>>

    @Query("SELECT * FROM behaviour_policies WHERE enabled = 1 ORDER BY priority ASC")
    suspend fun getActivePolicies(): List<BehaviourPolicyEntity>

    @Query("SELECT * FROM behaviour_policies WHERE triggerId = :triggerId AND enabled = 1")
    suspend fun getActivePoliciesForTrigger(triggerId: String): List<BehaviourPolicyEntity>

    @Query("SELECT * FROM behaviour_policies WHERE goalId = :goalId")
    suspend fun getPoliciesForGoal(goalId: String): List<BehaviourPolicyEntity>

    @Query("SELECT * FROM behaviour_policies WHERE policyId = :policyId LIMIT 1")
    suspend fun getPolicyById(policyId: String): BehaviourPolicyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: BehaviourPolicyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(policies: List<BehaviourPolicyEntity>)

    @Update
    suspend fun updatePolicy(policy: BehaviourPolicyEntity)

    @Delete
    suspend fun deletePolicy(policy: BehaviourPolicyEntity)

    @Query("DELETE FROM behaviour_policies WHERE policyId = :policyId")
    suspend fun deletePolicyById(policyId: String)

    @Query("DELETE FROM behaviour_policies")
    suspend fun clearAllPolicies()
}

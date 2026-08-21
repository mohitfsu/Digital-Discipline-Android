package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.WalletSessionEntity
import com.digitaldiscipline.spike.data.local.entities.WalletSessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletSessionDao {

    @Query("SELECT * FROM wallet_sessions WHERE walletId = :walletId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(walletId: String = "wallet_self"): WalletSessionEntity?

    @Query("SELECT * FROM wallet_sessions WHERE walletId = :walletId AND status = 'ACTIVE' LIMIT 1")
    fun getActiveSessionFlow(walletId: String = "wallet_self"): Flow<WalletSessionEntity?>

    @Query("SELECT * FROM wallet_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): WalletSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSession(session: WalletSessionEntity)

    @Query("UPDATE wallet_sessions SET status = :status, consumedSeconds = :consumedSeconds, updatedAt = :now WHERE sessionId = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: String, consumedSeconds: Int, now: Long)

    @Query("UPDATE wallet_sessions SET status = 'INVALIDATED', updatedAt = :now WHERE walletId = :walletId AND status = 'ACTIVE'")
    suspend fun invalidateAllActiveSessions(walletId: String = "wallet_self", now: Long = System.currentTimeMillis())

    @Query("DELETE FROM wallet_sessions")
    suspend fun clearAll()
}

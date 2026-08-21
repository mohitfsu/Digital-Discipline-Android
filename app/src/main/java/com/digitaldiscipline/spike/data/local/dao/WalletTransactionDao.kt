package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.WalletTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletTransactionDao {

    @Query("SELECT * FROM wallet_transactions WHERE walletId = :walletId ORDER BY timestampWallClock DESC LIMIT :limit")
    fun getRecentTransactionsFlow(walletId: String = "wallet_self", limit: Int = 20): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions WHERE walletId = :walletId ORDER BY timestampWallClock ASC")
    suspend fun getAllTransactions(walletId: String = "wallet_self"): List<WalletTransactionEntity>

    @Query("SELECT * FROM wallet_transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTransactionByIdempotencyKey(key: String): WalletTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: WalletTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<WalletTransactionEntity>)

    @Query("DELETE FROM wallet_transactions WHERE walletId = :walletId")
    suspend fun clearTransactionsForWallet(walletId: String)

    @Query("DELETE FROM wallet_transactions")
    suspend fun clearAll()
}

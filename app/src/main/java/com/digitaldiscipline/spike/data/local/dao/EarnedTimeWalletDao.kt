package com.digitaldiscipline.spike.data.local.dao

import androidx.room.*
import com.digitaldiscipline.spike.data.local.entities.EarnedTimeWalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EarnedTimeWalletDao {

    @Query("SELECT * FROM earned_time_wallets WHERE walletId = :walletId LIMIT 1")
    fun getWalletFlow(walletId: String = "wallet_self"): Flow<EarnedTimeWalletEntity?>

    @Query("SELECT * FROM earned_time_wallets WHERE walletId = :walletId LIMIT 1")
    suspend fun getWallet(walletId: String = "wallet_self"): EarnedTimeWalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: EarnedTimeWalletEntity)

    @Query("UPDATE earned_time_wallets SET availableSeconds = :availableSeconds, updatedAt = :now WHERE walletId = :walletId")
    suspend fun updateBalance(walletId: String, availableSeconds: Int, now: Long)

    @Query("DELETE FROM earned_time_wallets WHERE walletId = :walletId")
    suspend fun deleteWallet(walletId: String)

    @Query("DELETE FROM earned_time_wallets")
    suspend fun clearAllWallets()
}

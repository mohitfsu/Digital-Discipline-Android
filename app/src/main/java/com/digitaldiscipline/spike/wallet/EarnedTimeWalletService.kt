package com.digitaldiscipline.spike.wallet

import android.os.SystemClock
import com.digitaldiscipline.spike.data.local.dao.EarnedTimeWalletDao
import com.digitaldiscipline.spike.data.local.dao.WalletSessionDao
import com.digitaldiscipline.spike.data.local.dao.WalletTransactionDao
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

sealed class EarnResult {
    data class Success(val earnedSeconds: Int, val newBalanceSeconds: Int) : EarnResult()
    data class DuplicateIgnored(val existingTransactionId: String) : EarnResult()
    data class CapReached(val reason: String) : EarnResult()
    object InvalidAmount : EarnResult()
}

sealed class SessionStartResult {
    data class Started(val session: WalletSessionEntity) : SessionStartResult()
    data class Resumed(val session: WalletSessionEntity) : SessionStartResult()
    object InsufficientBalance : SessionStartResult()
}

sealed class SessionUpdateResult {
    data class Active(val remainingSeconds: Int) : SessionUpdateResult()
    object Expired : SessionUpdateResult()
    object RebootInvalidated : SessionUpdateResult()
    object NoActiveSession : SessionUpdateResult()
}

sealed class SessionEndResult {
    data class Ended(val consumedSeconds: Int, val remainingSeconds: Int) : SessionEndResult()
    object NoActiveSession : SessionEndResult()
}

class EarnedTimeWalletService(
    private val walletDao: EarnedTimeWalletDao,
    private val transactionDao: WalletTransactionDao,
    private val sessionDao: WalletSessionDao,
    private val preferencesManager: PreferencesManager? = null
) {
    private val mutex = Mutex()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val todayDateString: String
        get() = dateFormat.format(Date())

    fun getWalletFlow(walletId: String = "wallet_self"): Flow<EarnedTimeWalletEntity?> {
        return walletDao.getWalletFlow(walletId)
    }

    fun getRecentTransactionsFlow(walletId: String = "wallet_self", limit: Int = 20): Flow<List<WalletTransactionEntity>> {
        return transactionDao.getRecentTransactionsFlow(walletId, limit)
    }

    fun getActiveSessionFlow(walletId: String = "wallet_self"): Flow<WalletSessionEntity?> {
        return sessionDao.getActiveSessionFlow(walletId)
    }

    suspend fun getWallet(walletId: String = "wallet_self"): EarnedTimeWalletEntity = mutex.withLock {
        getOrCreateWalletInternal(walletId)
    }

    private suspend fun getOrCreateWalletInternal(walletId: String = "wallet_self"): EarnedTimeWalletEntity {
        var wallet = walletDao.getWallet(walletId)
        val today = todayDateString

        val currentMode = try { preferencesManager?.getUserMode() ?: "SELF" } catch (_: Exception) { "SELF" }
        val isChild = currentMode == "CHILD"

        val modeDailyEarnCap = if (isChild) 1800 else 3600
        val modeMaxBalanceCap = if (isChild) 900 else 3600
        val modeMaxSession = if (isChild) 600 else 1800

        if (wallet == null) {
            wallet = EarnedTimeWalletEntity(
                walletId = walletId,
                ownerId = if (isChild) "child" else "self",
                mode = currentMode,
                availableSeconds = 0,
                dailyEarnCapSeconds = modeDailyEarnCap,
                maxBalanceCapSeconds = modeMaxBalanceCap,
                maxSessionSeconds = modeMaxSession,
                lastDateString = today
            )
            walletDao.insertOrUpdateWallet(wallet)
        } else {
            var updated = wallet
            if (wallet.lastDateString != today) {
                // Day rollover: Reset daily counters cleanly
                updated = updated.copy(
                    dailyEarnedSeconds = 0,
                    dailyConsumedSeconds = 0,
                    lastDateString = today,
                    updatedAt = System.currentTimeMillis()
                )
            }
            // Update caps if mode switched to/from CHILD or if child balance exceeds 15m cap
            if (isChild && (updated.maxBalanceCapSeconds > 900 || updated.availableSeconds > 900 || updated.mode != "CHILD")) {
                updated = updated.copy(
                    mode = "CHILD",
                    dailyEarnCapSeconds = 1800,
                    maxBalanceCapSeconds = 900,
                    maxSessionSeconds = 600,
                    availableSeconds = updated.availableSeconds.coerceAtMost(900),
                    updatedAt = System.currentTimeMillis()
                )
            } else if (!isChild && updated.mode == "CHILD") {
                updated = updated.copy(
                    mode = "SELF",
                    dailyEarnCapSeconds = 3600,
                    maxBalanceCapSeconds = 3600,
                    maxSessionSeconds = 1800,
                    updatedAt = System.currentTimeMillis()
                )
            }
            if (updated != wallet) {
                walletDao.insertOrUpdateWallet(updated)
            }
            wallet = updated
        }
        return wallet
    }

    suspend fun saveWallet(wallet: EarnedTimeWalletEntity) = mutex.withLock {
        walletDao.insertOrUpdateWallet(wallet)
    }

    suspend fun resetWalletBalance(walletId: String = "wallet_self") = mutex.withLock {
        val wallet = getOrCreateWalletInternal(walletId)
        val updated = wallet.copy(
            availableSeconds = 0,
            updatedAt = System.currentTimeMillis()
        )
        walletDao.insertOrUpdateWallet(updated)
        sessionDao.getActiveSession(walletId)?.let { active ->
            sessionDao.updateSessionStatus(active.sessionId, WalletSessionStatus.ENDED.name, active.consumedSeconds, System.currentTimeMillis())
        }
    }

    /**
     * Authoritatively adds earned time to wallet ledger with caps and idempotency.
     */
    suspend fun earnTime(
        amountSeconds: Int,
        source: String,
        triggerPackage: String? = null,
        goalId: String? = null,
        idempotencyKey: String? = null,
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): EarnResult = mutex.withLock {
        if (amountSeconds <= 0) return EarnResult.InvalidAmount

        // 1. Idempotency Check
        if (idempotencyKey != null) {
            val existing = transactionDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                return EarnResult.DuplicateIgnored(existing.transactionId)
            }
        }

        val wallet = getOrCreateWalletInternal("wallet_self")

        // 2. Enforce Daily Cap
        val remainingDailyAllowance = (wallet.dailyEarnCapSeconds - wallet.dailyEarnedSeconds).coerceAtLeast(0)
        if (remainingDailyAllowance <= 0) {
            return EarnResult.CapReached("Daily earning cap reached (${wallet.dailyEarnCapSeconds / 60} min)")
        }

        // 3. Enforce Max Wallet Balance Cap (e.g. 15 mins for child)
        val remainingBalanceAllowance = (wallet.maxBalanceCapSeconds - wallet.availableSeconds).coerceAtLeast(0)
        if (remainingBalanceAllowance <= 0) {
            return EarnResult.CapReached("Wallet is full (${wallet.maxBalanceCapSeconds / 60} min max). Use your earned time first!")
        }

        val finalEarned = amountSeconds.coerceAtMost(remainingDailyAllowance).coerceAtMost(remainingBalanceAllowance)
        if (finalEarned <= 0) {
            return EarnResult.CapReached("Cap limit prevents additional earnings")
        }

        val newBalance = wallet.availableSeconds + finalEarned
        val txId = "tx_${UUID.randomUUID()}"

        val transaction = WalletTransactionEntity(
            transactionId = txId,
            walletId = wallet.walletId,
            type = WalletTransactionType.EARN.name,
            amountSeconds = finalEarned,
            balanceAfterSeconds = newBalance,
            source = source,
            triggerPackage = triggerPackage,
            idempotencyKey = idempotencyKey,
            goalId = goalId,
            timestampWallClock = System.currentTimeMillis(),
            elapsedRealtime = nowElapsed
        )
        transactionDao.insertTransaction(transaction)

        val updatedWallet = wallet.copy(
            availableSeconds = newBalance,
            dailyEarnedSeconds = wallet.dailyEarnedSeconds + finalEarned,
            lifetimeEarnedSeconds = wallet.lifetimeEarnedSeconds + finalEarned,
            lastUpdatedElapsedRealtime = nowElapsed,
            lastUpdatedWallClock = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        walletDao.insertOrUpdateWallet(updatedWallet)

        return EarnResult.Success(earnedSeconds = finalEarned, newBalanceSeconds = newBalance)
    }

    /**
     * Starts or resumes a monotonic session on a target app.
     */
    suspend fun startOrResumeSession(
        triggerPackage: String,
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): SessionStartResult = mutex.withLock {
        val wallet = getOrCreateWalletInternal("wallet_self")
        val activeSession = sessionDao.getActiveSession(wallet.walletId)

        // Detect reboot during active session
        if (activeSession != null && nowElapsed < activeSession.startedElapsedRealtime) {
            sessionDao.updateSessionStatus(
                sessionId = activeSession.sessionId,
                status = WalletSessionStatus.INVALIDATED.name,
                consumedSeconds = activeSession.consumedSeconds,
                now = System.currentTimeMillis()
            )
        } else if (activeSession != null) {
            if (activeSession.triggerPackage == triggerPackage) {
                // Resume existing session on same package
                return SessionStartResult.Resumed(activeSession)
            } else {
                // Finalize session on old package before starting on new package
                finalizeSessionInternal(activeSession, wallet, nowElapsed)
            }
        }

        // Fresh check of balance
        val currentWallet = walletDao.getWallet(wallet.walletId) ?: wallet
        if (currentWallet.availableSeconds <= 0) {
            return SessionStartResult.InsufficientBalance
        }

        val sessionId = "sess_${UUID.randomUUID()}"
        val maxAllowed = currentWallet.availableSeconds.coerceAtMost(currentWallet.maxSessionSeconds)

        val session = WalletSessionEntity(
            sessionId = sessionId,
            walletId = currentWallet.walletId,
            triggerPackage = triggerPackage,
            startedElapsedRealtime = nowElapsed,
            lastHeartbeatElapsedRealtime = nowElapsed,
            startedWallClock = System.currentTimeMillis(),
            initialWalletSeconds = currentWallet.availableSeconds,
            consumedSeconds = 0,
            maxAllowedSeconds = maxAllowed,
            status = WalletSessionStatus.ACTIVE.name
        )
        sessionDao.insertOrUpdateSession(session)

        return SessionStartResult.Started(session)
    }

    /**
     * Heartbeat called periodically while target app is active in foreground.
     */
    suspend fun heartbeatOrUpdateSession(
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): SessionUpdateResult = mutex.withLock {
        val wallet = getOrCreateWalletInternal("wallet_self")
        val activeSession = sessionDao.getActiveSession(wallet.walletId) ?: return SessionUpdateResult.NoActiveSession

        // Reboot check
        if (nowElapsed < activeSession.startedElapsedRealtime) {
            sessionDao.updateSessionStatus(activeSession.sessionId, WalletSessionStatus.INVALIDATED.name, activeSession.consumedSeconds, System.currentTimeMillis())
            return SessionUpdateResult.RebootInvalidated
        }

        val deltaSeconds = ((nowElapsed - activeSession.lastHeartbeatElapsedRealtime) / 1000L).toInt().coerceAtLeast(0)
        if (deltaSeconds <= 0) {
            return SessionUpdateResult.Active(wallet.availableSeconds)
        }

        val newConsumed = activeSession.consumedSeconds + deltaSeconds
        val newAvailable = (wallet.availableSeconds - deltaSeconds).coerceAtLeast(0)

        val updatedWallet = wallet.copy(
            availableSeconds = newAvailable,
            dailyConsumedSeconds = wallet.dailyConsumedSeconds + deltaSeconds,
            lifetimeConsumedSeconds = wallet.lifetimeConsumedSeconds + deltaSeconds,
            lastUpdatedElapsedRealtime = nowElapsed,
            lastUpdatedWallClock = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        walletDao.insertOrUpdateWallet(updatedWallet)

        if (newAvailable <= 0 || newConsumed >= activeSession.maxAllowedSeconds) {
            // Session has expired
            sessionDao.updateSessionStatus(activeSession.sessionId, WalletSessionStatus.EXPIRED.name, newConsumed, System.currentTimeMillis())

            val txId = "tx_${UUID.randomUUID()}"
            transactionDao.insertTransaction(
                WalletTransactionEntity(
                    transactionId = txId,
                    walletId = wallet.walletId,
                    type = WalletTransactionType.SPEND.name,
                    amountSeconds = newConsumed,
                    balanceAfterSeconds = newAvailable,
                    source = "APP_SESSION",
                    triggerPackage = activeSession.triggerPackage,
                    sessionId = activeSession.sessionId,
                    elapsedRealtime = nowElapsed
                )
            )
            return SessionUpdateResult.Expired
        } else {
            val updatedSession = activeSession.copy(
                lastHeartbeatElapsedRealtime = nowElapsed,
                consumedSeconds = newConsumed,
                updatedAt = System.currentTimeMillis()
            )
            sessionDao.insertOrUpdateSession(updatedSession)
            return SessionUpdateResult.Active(newAvailable)
        }
    }

    /**
     * Pauses or ends active session on app exit / backgrounding.
     */
    suspend fun pauseOrEndSession(
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): SessionEndResult = mutex.withLock {
        val wallet = getOrCreateWalletInternal("wallet_self")
        val activeSession = sessionDao.getActiveSession(wallet.walletId) ?: return SessionEndResult.NoActiveSession

        return finalizeSessionInternal(activeSession, wallet, nowElapsed)
    }

    private suspend fun finalizeSessionInternal(
        session: WalletSessionEntity,
        wallet: EarnedTimeWalletEntity,
        nowElapsed: Long
    ): SessionEndResult {
        // Reboot check
        if (nowElapsed < session.startedElapsedRealtime) {
            sessionDao.updateSessionStatus(session.sessionId, WalletSessionStatus.INVALIDATED.name, session.consumedSeconds, System.currentTimeMillis())
            return SessionEndResult.NoActiveSession
        }

        val deltaSeconds = ((nowElapsed - session.lastHeartbeatElapsedRealtime) / 1000L).toInt().coerceAtLeast(0)
        val totalConsumed = (session.consumedSeconds + deltaSeconds).coerceAtMost(session.maxAllowedSeconds)
        val newAvailable = (wallet.availableSeconds - deltaSeconds).coerceAtLeast(0)

        val updatedWallet = wallet.copy(
            availableSeconds = newAvailable,
            dailyConsumedSeconds = wallet.dailyConsumedSeconds + deltaSeconds,
            lifetimeConsumedSeconds = wallet.lifetimeConsumedSeconds + deltaSeconds,
            lastUpdatedElapsedRealtime = nowElapsed,
            lastUpdatedWallClock = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        walletDao.insertOrUpdateWallet(updatedWallet)

        sessionDao.updateSessionStatus(session.sessionId, WalletSessionStatus.ENDED.name, totalConsumed, System.currentTimeMillis())

        if (totalConsumed > 0) {
            val txId = "tx_${UUID.randomUUID()}"
            transactionDao.insertTransaction(
                WalletTransactionEntity(
                    transactionId = txId,
                    walletId = wallet.walletId,
                    type = WalletTransactionType.SPEND.name,
                    amountSeconds = totalConsumed,
                    balanceAfterSeconds = newAvailable,
                    source = "APP_SESSION",
                    triggerPackage = session.triggerPackage,
                    sessionId = session.sessionId,
                    elapsedRealtime = nowElapsed
                )
            )
        }

        return SessionEndResult.Ended(consumedSeconds = totalConsumed, remainingSeconds = newAvailable)
    }

    /**
     * Recovers state after process restart or reboot.
     */
    suspend fun recoverAfterCrashOrReboot(nowElapsed: Long = SystemClock.elapsedRealtime()) = mutex.withLock {
        val activeSession = sessionDao.getActiveSession("wallet_self")
        if (activeSession != null) {
            if (nowElapsed < activeSession.startedElapsedRealtime) {
                // Device rebooted: Invalidate session
                sessionDao.updateSessionStatus(activeSession.sessionId, WalletSessionStatus.INVALIDATED.name, activeSession.consumedSeconds, System.currentTimeMillis())
            } else {
                // Process died during session: Finalize consumption up to last known heartbeat
                val wallet = getOrCreateWalletInternal("wallet_self")
                finalizeSessionInternal(activeSession, wallet, nowElapsed)
            }
        }
    }

    /**
     * Audits and reconstructs the mathematical balance from raw ledger transactions.
     */
    suspend fun reconstructBalanceFromLedger(walletId: String = "wallet_self"): Int {
        val txs = transactionDao.getAllTransactions(walletId)
        var balance = 0
        txs.forEach { tx ->
            when (tx.type) {
                WalletTransactionType.EARN.name -> balance += tx.amountSeconds
                WalletTransactionType.SPEND.name -> balance = (balance - tx.amountSeconds).coerceAtLeast(0)
                WalletTransactionType.EXPIRE.name -> balance = (balance - tx.amountSeconds).coerceAtLeast(0)
                WalletTransactionType.ADJUSTMENT.name -> balance = (balance + tx.amountSeconds).coerceAtLeast(0)
                WalletTransactionType.RESET.name -> balance = tx.amountSeconds
            }
        }
        return balance
    }
}

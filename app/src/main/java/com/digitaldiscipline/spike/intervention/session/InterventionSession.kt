package com.digitaldiscipline.spike.intervention.session

import android.os.SystemClock
import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.intervention.model.InterventionDifficulty
import java.util.UUID

enum class SessionState {
    CREATED,
    READY,
    RUNNING,
    COMPLETING,
    COMPLETED,
    EXPIRED,
    CANCELLED,
    FAILED
}

enum class PolicySource {
    SELF,
    PARENT
}

data class InterventionSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val intervention: InterventionDefinition,
    val targetPackage: String,
    val policySource: PolicySource = PolicySource.SELF,
    val difficulty: InterventionDifficulty = InterventionDifficulty.STANDARD,
    val requiredReps: Int = intervention.getRepsForDifficulty(difficulty),
    val requiredDurationSeconds: Int = intervention.getDurationForDifficulty(difficulty),
    val rewardSeconds: Int = intervention.rewardSeconds,
    val timeoutSeconds: Int = (if (requiredDurationSeconds > 0) requiredDurationSeconds * 3 else 180).coerceAtLeast(30),
    val createdAtRealtimeMs: Long = SystemClock.elapsedRealtime(),
    var startedAtRealtimeMs: Long = 0L,
    var completedAtRealtimeMs: Long = 0L,
    var state: SessionState = SessionState.CREATED,
    var currentProgress: Int = 0,
    var failureReason: String? = null
) {
    val isTerminal: Boolean
        get() = state == SessionState.COMPLETED ||
                state == SessionState.EXPIRED ||
                state == SessionState.CANCELLED ||
                state == SessionState.FAILED

    fun prepare(): Boolean {
        if (state != SessionState.CREATED) return false
        state = SessionState.READY
        return true
    }

    fun start(): Boolean {
        if (state != SessionState.READY && state != SessionState.CREATED) return false
        state = SessionState.RUNNING
        startedAtRealtimeMs = SystemClock.elapsedRealtime()
        return true
    }

    fun updateProgress(newProgress: Int): Boolean {
        if (state != SessionState.RUNNING) return false
        if (isExpired()) {
            expire()
            return false
        }
        currentProgress = newProgress.coerceAtLeast(0)
        return true
    }

    fun markCompleting(): Boolean {
        if (state != SessionState.RUNNING) return false
        if (isExpired()) {
            expire()
            return false
        }
        state = SessionState.COMPLETING
        return true
    }

    fun complete(): Boolean {
        if (state == SessionState.COMPLETED) {
            // Idempotent success
            return true
        }
        if (state != SessionState.RUNNING && state != SessionState.COMPLETING) {
            return false
        }
        state = SessionState.COMPLETED
        completedAtRealtimeMs = SystemClock.elapsedRealtime()
        return true
    }

    fun cancel(reason: String = "User exited"): Boolean {
        if (isTerminal) return false
        state = SessionState.CANCELLED
        failureReason = reason
        return true
    }

    fun fail(reason: String): Boolean {
        if (isTerminal) return false
        state = SessionState.FAILED
        failureReason = reason
        return true
    }

    fun expire(): Boolean {
        if (isTerminal) return false
        state = SessionState.EXPIRED
        failureReason = "Session timed out"
        return true
    }

    fun isExpired(): Boolean {
        if (startedAtRealtimeMs <= 0L) return false
        val elapsedSec = (SystemClock.elapsedRealtime() - startedAtRealtimeMs) / 1000L
        return elapsedSec > timeoutSeconds
    }

    fun getElapsedDurationSeconds(): Long {
        if (startedAtRealtimeMs <= 0L) return 0L
        val end = if (completedAtRealtimeMs > 0L) completedAtRealtimeMs else SystemClock.elapsedRealtime()
        return (end - startedAtRealtimeMs) / 1000L
    }
}

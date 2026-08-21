package com.digitaldiscipline.spike.intervention.validation

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.digitaldiscipline.spike.intervention.session.InterventionSession

/**
 * Monotonic Timer Validator.
 * Validates breathing cycles, meditation, and timed physical resets
 * using monotonic elapsed time (SystemClock.elapsedRealtime()).
 */
class TimerValidator : InterventionValidator {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeSession: InterventionSession? = null
    private var callback: ((ValidationResult) -> Unit)? = null
    private var pollRunnable: Runnable? = null

    override fun isSupported(): Boolean = true

    override fun startValidation(
        session: InterventionSession,
        onResult: (ValidationResult) -> Unit
    ) {
        stopValidation()

        activeSession = session
        callback = onResult

        val targetSeconds = session.requiredDurationSeconds.coerceAtLeast(1)
        val startRealtimeMs = SystemClock.elapsedRealtime()

        pollRunnable = object : Runnable {
            override fun run() {
                val currentSession = activeSession ?: return
                val cb = callback ?: return

                if (currentSession.isExpired()) {
                    stopValidation()
                    cb(ValidationResult.Failed("Session expired"))
                    return
                }

                val elapsedSec = ((SystemClock.elapsedRealtime() - startRealtimeMs) / 1000L).toInt()
                currentSession.updateProgress(elapsedSec)
                cb(ValidationResult.Progress(currentProgress = elapsedSec, requiredTarget = targetSeconds))

                if (elapsedSec >= targetSeconds) {
                    currentSession.markCompleting()
                    currentSession.complete()
                    stopValidation()
                    cb(ValidationResult.Completed(currentSession))
                } else {
                    mainHandler.postDelayed(this, 500L)
                }
            }
        }

        pollRunnable?.let { mainHandler.post(it) }
    }

    override fun stopValidation() {
        pollRunnable?.let { mainHandler.removeCallbacks(it) }
        pollRunnable = null
        activeSession = null
        callback = null
    }
}

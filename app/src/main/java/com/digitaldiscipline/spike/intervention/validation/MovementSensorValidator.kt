package com.digitaldiscipline.spike.intervention.validation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.digitaldiscipline.spike.intervention.session.InterventionSession
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * On-device Movement Sensor Validator.
 * Validates repetitive physical actions (e.g. squats, jumping jacks, sit-to-stand, push-ups)
 * using device accelerometer signals with anti-circumvention protection.
 *
 * Invariants:
 * - Sensors active ONLY during an active session.
 * - Unregistered immediately on stop/completion/failure.
 * - Rejects rapid shaking (<600ms per rep).
 * - Requires plausible upward & downward movement amplitude.
 */
class MovementSensorValidator(
    private val context: Context
) : InterventionValidator, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var activeSession: InterventionSession? = null
    private var callback: ((ValidationResult) -> Unit)? = null

    // Movement tracking state
    private var lastRepTimestampMs: Long = 0L
    private var repCount: Int = 0
    private var isDownPhase: Boolean = false
    private var baselineMagnitude: Float = 9.8f
    private var isListening: Boolean = false

    // Anti-cheat parameters
    private val minRepIntervalMs: Long = 650L // Maximum ~90 reps/min (human ceiling)
    private val maxRepIntervalMs: Long = 8000L // Reset phase if pause is too long
    private val downwardThreshold: Float = 2.2f // Delta below baseline
    private val upwardThreshold: Float = 2.2f   // Delta above baseline
    private val maxSpikeFilter: Float = 28.0f   // Rejects violent phone shaking/slamming

    override fun isSupported(): Boolean = accelerometer != null

    override fun startValidation(
        session: InterventionSession,
        onResult: (ValidationResult) -> Unit
    ) {
        stopValidation() // Clean up any lingering listener

        activeSession = session
        callback = onResult
        repCount = session.currentProgress
        isDownPhase = false
        lastRepTimestampMs = SystemClock.elapsedRealtime()

        if (accelerometer != null && sensorManager != null) {
            isListening = sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        if (!isListening) {
            // Fallback if sensor cannot be registered
            onResult(ValidationResult.Failed("Sensor unavailable on this device"))
        }
    }

    override fun stopValidation() {
        if (isListening && sensorManager != null) {
            try {
                sensorManager.unregisterListener(this)
            } catch (_: Exception) {}
            isListening = false
        }
        activeSession = null
        callback = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val session = activeSession ?: return
        val cb = callback ?: return

        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val nowMs = SystemClock.elapsedRealtime()

        // Check session timeout
        if (session.isExpired()) {
            stopValidation()
            cb(ValidationResult.Failed("Intervention session timed out"))
            return
        }

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        val magnitude = sqrt(ax * ax + ay * ay + az * az)

        // Filter out extreme unrealistic acceleration spikes (e.g. dropping or hitting phone)
        if (magnitude > maxSpikeFilter) {
            return
        }

        // Smooth baseline slowly
        baselineMagnitude = baselineMagnitude * 0.95f + magnitude * 0.05f
        val delta = magnitude - baselineMagnitude

        // State Machine for Movement Repetition:
        // Phase 1: Downward motion (drop in acceleration / negative delta)
        if (!isDownPhase && delta < -downwardThreshold) {
            isDownPhase = true
        }

        // Phase 2: Upward rebound & completion of full cycle
        if (isDownPhase && delta > upwardThreshold) {
            val timeSinceLastRep = nowMs - lastRepTimestampMs

            if (timeSinceLastRep in minRepIntervalMs..maxRepIntervalMs) {
                repCount++
                lastRepTimestampMs = nowMs
                isDownPhase = false

                session.updateProgress(repCount)
                cb(ValidationResult.Progress(currentProgress = repCount, requiredTarget = session.requiredReps))

                if (repCount >= session.requiredReps) {
                    session.markCompleting()
                    session.complete()
                    stopValidation()
                    cb(ValidationResult.Completed(session))
                }
            } else if (timeSinceLastRep < minRepIntervalMs) {
                // Rejection: Too fast! (User is rapidly shaking device)
                isDownPhase = false
            } else {
                // Reset phase on long pause
                isDownPhase = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

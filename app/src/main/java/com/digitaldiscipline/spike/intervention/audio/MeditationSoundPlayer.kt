package com.digitaldiscipline.spike.intervention.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * On-Device Real-Time Meditation & Ambient Sound Synthesizer.
 *
 * Generates soothing harmonic resonance in real-time without requiring external audio files or internet access:
 * - Solfeggio 528 Hz & 432 Hz warm harmonic tones.
 * - Gentle 108 Hz sub-bass ambient drone.
 * - Theta binaural modulation (6 Hz) for deep mindfulness and relaxation.
 * - Periodic soft singing bowl chime envelope.
 */
object MeditationSoundPlayer {

    private const val SAMPLE_RATE = 44100
    private var audioTrack: AudioTrack? = null
    private var isPlayingState: Boolean = false
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val isPlaying: Boolean
        get() = isPlayingState

    fun play() {
        if (isPlayingState) return
        isPlayingState = true

        playbackJob = scope.launch {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, SAMPLE_RATE * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            val buffer = ShortArray(2048)
            var phase432 = 0.0
            var phase528 = 0.0
            var phase108 = 0.0
            var phaseBinaural = 0.0
            var sampleIndex = 0L

            val inc432 = 2.0 * PI * 432.0 / SAMPLE_RATE
            val inc528 = 2.0 * PI * 528.0 / SAMPLE_RATE
            val inc108 = 2.0 * PI * 108.0 / SAMPLE_RATE
            val incBinaural = 2.0 * PI * 6.0 / SAMPLE_RATE // 6 Hz theta pulse

            try {
                while (isPlayingState && isActive) {
                    for (i in buffer.indices) {
                        // Periodic singing bowl chime envelope (every ~7 seconds)
                        val chimePeriodSamples = SAMPLE_RATE * 7
                        val chimePos = (sampleIndex % chimePeriodSamples).toDouble() / SAMPLE_RATE
                        val chimeEnv = exp(-chimePos * 0.9) * 0.45

                        // Binaural depth modulation
                        val thetaLfo = (1.0 + 0.3 * sin(phaseBinaural)) * 0.5

                        // Multi-harmonic soothing synthesis
                        val s108 = sin(phase108) * 0.20
                        val s432 = sin(phase432) * 0.15 * thetaLfo
                        val s528 = sin(phase528) * chimeEnv

                        val combined = (s108 + s432 + s528) * 0.45
                        val sampleValue = (combined * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        buffer[i] = sampleValue.toShort()

                        phase432 += inc432
                        phase528 += inc528
                        phase108 += inc108
                        phaseBinaural += incBinaural
                        sampleIndex++
                    }

                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        isPlayingState = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}

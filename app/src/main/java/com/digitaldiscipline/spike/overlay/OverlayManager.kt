package com.digitaldiscipline.spike.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.intervention.adaptive.HelpfulnessFeedback
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.security.ParentPinManager

class OverlayManager(
    private val context: Context,
    private val pinManager: ParentPinManager
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeOverlayView: View? = null
    private var activeLifecycleOwner: OverlayLifecycleOwner? = null
    private var currentTargetPackage: String? = null

    var onInterventionCompletedListener: ((targetPackage: String, durationMs: Long) -> Unit)? = null
    var onExitToHomeListener: (() -> Unit)? = null

    private fun requestAudioFocusToMuteBackgroundMedia() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* no-op */ }
                    .build()
                audioFocusRequest = focusRequest
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (_: Throwable) {}
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager?.abandonAudioFocusRequest(it)
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (_: Throwable) {}
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    @SuppressLint("SetTextI18n")
    fun showInterventionOverlay(
        targetPackage: String,
        targetAppName: String = "Application",
        unlockDurationSeconds: Int = 300,
        attemptNumber: Int = 1,
        ruleMode: RuleMode = RuleMode.EARN,
        pauseDurationSeconds: Int = 10,
        breathingDurationSeconds: Int = 30,
        squatsTargetCount: Int = 10,
        shouldSampleFeedback: Boolean = false,
        onFeedbackSubmitted: ((HelpfulnessFeedback) -> Unit)? = null
    ) {
        if (!canDrawOverlays()) {
            com.digitaldiscipline.spike.logging.DiagnosticLogger.logOverlayPermissionMissing()
            EventLogger.log(
                source = "OVERLAY",
                packageName = targetPackage,
                eventType = "OVERLAY_PERMISSION_MISSING",
                details = "SYSTEM_ALERT_WINDOW not granted"
            )
            return
        }

        mainHandler.post {
            if (activeOverlayView != null) {
                if (currentTargetPackage == targetPackage) return@post
                hideOverlay()
            }

            currentTargetPackage = targetPackage
            val startTime = System.currentTimeMillis()

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val lifecycleOwner = OverlayLifecycleOwner()
            activeLifecycleOwner = lifecycleOwner

            val composeView = ComposeView(context).apply {
                attachOverlayLifecycle(lifecycleOwner)
                setContent {
                    InterventionOverlayContent(
                        context = context,
                        targetPackage = targetPackage,
                        targetAppName = targetAppName,
                        unlockDurationSeconds = unlockDurationSeconds,
                        attemptNumber = attemptNumber,
                        ruleMode = ruleMode,
                        pauseDurationSeconds = pauseDurationSeconds,
                        breathingDurationSeconds = breathingDurationSeconds,
                        squatsTargetCount = squatsTargetCount,
                        pinManager = pinManager,
                        shouldSampleFeedback = shouldSampleFeedback,
                        onFeedbackSubmitted = onFeedbackSubmitted,
                        onComplete = { durationSec ->
                            hideOverlay()
                            onInterventionCompletedListener?.invoke(targetPackage, durationSec * 1000L)
                        },
                        onExitHome = {
                            hideOverlay()
                            onExitToHomeListener?.invoke()
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(homeIntent)
                        }
                    )
                }
            }

            activeOverlayView = composeView

            try {
                windowManager.addView(composeView, layoutParams)
                requestAudioFocusToMuteBackgroundMedia()
                val renderLatency = System.currentTimeMillis() - startTime

                EventLogger.log(
                    source = "OVERLAY",
                    packageName = targetPackage,
                    eventType = "COMPOSE_OVERLAY_SHOWN",
                    latencyMs = renderLatency,
                    details = "Target: $targetAppName | Mode: $ruleMode | Pause: ${pauseDurationSeconds}s | Breathe: ${breathingDurationSeconds}s | Squats: $squatsTargetCount | Attempt: $attemptNumber | Unlock Window: ${unlockDurationSeconds}s"
                )
            } catch (e: Exception) {
                EventLogger.log(
                    source = "OVERLAY",
                    packageName = targetPackage,
                    eventType = "OVERLAY_SHOW_ERROR",
                    details = e.message ?: "Failed to add compose view"
                )
            }
        }
    }

    fun hideOverlay() {
        mainHandler.post {
            abandonAudioFocus()
            activeOverlayView?.let { view ->
                try {
                    windowManager.removeView(view)
                    activeLifecycleOwner?.destroy()
                    activeLifecycleOwner = null
                    EventLogger.log(
                        source = "OVERLAY",
                        packageName = currentTargetPackage ?: "unknown",
                        eventType = "INTERVENTION_DISMISSED"
                    )
                } catch (e: Exception) {
                    // Ignore if already removed
                }
                activeOverlayView = null
                currentTargetPackage = null
            }
        }
    }

    fun isOverlayActive(): Boolean = activeOverlayView != null
}

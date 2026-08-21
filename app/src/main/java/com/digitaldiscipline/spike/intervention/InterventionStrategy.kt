package com.digitaldiscipline.spike.intervention

import android.content.Context

interface InterventionStrategy {
    fun showIntervention(
        context: Context,
        targetPackage: String,
        targetAppName: String,
        unlockDurationSeconds: Int,
        onComplete: (durationMs: Long) -> Unit,
        onDismiss: () -> Unit
    )

    fun dismissIntervention()
}

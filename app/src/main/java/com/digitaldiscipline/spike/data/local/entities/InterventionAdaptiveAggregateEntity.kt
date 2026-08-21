package com.digitaldiscipline.spike.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable aggregated statistical model for personalized adaptive interventions.
 *
 * PRIVACY CONTRACT:
 * - Never persists timestamps of individual distraction attempts.
 * - Never collects screenshots, keystrokes, screen contents, or URLs.
 * - Stores only coarsened, bounded counters per evidence level (GLOBAL, CAT, TRIG, CTX).
 */
@Entity(
    tableName = "intervention_adaptive_aggregates",
    indices = [
        Index(value = ["interventionId"]),
        Index(value = ["targetPackage"]),
        Index(value = ["evidenceLevel"])
    ]
)
data class InterventionAdaptiveAggregateEntity(
    @PrimaryKey
    val aggregateKey: String, // e.g. "GLOBAL:BOX_BREATHING", "CAT:BREATHING", "TRIG:com.instagram.android:BOX_BREATHING", "CTX:com.instagram.android:EVENING:BOX_BREATHING"
    val evidenceLevel: String, // "GLOBAL", "CATEGORY", "TRIGGER", "CONTEXT"
    val interventionId: String,
    val targetPackage: String? = null,
    val timeBucket: String? = null,
    val startedCount: Int = 0,
    val completedCount: Int = 0,
    val helpedCount: Int = 0,
    val didNotHelpCount: Int = 0,
    val totalFeedbackCount: Int = 0,
    val lastUpdatedTimestampMs: Long = System.currentTimeMillis()
)

package com.digitaldiscipline.spike.intervention.vision

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * High-performance 2D/3D geometric angle calculator for skeletal landmarks.
 */
object PoseAngleCalculator {

    /**
     * Calculates the interior angle in degrees between three landmarks: first (A), mid/vertex (B), and end (C).
     * Returns an angle between 0.0 and 180.0 degrees.
     */
    fun calculateAngle(
        first: PoseLandmark?,
        mid: PoseLandmark?,
        end: PoseLandmark?
    ): Double {
        if (first == null || mid == null || end == null) return -1.0
        val firstP = first.position
        val midP = mid.position
        val endP = end.position
        return calculateAngle(firstP.x, firstP.y, midP.x, midP.y, endP.x, endP.y)
    }

    /**
     * Calculates the interior angle in degrees between three 2D points (A, B=vertex, C).
     */
    fun calculateAngle(
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float
    ): Double {
        val angle1 = atan2((p1y - p2y).toDouble(), (p1x - p2x).toDouble())
        val angle2 = atan2((p3y - p2y).toDouble(), (p3x - p2x).toDouble())

        var degrees = Math.toDegrees(angle1 - angle2)
        degrees = Math.abs(degrees)
        if (degrees > 180.0) {
            degrees = 360.0 - degrees
        }
        return degrees
    }

    /**
     * Calculates the distance between two landmarks.
     */
    fun calculateDistance(p1: PoseLandmark?, p2: PoseLandmark?): Double {
        if (p1 == null || p2 == null) return 0.0
        return calculateDistance(p1.position.x, p1.position.y, p2.position.x, p2.position.y)
    }

    /**
     * Calculates the Euclidean distance between two points.
     */
    fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val dx = (x1 - x2).toDouble()
        val dy = (y1 - y2).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Checks if a joint is visible with high confidence (in-frame).
     */
    fun isConfident(landmark: PoseLandmark?, threshold: Float = 0.5f): Boolean {
        return landmark != null && landmark.inFrameLikelihood >= threshold
    }
}

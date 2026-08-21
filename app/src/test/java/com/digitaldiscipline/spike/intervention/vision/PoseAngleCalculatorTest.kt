package com.digitaldiscipline.spike.intervention.vision

import org.junit.Assert.*
import org.junit.Test

class PoseAngleCalculatorTest {

    @Test
    fun testRightAngleCalculation() {
        // 90 degree angle: (0, 100) -> (0, 0) -> (100, 0)
        val angle = PoseAngleCalculator.calculateAngle(
            0f, 100f,
            0f, 0f,
            100f, 0f
        )
        assertEquals(90.0, angle, 0.1)
    }

    @Test
    fun testStraightLineCalculation() {
        // 180 degree angle: (0, -100) -> (0, 0) -> (0, 100)
        val angle = PoseAngleCalculator.calculateAngle(
            0f, -100f,
            0f, 0f,
            0f, 100f
        )
        assertEquals(180.0, angle, 0.1)
    }

    @Test
    fun testAcuteAngleCalculation() {
        // 45 degree angle
        val angle = PoseAngleCalculator.calculateAngle(
            100f, 100f,
            0f, 0f,
            100f, 0f
        )
        assertEquals(45.0, angle, 0.1)
    }

    @Test
    fun testDistanceCalculation() {
        val distance = PoseAngleCalculator.calculateDistance(0f, 0f, 30f, 40f)
        assertEquals(50.0, distance, 0.01)
    }

    @Test
    fun testNullLandmarkReturnsNegativeOne() {
        val angle = PoseAngleCalculator.calculateAngle(null, null, null)
        assertEquals(-1.0, angle, 0.0)
    }
}

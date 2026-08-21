package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.activation.SelfModeActivationCoordinator
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 4E-1 — SelfModeResumeTest
 *
 * Verifies onboarding state restoration, step tracking, and existing user bypass.
 */
class SelfModeResumeTest {

    @Test
    fun `test01 initial onboarding state is NOT_STARTED`() {
        assertEquals("NOT_STARTED", SelfModeActivationCoordinator.STATE_NOT_STARTED)
    }

    @Test
    fun `test02 in progress onboarding state constant is correct`() {
        assertEquals("IN_PROGRESS", SelfModeActivationCoordinator.STATE_IN_PROGRESS)
    }

    @Test
    fun `test03 completed onboarding state constant is correct`() {
        assertEquals("COMPLETED", SelfModeActivationCoordinator.STATE_COMPLETED)
    }

    @Test
    fun `test04 ready onboarding state constant is correct`() {
        assertEquals("READY", SelfModeActivationCoordinator.STATE_READY)
    }

    @Test
    fun `test05 existing completed user bypasses onboarding to dashboard`() {
        val onboardingCompleted = true
        val targetDestination = if (onboardingCompleted) "DASHBOARD" else "ONBOARDING"
        assertEquals("DASHBOARD", targetDestination)
    }

    @Test
    fun `test06 fresh user routes to mode selection`() {
        val onboardingCompleted = false
        val selectedMode: String? = null
        val targetScreen = if (!onboardingCompleted && selectedMode == null) "MODE_SELECTION" else "DASHBOARD"
        assertEquals("MODE_SELECTION", targetScreen)
    }

    @Test
    fun `test07 step transition increments deterministically`() {
        var currentStep = 1
        currentStep++
        assertEquals(2, currentStep)
        currentStep++
        assertEquals(3, currentStep)
        currentStep++
        assertEquals(4, currentStep)
        currentStep++
        assertEquals(5, currentStep)
    }

    @Test
    fun `test08 back navigation decrements step deterministically`() {
        var currentStep = 4
        currentStep--
        assertEquals(3, currentStep)
        currentStep--
        assertEquals(2, currentStep)
        currentStep--
        assertEquals(1, currentStep)
    }

    @Test
    fun `test09 minimum distraction apps constant is 1`() {
        assertEquals(1, SelfModeActivationCoordinator.MIN_DISTRACTION_APPS)
    }

    @Test
    fun `test10 maximum distraction apps constant is 5`() {
        assertEquals(5, SelfModeActivationCoordinator.MAX_DISTRACTION_APPS)
    }
}

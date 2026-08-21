package com.digitaldiscipline.spike.intervention.validation

import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.ValidationCapability
import com.digitaldiscipline.spike.intervention.session.PolicySource
import org.junit.Assert.*
import org.junit.Test

class ValidationMethodSelectorTest {

    @Test
    fun testSelectsCameraWhenPermittedAndPreferred() {
        val pushups = InterventionCatalog.getIntervention("PUSH_UPS")!!
        val context = ValidationSelectionContext(
            policySource = PolicySource.SELF,
            userPreference = UserValidationPreference.PREFER_CAMERA,
            hasCameraPermission = true,
            isCameraHardwareAvailable = true,
            isMotionSensorAvailable = true
        )

        val selected = ValidationMethodSelector.selectValidationCapability(pushups, context)
        assertEquals(ValidationCapability.CAMERA_POSE, selected)
    }

    @Test
    fun testFallsBackToMotionSensorWhenCameraNotPermitted() {
        val pushups = InterventionCatalog.getIntervention("PUSH_UPS")!!
        val context = ValidationSelectionContext(
            policySource = PolicySource.SELF,
            userPreference = UserValidationPreference.ASK_EACH_TIME,
            hasCameraPermission = false,
            isCameraHardwareAvailable = true,
            isMotionSensorAvailable = true
        )

        val selected = ValidationMethodSelector.selectValidationCapability(pushups, context)
        assertEquals(ValidationCapability.DEVICE_MOTION, selected)
    }

    @Test
    fun testNeverCameraPreferenceExcludesCamera() {
        val pushups = InterventionCatalog.getIntervention("PUSH_UPS")!!
        val context = ValidationSelectionContext(
            policySource = PolicySource.SELF,
            userPreference = UserValidationPreference.NEVER_CAMERA,
            hasCameraPermission = true,
            isCameraHardwareAvailable = true,
            isMotionSensorAvailable = true
        )

        val selected = ValidationMethodSelector.selectValidationCapability(pushups, context)
        assertNotEquals(ValidationCapability.CAMERA_POSE, selected)
        assertEquals(ValidationCapability.DEVICE_MOTION, selected)
        assertFalse(ValidationMethodSelector.canOfferCameraValidation(pushups, context))
    }

    @Test
    fun testBreathingAlwaysSelectsTimer() {
        val breathing = InterventionCatalog.getIntervention("BOX_BREATHING")!!
        val context = ValidationSelectionContext(
            policySource = PolicySource.SELF,
            userPreference = UserValidationPreference.PREFER_CAMERA,
            hasCameraPermission = true,
            isCameraHardwareAvailable = true,
            isMotionSensorAvailable = true
        )

        val selected = ValidationMethodSelector.selectValidationCapability(breathing, context)
        assertEquals(ValidationCapability.TIMER, selected)
    }
}

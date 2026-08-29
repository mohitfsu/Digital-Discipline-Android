package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.model.InterventionDifficulty
import com.digitaldiscipline.spike.intervention.model.ValidationType
import org.junit.Assert.*
import org.junit.Test

class InterventionCatalogTest {

    @Test
    fun catalogContainsAll48Interventions() {
        val all = InterventionCatalog.getAllInterventions()
        assertEquals(51, all.size)
    }

    @Test
    fun verifyCategoryDistribution() {
        assertEquals(12, InterventionCatalog.getByCategory(InterventionCategory.MOVEMENT).size)
        assertEquals(1, InterventionCatalog.getByCategory(InterventionCategory.UPPER_BODY).size)
        assertEquals(4, InterventionCatalog.getByCategory(InterventionCategory.BREATHING).size)
        assertEquals(3, InterventionCatalog.getByCategory(InterventionCategory.MEDITATION).size)
        assertEquals(6, InterventionCatalog.getByCategory(InterventionCategory.YOGA_MOBILITY).size)
        assertEquals(5, InterventionCatalog.getByCategory(InterventionCategory.PHYSICAL_RESET).size)
        assertEquals(13, InterventionCatalog.getByCategory(InterventionCategory.COGNITIVE).size)
        assertEquals(4, InterventionCatalog.getByCategory(InterventionCategory.CREATIVE_FLOW).size)
        assertEquals(3, InterventionCatalog.getByCategory(InterventionCategory.MINDFUL_PERSPECTIVE).size)
    }

    @Test
    fun verifyValidationTypeIntegrity_noFalseBiometricClaims() {
        // Breathing and meditation must be TIMER_VALIDATED, never SENSOR_VALIDATED (no false physiology claims)
        val breathing = InterventionCatalog.getByCategory(InterventionCategory.BREATHING)
        breathing.forEach {
            assertEquals(ValidationType.TIMER_VALIDATED, it.validationType)
        }

        val meditation = InterventionCatalog.getByCategory(InterventionCategory.MEDITATION)
        meditation.forEach {
            assertEquals(ValidationType.TIMER_VALIDATED, it.validationType)
        }

        // Pull-ups must be MANUAL_CONFIRMATION (no faking bar detection)
        val pullups = InterventionCatalog.getIntervention("PULL_UPS")
        assertNotNull(pullups)
        assertEquals(ValidationType.MANUAL_CONFIRMATION, pullups?.validationType)
    }

    @Test
    fun verifyDifficultyScaling() {
        val squats = InterventionCatalog.getIntervention("SQUATS")!!
        assertEquals(10, squats.defaultReps)
        assertEquals(5, squats.getRepsForDifficulty(InterventionDifficulty.LIGHT))
        assertEquals(10, squats.getRepsForDifficulty(InterventionDifficulty.STANDARD))
        assertEquals(15, squats.getRepsForDifficulty(InterventionDifficulty.STRONG))

        val plank = InterventionCatalog.getIntervention("PLANK")!!
        assertEquals(30, plank.defaultDurationSeconds)
        assertEquals(18, plank.getDurationForDifficulty(InterventionDifficulty.LIGHT))
        assertEquals(30, plank.getDurationForDifficulty(InterventionDifficulty.STANDARD))
        assertEquals(45, plank.getDurationForDifficulty(InterventionDifficulty.STRONG))
    }

    @Test
    fun verifyCalmCopy_noShameLanguage() {
        val shameWords = listOf("fail", "loss", "loser", "punish", "shame", "bad habit", "ruined")
        InterventionCatalog.getAllInterventions().forEach { item ->
            shameWords.forEach { word ->
                assertFalse(
                    "Intervention ${item.id} contains shame word '$word'",
                    item.calmPrompt.lowercase().contains(word)
                )
            }
        }
    }
}

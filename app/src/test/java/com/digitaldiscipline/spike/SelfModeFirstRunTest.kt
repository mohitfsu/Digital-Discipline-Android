package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.templates.GoalTemplateRepository
import com.digitaldiscipline.spike.behaviour.templates.RewardPreset
import com.digitaldiscipline.spike.data.local.entities.GoalCategory
import com.digitaldiscipline.spike.data.local.entities.UserMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 4E-1 — SelfModeFirstRunTest
 *
 * Verifies template defaults, goal choices, reward mapping, and first-run copy invariants.
 */
class SelfModeFirstRunTest {

    @Test
    fun `test01 goal template repository contains all required starter goals`() {
        val templates = GoalTemplateRepository.getAllTemplates()
        assertTrue(templates.any { it.category == GoalCategory.FITNESS })
        assertTrue(templates.any { it.category == GoalCategory.STUDY })
        assertTrue(templates.any { it.category == GoalCategory.PRODUCTIVITY })
        assertTrue(templates.any { it.category == GoalCategory.MINDFULNESS })
        assertTrue(templates.any { it.category == GoalCategory.READING })
        assertTrue(templates.any { it.category == GoalCategory.SLEEP })
        assertTrue(templates.any { it.category == GoalCategory.CUSTOM })
    }

    @Test
    fun `test02 fitness template defaults to bodyweight squats`() {
        val fitness = GoalTemplateRepository.getAllTemplates().first { it.category == GoalCategory.FITNESS }
        assertEquals("💪", fitness.icon)
        val firstReplacement = fitness.recommendedReplacementBehaviours.first()
        assertTrue(firstReplacement.title.contains("Squats"))
    }

    @Test
    fun `test03 study template defaults to study block`() {
        val study = GoalTemplateRepository.getAllTemplates().first { it.category == GoalCategory.STUDY }
        assertEquals("📚", study.icon)
        val firstReplacement = study.recommendedReplacementBehaviours.first()
        assertTrue(firstReplacement.title.contains("Study"))
    }

    @Test
    fun `test04 mindfulness template defaults to box breathing`() {
        val mindfulness = GoalTemplateRepository.getAllTemplates().first { it.category == GoalCategory.MINDFULNESS }
        assertEquals("🧘", mindfulness.icon)
        val firstReplacement = mindfulness.recommendedReplacementBehaviours.first()
        assertTrue(firstReplacement.title.contains("Breathing"))
    }

    @Test
    fun `test05 distraction app recommendations include common social media and streaming apps`() {
        val apps = GoalTemplateRepository.getAllDistractionRecommendations()
        assertTrue(apps.any { it.displayName == "Instagram" })
        assertTrue(apps.any { it.displayName == "YouTube" })
        assertTrue(apps.any { it.displayName == "Reddit" })
    }

    @Test
    fun `test06 reward presets map to correct duration minutes`() {
        assertEquals(5, RewardPreset.LIGHT.rewardMinutes)
        assertEquals(10, RewardPreset.STANDARD.rewardMinutes)
        assertEquals(10, RewardPreset.STRONG.rewardMinutes)
    }

    @Test
    fun `test07 reward presets have valid positive earning caps`() {
        RewardPreset.values().forEach { preset ->
            assertTrue(preset.rewardSeconds > 0)
            assertTrue(preset.dailyCapSeconds >= preset.rewardSeconds)
            assertTrue(preset.sessionCapSeconds >= preset.rewardSeconds)
        }
    }

    @Test
    fun `test08 user mode enum contains SELF and PARENT modes`() {
        assertEquals("SELF", UserMode.SELF.name)
        assertEquals("PARENT", UserMode.PARENT.name)
    }

    @Test
    fun `test09 default reward preset is STANDARD (10 minutes)`() {
        val template = GoalTemplateRepository.getAllTemplates().first()
        assertEquals(RewardPreset.STANDARD, template.defaultRewardPreset)
        assertEquals(10, template.defaultRewardPreset.rewardMinutes)
    }

    @Test
    fun `test10 all templates have non-empty titles and icons`() {
        GoalTemplateRepository.getAllTemplates().forEach { tmpl ->
            assertFalse(tmpl.name.isBlank())
            assertFalse(tmpl.icon.isBlank())
            assertFalse(tmpl.shortDescription.isBlank())
            assertTrue(tmpl.recommendedReplacementBehaviours.isNotEmpty())
        }
    }
}

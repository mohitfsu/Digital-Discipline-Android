package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.behaviour.activation.SelfModeActivationCoordinator
import com.digitaldiscipline.spike.behaviour.templates.DistractionAppRecommendation
import com.digitaldiscipline.spike.behaviour.templates.GoalTemplateRepository
import com.digitaldiscipline.spike.behaviour.templates.RewardPreset
import com.digitaldiscipline.spike.data.local.entities.GoalCategory
import com.digitaldiscipline.spike.data.local.entities.TriggerCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 4E-1 — SelfModeActivationTest
 *
 * Verifies atomic draft validation, 1-to-5 app limits, and plan creation logic.
 */
class SelfModeActivationTest {

    private val template = GoalTemplateRepository.getAllTemplates().first()
    private val allDistractions = GoalTemplateRepository.getAllDistractionRecommendations()
    private val replacement = template.recommendedReplacementBehaviours.first()
    private val reward = template.defaultRewardPreset

    @Test
    fun `test01 valid selections pass validation`() {
        val result = SelfModeActivationCoordinator.validateSelections(
            template = template,
            distractions = allDistractions.take(2),
            replacement = replacement,
            rewardPreset = reward
        )
        assertTrue(result is SelfModeActivationCoordinator.ValidationResult.Valid)
    }

    @Test
    fun `test02 null template fails validation`() {
        val result = SelfModeActivationCoordinator.validateSelections(
            template = null,
            distractions = allDistractions.take(2),
            replacement = replacement,
            rewardPreset = reward
        )
        assertTrue(result is SelfModeActivationCoordinator.ValidationResult.Invalid)
        assertTrue((result as SelfModeActivationCoordinator.ValidationResult.Invalid).message.contains("goal"))
    }

    @Test
    fun `test03 empty distraction list fails validation`() {
        val result = SelfModeActivationCoordinator.validateSelections(
            template = template,
            distractions = emptyList(),
            replacement = replacement,
            rewardPreset = reward
        )
        assertTrue(result is SelfModeActivationCoordinator.ValidationResult.Invalid)
        assertTrue((result as SelfModeActivationCoordinator.ValidationResult.Invalid).message.contains("at least 1 app"))
    }

    @Test
    fun `test04 selecting 1 distraction app is valid`() {
        val result = SelfModeActivationCoordinator.validateSelections(
            template = template,
            distractions = allDistractions.take(1),
            replacement = replacement,
            rewardPreset = reward
        )
        assertTrue(result is SelfModeActivationCoordinator.ValidationResult.Valid)
    }

    @Test
    fun `test05 selecting 5 distraction apps is valid`() {
        val result = SelfModeActivationCoordinator.validateSelections(
            template = template,
            distractions = allDistractions.take(5),
            replacement = replacement,
            rewardPreset = reward
        )
        assertTrue(result is SelfModeActivationCoordinator.ValidationResult.Valid)
    }

    @Test
    fun `test06 selecting 6 distraction apps fails validation`() {
        val sixApps = (1..6).map {
            DistractionAppRecommendation("com.app$it", "App $it", "📱", TriggerCategory.SOCIAL_MEDIA)
        }
        val result = SelfModeActivationCoordinator.validateSelections(
            template = template,
            distractions = sixApps,
            replacement = replacement,
            rewardPreset = reward
        )
        assertTrue(result is SelfModeActivationCoordinator.ValidationResult.Invalid)
        assertTrue((result as SelfModeActivationCoordinator.ValidationResult.Invalid).message.contains("up to 5"))
    }

    @Test
    fun `test07 null replacement fails validation`() {
        val result = SelfModeActivationCoordinator.validateSelections(
            template = template,
            distractions = allDistractions.take(2),
            replacement = null,
            rewardPreset = reward
        )
        assertTrue(result is SelfModeActivationCoordinator.ValidationResult.Invalid)
    }

    @Test
    fun `test08 null reward preset fails validation`() {
        val result = SelfModeActivationCoordinator.validateSelections(
            template = template,
            distractions = allDistractions.take(2),
            replacement = replacement,
            rewardPreset = null
        )
        assertTrue(result is SelfModeActivationCoordinator.ValidationResult.Invalid)
    }

    @Test
    fun `test09 draft plan creates correct number of trigger and policy entities`() {
        val selected = allDistractions.take(3)
        val draft = SelfModeActivationCoordinator.createDraft(
            template = template,
            selectedDistractions = selected,
            selectedReplacement = replacement,
            rewardPreset = reward
        )
        assertEquals(3, draft.triggerEntities.size)
        assertEquals(3, draft.policyEntities.size)
        assertEquals(template.name, draft.goalEntity.title)
    }

    @Test
    fun `test10 custom goal preserves custom title and description in draft`() {
        val customTemplate = GoalTemplateRepository.getAllTemplates().first { it.category == GoalCategory.CUSTOM }
        val draft = SelfModeActivationCoordinator.createDraft(
            template = customTemplate,
            selectedDistractions = allDistractions.take(2),
            selectedReplacement = replacement,
            rewardPreset = reward,
            customGoalTitle = "Write 500 Words",
            customGoalDescription = "Daily writing practice",
            customDailyTarget = 500,
            customUnit = "words"
        )
        assertEquals("Write 500 Words", draft.goalEntity.title)
        assertEquals("Daily writing practice", draft.goalEntity.description)
        assertEquals(500, draft.goalEntity.dailyTarget)
        assertEquals("words", draft.goalEntity.unit)
    }
}

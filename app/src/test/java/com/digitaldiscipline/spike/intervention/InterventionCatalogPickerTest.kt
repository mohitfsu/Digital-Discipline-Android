package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.intervention.adaptive.*
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.session.PolicySource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InterventionCatalogPickerTest {

    private lateinit var store: InterventionAdaptiveStore
    private lateinit var selector: InterventionSelector

    @Before
    fun setUp() {
        store = InterventionAdaptiveStore()
        selector = InterventionSelector(store)
    }

    @Test
    fun testSelectFiltersByEnabledInterventionIds() {
        val context = InterventionContext(
            triggerId = "trig_1",
            targetPackage = "com.instagram.android",
            timestampMs = 1700000000000L,
            dayOfWeek = 2,
            timeBucket = TimeBucket.AFTERNOON,
            policySource = PolicySource.SELF,
            configuredInterventionId = "BOX_BREATHING",
            recentInterventionIds = emptyList(),
            walletBalanceSeconds = 0
        )

        // Only enable 2 valid cognitive interventions from InterventionCatalog
        val enabledSet = setOf("MATH_CHALLENGE", "MEMORY_SEQUENCE")
        val selection = selector.select(context, enabledSet)

        assertTrue(
            "Selected intervention should be in the enabled set",
            enabledSet.contains(selection.selectedIntervention.id)
        )
    }

    @Test
    fun testSelectCrossCategoryFitnessAndCognitive() {
        val context = InterventionContext(
            triggerId = "trig_1",
            targetPackage = "com.google.android.youtube",
            timestampMs = 1700000000000L,
            dayOfWeek = 3,
            timeBucket = TimeBucket.MORNING,
            policySource = PolicySource.SELF,
            configuredInterventionId = "SQUATS",
            recentInterventionIds = emptyList(),
            walletBalanceSeconds = 0
        )

        // Cross category: Squats (Movement) + Math Challenge (Cognitive)
        val crossCategorySet = setOf("SQUATS", "MATH_CHALLENGE")
        val selection = selector.select(context, crossCategorySet)

        assertTrue(
            "Selected intervention should be one of the cross-category enabled items",
            crossCategorySet.contains(selection.selectedIntervention.id)
        )
    }

    @Test
    fun testFallbackWhenEnabledSetEmptyUsesFullCatalog() {
        val context = InterventionContext(
            triggerId = "trig_1",
            targetPackage = "com.zhiliaoapp.musically",
            timestampMs = 1700000000000L,
            dayOfWeek = 4,
            timeBucket = TimeBucket.EVENING,
            policySource = PolicySource.SELF,
            configuredInterventionId = "BOX_BREATHING",
            recentInterventionIds = emptyList(),
            walletBalanceSeconds = 0
        )

        val selection = selector.select(context, emptySet())
        assertNotNull(selection.selectedIntervention)
    }
}

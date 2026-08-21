package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.notification.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 4D-3 — NotificationScheduler Unit Tests (Pure Logic)
 *
 * These tests verify the tag constants, batch logic, and policy around
 * scheduling without requiring Android context or WorkManager.
 */
class NotificationSchedulerTest {

    @Test
    fun `test01 daily notification worker tag is defined`() {
        assertEquals("smart_notification_daily", NotificationScheduler.DAILY_NOTIFICATION_TAG)
    }

    @Test
    fun `test02 action reminder worker tag is defined`() {
        assertEquals("smart_notification_action", NotificationScheduler.ACTION_REMINDER_TAG)
    }

    @Test
    fun `test03 daily and action reminder tags are distinct`() {
        assertNotEquals(NotificationScheduler.DAILY_NOTIFICATION_TAG, NotificationScheduler.ACTION_REMINDER_TAG)
    }

    @Test
    fun `test04 notification channels are distinct`() {
        val channelIds = setOf(
            NotificationChannelManager.CHANNEL_DAILY,
            NotificationChannelManager.CHANNEL_ACTIONS,
            NotificationChannelManager.CHANNEL_WEEKLY
        )
        assertEquals(3, channelIds.size)
    }

    @Test
    fun `test05 daily channel id is correct`() {
        assertEquals("digital_discipline_daily", NotificationChannelManager.CHANNEL_DAILY)
    }

    @Test
    fun `test06 actions channel id is correct`() {
        assertEquals("digital_discipline_actions", NotificationChannelManager.CHANNEL_ACTIONS)
    }

    @Test
    fun `test07 weekly channel id is correct`() {
        assertEquals("digital_discipline_weekly", NotificationChannelManager.CHANNEL_WEEKLY)
    }

    @Test
    fun `test08 notification types map to correct channels`() {
        assertEquals(NotificationChannelManager.CHANNEL_DAILY,   NotificationType.MORNING_INTENTION.channelId)
        assertEquals(NotificationChannelManager.CHANNEL_ACTIONS, NotificationType.NEXT_ACTION.channelId)
        assertEquals(NotificationChannelManager.CHANNEL_ACTIONS, NotificationType.DISTRACTION_PREEMPTION.channelId)
        assertEquals(NotificationChannelManager.CHANNEL_ACTIONS, NotificationType.MISSED_ACTION.channelId)
        assertEquals(NotificationChannelManager.CHANNEL_DAILY,   NotificationType.SUCCESS.channelId)
        assertEquals(NotificationChannelManager.CHANNEL_DAILY,   NotificationType.EVENING_REFLECTION.channelId)
        assertEquals(NotificationChannelManager.CHANNEL_WEEKLY,  NotificationType.WEEKLY_REVIEW.channelId)
    }

    @Test
    fun `test09 notification preferences default to BALANCED`() {
        val prefs = NotificationPreferences()
        assertEquals(NotificationFrequencyMode.BALANCED, prefs.frequencyMode)
    }

    @Test
    fun `test10 all notification categories default to enabled`() {
        val prefs = NotificationPreferences()
        assertTrue(prefs.enableDailyFocus)
        assertTrue(prefs.enableActionReminders)
        assertTrue(prefs.enableDistractionWindow)
        assertTrue(prefs.enableSuccess)
        assertTrue(prefs.enableEveningReflection)
        assertTrue(prefs.enableWeeklyReview)
    }

    @Test
    fun `test11 deep link TODAY is parsed correctly`() {
        val result = com.digitaldiscipline.spike.ui.NotificationDeepLink.parse("digitaldiscipline://today")
        assertTrue(result is com.digitaldiscipline.spike.ui.NotificationDeepLink.Today)
    }

    @Test
    fun `test12 deep link WEEKLY REVIEW is parsed correctly`() {
        val result = com.digitaldiscipline.spike.ui.NotificationDeepLink.parse("digitaldiscipline://weekly-review")
        assertTrue(result is com.digitaldiscipline.spike.ui.NotificationDeepLink.WeeklyReview)
    }

    @Test
    fun `test13 deep link ACTION with valid id is parsed correctly`() {
        val result = com.digitaldiscipline.spike.ui.NotificationDeepLink.parse("digitaldiscipline://action/action_squats_10")
        assertTrue(result is com.digitaldiscipline.spike.ui.NotificationDeepLink.Action)
        assertEquals("action_squats_10", (result as com.digitaldiscipline.spike.ui.NotificationDeepLink.Action).actionId)
    }

    @Test
    fun `test14 deep link ACTION with empty id returns Unknown`() {
        val result = com.digitaldiscipline.spike.ui.NotificationDeepLink.parse("digitaldiscipline://action/")
        assertTrue(result is com.digitaldiscipline.spike.ui.NotificationDeepLink.Unknown)
    }

    @Test
    fun `test15 deep link null returns Unknown`() {
        val result = com.digitaldiscipline.spike.ui.NotificationDeepLink.parse(null)
        assertTrue(result is com.digitaldiscipline.spike.ui.NotificationDeepLink.Unknown)
    }

    @Test
    fun `test16 deep link blank returns Unknown`() {
        val result = com.digitaldiscipline.spike.ui.NotificationDeepLink.parse("   ")
        assertTrue(result is com.digitaldiscipline.spike.ui.NotificationDeepLink.Unknown)
    }

    @Test
    fun `test17 unknown scheme returns Unknown`() {
        val result = com.digitaldiscipline.spike.ui.NotificationDeepLink.parse("https://example.com/today")
        assertTrue(result is com.digitaldiscipline.spike.ui.NotificationDeepLink.Unknown)
    }
}

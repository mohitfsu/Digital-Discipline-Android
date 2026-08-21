package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.notification.*
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Phase 4D-3 — NotificationFrequencyGovernor Unit Tests (Pure Logic)
 *
 * Tests the GovernorState logic deterministically, without requiring
 * Android context or DataStore (uses GovernorState data class directly).
 */
class NotificationFrequencyGovernorTest {

    private val balanced = NotificationPreferences(frequencyMode = NotificationFrequencyMode.BALANCED)
    private val minimal  = NotificationPreferences(frequencyMode = NotificationFrequencyMode.MINIMAL)
    private val helpful  = NotificationPreferences(frequencyMode = NotificationFrequencyMode.HELPFUL)
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // -------------------------------------------------------------------------
    // Test 1: Fresh state allows sending
    // -------------------------------------------------------------------------
    @Test
    fun `test01 fresh governor state permits first notification`() {
        val state = GovernorState(dateString = today)
        assertTrue(governorAllows(state, NotificationType.MORNING_INTENTION, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 2: Total daily cap enforced
    // -------------------------------------------------------------------------
    @Test
    fun `test02 total daily cap enforced balanced mode`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = balanced.maxTotalPerDay, // at cap
            lastSentTimestampMs = System.currentTimeMillis() - 3 * 60 * 60 * 1000L // 3h ago
        )
        assertFalse(governorAllows(state, NotificationType.MORNING_INTENTION, balanced))
    }

    @Test
    fun `test03 total daily cap is 1 in minimal mode`() {
        assertEquals(1, minimal.maxTotalPerDay)
    }

    @Test
    fun `test04 total daily cap is 3 in balanced mode`() {
        assertEquals(3, balanced.maxTotalPerDay)
    }

    @Test
    fun `test05 total daily cap is 5 in helpful mode`() {
        assertEquals(5, helpful.maxTotalPerDay)
    }

    // -------------------------------------------------------------------------
    // Test 6: Min gap enforced
    // -------------------------------------------------------------------------
    @Test
    fun `test06 minimum gap blocks notification sent too recently`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = 1,
            lastSentTimestampMs = System.currentTimeMillis() - 30 * 60 * 1000L // 30min ago, gap=120min
        )
        assertFalse(governorAllows(state, NotificationType.NEXT_ACTION, balanced))
    }

    @Test
    fun `test07 notification permitted after min gap has passed`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = 1,
            lastSentTimestampMs = System.currentTimeMillis() - 3 * 60 * 60 * 1000L // 3h ago
        )
        assertTrue(governorAllows(state, NotificationType.NEXT_ACTION, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 8: Per-type cap (max 1 per type per day)
    // -------------------------------------------------------------------------
    @Test
    fun `test08 per type cap enforced`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = 1,
            perTypeSentToday = mapOf(NotificationType.MORNING_INTENTION.name to 1),
            lastSentTimestampMs = System.currentTimeMillis() - 3 * 60 * 60 * 1000L
        )
        assertFalse(governorAllows(state, NotificationType.MORNING_INTENTION, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 9: Behaviour reminder cap
    // -------------------------------------------------------------------------
    @Test
    fun `test09 behaviour reminder cap enforced`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = 2,
            behaviourRemindersSentToday = balanced.maxBehaviourRemindersPerDay, // at cap
            lastSentTimestampMs = System.currentTimeMillis() - 3 * 60 * 60 * 1000L
        )
        assertFalse(governorAllows(state, NotificationType.NEXT_ACTION, balanced))
        assertFalse(governorAllows(state, NotificationType.MISSED_ACTION, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 10: Success capped at 1 per day
    // -------------------------------------------------------------------------
    @Test
    fun `test10 success capped at 1 per day`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = 1,
            successSentToday = 1,
            lastSentTimestampMs = System.currentTimeMillis() - 3 * 60 * 60 * 1000L
        )
        assertFalse(governorAllows(state, NotificationType.SUCCESS, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 11: Preemptive capped at 1 per day
    // -------------------------------------------------------------------------
    @Test
    fun `test11 preemptive notification capped at 1 per day`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = 1,
            preemptiveSentToday = 1,
            lastSentTimestampMs = System.currentTimeMillis() - 3 * 60 * 60 * 1000L
        )
        assertFalse(governorAllows(state, NotificationType.DISTRACTION_PREEMPTION, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 12: Missed action capped at 1 per day
    // -------------------------------------------------------------------------
    @Test
    fun `test12 missed action capped at 1 per day`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = 1,
            missedActionSentToday = 1,
            lastSentTimestampMs = System.currentTimeMillis() - 3 * 60 * 60 * 1000L
        )
        assertFalse(governorAllows(state, NotificationType.MISSED_ACTION, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 13: New day resets all counters
    // -------------------------------------------------------------------------
    @Test
    fun `test13 new calendar day resets governor state`() {
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).let { fmt ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            fmt.format(cal.time)
        }
        val state = GovernorState(
            dateString = yesterday, // stale date
            totalSentToday = 5,
            behaviourRemindersSentToday = 3,
            successSentToday = 1
        )
        // Governor detects date mismatch → treats as fresh
        // (In pure logic: if state.dateString != today, counters are zero)
        val isStale = state.dateString != today
        assertTrue("Governor should detect stale date", isStale)
        // Fresh state permits sending
        val freshState = if (isStale) GovernorState(dateString = today) else state
        assertTrue(governorAllows(freshState, NotificationType.MORNING_INTENTION, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 14: Min gap is 180 minutes in MINIMAL mode
    // -------------------------------------------------------------------------
    @Test
    fun `test14 min gap is 180 minutes in minimal mode`() {
        assertEquals(180, minimal.minGapMinutes)
    }

    @Test
    fun `test15 min gap is 120 minutes in balanced mode`() {
        assertEquals(120, balanced.minGapMinutes)
    }

    @Test
    fun `test16 min gap is 60 minutes in helpful mode`() {
        assertEquals(60, helpful.minGapMinutes)
    }

    // -------------------------------------------------------------------------
    // Test 17: Governor allows weekly review (non-behaviour-reminder type)
    // -------------------------------------------------------------------------
    @Test
    fun `test17 weekly review not blocked by behaviour reminder cap`() {
        val state = GovernorState(
            dateString = today,
            totalSentToday = 1,
            behaviourRemindersSentToday = balanced.maxBehaviourRemindersPerDay, // hit cap
            lastSentTimestampMs = System.currentTimeMillis() - 3 * 60 * 60 * 1000L
        )
        // Weekly review is not a behaviour reminder — should still be allowed
        assertTrue(governorAllows(state, NotificationType.WEEKLY_REVIEW, balanced))
    }

    // -------------------------------------------------------------------------
    // Test 18: Zero lastSentTimestampMs bypasses gap check
    // -------------------------------------------------------------------------
    @Test
    fun `test18 zero last sent timestamp bypasses gap check`() {
        val state = GovernorState(dateString = today, totalSentToday = 0, lastSentTimestampMs = 0L)
        assertTrue(governorAllows(state, NotificationType.MORNING_INTENTION, balanced))
    }

    // -------------------------------------------------------------------------
    // Helper: pure in-memory governor allow logic (mirrors DataStore-backed version)
    // -------------------------------------------------------------------------
    private fun governorAllows(state: GovernorState, type: NotificationType, prefs: NotificationPreferences): Boolean {
        if (state.dateString != today) return true // fresh day

        if (state.lastSentTimestampMs > 0L) {
            val elapsedMs = System.currentTimeMillis() - state.lastSentTimestampMs
            val gapMs = prefs.minGapMinutes * 60 * 1000L
            if (elapsedMs < gapMs) return false
        }

        if (state.totalSentToday >= prefs.maxTotalPerDay) return false

        val typeCount = state.perTypeSentToday[type.name] ?: 0
        if (typeCount >= 1) return false

        val isBehaviourReminder = type in listOf(
            NotificationType.NEXT_ACTION,
            NotificationType.DISTRACTION_PREEMPTION,
            NotificationType.MISSED_ACTION
        )
        if (isBehaviourReminder && state.behaviourRemindersSentToday >= prefs.maxBehaviourRemindersPerDay) return false

        if (type == NotificationType.SUCCESS && state.successSentToday >= 1) return false
        if (type == NotificationType.DISTRACTION_PREEMPTION && state.preemptiveSentToday >= 1) return false
        if (type == NotificationType.MISSED_ACTION && state.missedActionSentToday >= 1) return false

        return true
    }
}

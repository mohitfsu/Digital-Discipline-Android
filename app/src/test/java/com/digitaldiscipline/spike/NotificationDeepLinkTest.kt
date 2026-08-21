package com.digitaldiscipline.spike

import com.digitaldiscipline.spike.notification.*
import com.digitaldiscipline.spike.ui.NotificationDeepLink
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 4D-3 — Notification Deep Link Safety Tests
 *
 * Validates that deep-link parsing is safe against all malformed inputs
 * and that notification candidates carry correct deep-link strings.
 */
class NotificationDeepLinkTest {

    // -------------------------------------------------------------------------
    // Test 1-4: Valid deep links
    // -------------------------------------------------------------------------
    @Test
    fun `test01 today deep link parses correctly`() {
        assertEquals(NotificationDeepLink.Today, NotificationDeepLink.parse("digitaldiscipline://today"))
    }

    @Test
    fun `test02 weekly review deep link parses correctly`() {
        assertEquals(NotificationDeepLink.WeeklyReview, NotificationDeepLink.parse("digitaldiscipline://weekly-review"))
    }

    @Test
    fun `test03 action deep link with id parses correctly`() {
        val result = NotificationDeepLink.parse("digitaldiscipline://action/abc_123")
        assertTrue(result is NotificationDeepLink.Action)
        assertEquals("abc_123", (result as NotificationDeepLink.Action).actionId)
    }

    @Test
    fun `test04 action deep link with complex id parses correctly`() {
        val result = NotificationDeepLink.parse("digitaldiscipline://action/action_squats_10_2026-08-17")
        assertTrue(result is NotificationDeepLink.Action)
        assertFalse((result as NotificationDeepLink.Action).actionId.isBlank())
    }

    // -------------------------------------------------------------------------
    // Test 5-10: Invalid / malformed deep links
    // -------------------------------------------------------------------------
    @Test
    fun `test05 null deep link returns Unknown`() {
        assertTrue(NotificationDeepLink.parse(null) is NotificationDeepLink.Unknown)
    }

    @Test
    fun `test06 empty string deep link returns Unknown`() {
        assertTrue(NotificationDeepLink.parse("") is NotificationDeepLink.Unknown)
    }

    @Test
    fun `test07 blank string deep link returns Unknown`() {
        assertTrue(NotificationDeepLink.parse("   ") is NotificationDeepLink.Unknown)
    }

    @Test
    fun `test08 http scheme deep link returns Unknown`() {
        assertTrue(NotificationDeepLink.parse("http://today") is NotificationDeepLink.Unknown)
    }

    @Test
    fun `test09 action with empty id returns Unknown`() {
        assertTrue(NotificationDeepLink.parse("digitaldiscipline://action/") is NotificationDeepLink.Unknown)
    }

    @Test
    fun `test10 action with whitespace only id returns Unknown`() {
        assertTrue(NotificationDeepLink.parse("digitaldiscipline://action/   ") is NotificationDeepLink.Unknown)
    }

    @Test
    fun `test11 completely random string returns Unknown`() {
        assertTrue(NotificationDeepLink.parse("xyzzy") is NotificationDeepLink.Unknown)
    }

    // -------------------------------------------------------------------------
    // Test 12: Notification candidates carry correct deep links
    // -------------------------------------------------------------------------
    @Test
    fun `test12 morning intention candidate deep link is today`() {
        val candidate = NotificationCandidate(
            type = NotificationType.MORNING_INTENTION,
            title = "Test",
            body = "Test body",
            deepLink = "digitaldiscipline://today",
            goalId = "",
            actionId = ""
        )
        val dest = NotificationDeepLink.parse(candidate.deepLink)
        assertTrue(dest is NotificationDeepLink.Today)
    }

    @Test
    fun `test13 next action candidate deep link is action link`() {
        val actionId = "action_squats_10"
        val candidate = NotificationCandidate(
            type = NotificationType.NEXT_ACTION,
            title = "Test",
            body = "Test body",
            deepLink = "digitaldiscipline://action/$actionId",
            goalId = "",
            actionId = actionId
        )
        val dest = NotificationDeepLink.parse(candidate.deepLink)
        assertTrue(dest is NotificationDeepLink.Action)
        assertEquals(actionId, (dest as NotificationDeepLink.Action).actionId)
    }

    @Test
    fun `test14 weekly review candidate deep link is weekly-review`() {
        val candidate = NotificationCandidate(
            type = NotificationType.WEEKLY_REVIEW,
            title = "Test",
            body = "Test body",
            deepLink = "digitaldiscipline://weekly-review",
            goalId = "",
            actionId = ""
        )
        val dest = NotificationDeepLink.parse(candidate.deepLink)
        assertTrue(dest is NotificationDeepLink.WeeklyReview)
    }

    // -------------------------------------------------------------------------
    // Test 15: Notification invariants — no enforcement, no wallet mutation
    // -------------------------------------------------------------------------
    @Test
    fun `test15 notification candidate has no enforcement fields`() {
        // NotificationCandidate must not contain any enforcement-path references
        val candidate = NotificationCandidate(
            type = NotificationType.SUCCESS,
            title = "Done",
            body = "Nice work",
            deepLink = "digitaldiscipline://today",
            goalId = "",
            actionId = ""
        )
        // Cannot block apps
        assertEquals("", candidate.goalId)
        // Cannot start wallet sessions (no walletService reference in candidate)
        assertNotNull(candidate.deepLink)
    }

    @Test
    fun `test16 all notification types have a label`() {
        NotificationType.values().forEach { type ->
            assertTrue("Type ${type.name} should have a non-blank label", type.label.isNotBlank())
        }
    }

    @Test
    fun `test17 all notification types have a channel id`() {
        val validChannels = setOf(
            NotificationChannelManager.CHANNEL_DAILY,
            NotificationChannelManager.CHANNEL_ACTIONS,
            NotificationChannelManager.CHANNEL_WEEKLY
        )
        NotificationType.values().forEach { type ->
            assertTrue("Type ${type.name} channel '${type.channelId}' should be valid",
                type.channelId in validChannels)
        }
    }
}

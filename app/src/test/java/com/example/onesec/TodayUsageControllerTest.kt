package com.example.onesec

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayUsageControllerTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-08-21T04:00:00Z")
    private val rule = RestrictedAppRule(
        packageName = "com.example.video",
        displayName = "短视频",
        level = RestrictionLevel.HARD,
        dailyAllowance = DailyAllowance.ofMinutes(30),
    )

    @Test
    fun `only completed foreground intervals count toward today's usage`() {
        val controller = controllerWith(
            events = listOf(
                event("2026-08-21T01:00:00Z", UsageEventType.FOREGROUND),
                event("2026-08-21T01:12:30Z", UsageEventType.BACKGROUND),
                event("2026-08-21T02:00:00Z", UsageEventType.FOREGROUND),
                event("2026-08-21T02:02:01Z", UsageEventType.BACKGROUND),
            ),
        )

        controller.refresh()

        assertEquals(15, controller.state.apps.single().usedMinutes)
        assertEquals(15, controller.state.apps.single().remainingMinutes)
    }

    @Test
    fun `foreground usage stops while the phone is locked`() {
        val controller = controllerWith(
            events = listOf(
                event("2026-08-21T01:00:00Z", UsageEventType.FOREGROUND),
                UsageEvent(null, Instant.parse("2026-08-21T01:05:00Z"), UsageEventType.SCREEN_LOCKED),
                event("2026-08-21T01:20:00Z", UsageEventType.BACKGROUND),
            ),
        )

        controller.refresh()

        assertEquals(5, controller.state.apps.single().usedMinutes)
    }

    @Test
    fun `unlock resumes counting an activity that remains in the foreground`() {
        val controller = controllerWith(
            events = listOf(
                event("2026-08-21T01:00:00Z", UsageEventType.FOREGROUND),
                UsageEvent(null, Instant.parse("2026-08-21T01:05:00Z"), UsageEventType.SCREEN_LOCKED),
                UsageEvent(null, Instant.parse("2026-08-21T01:20:00Z"), UsageEventType.SCREEN_UNLOCKED),
                event("2026-08-21T01:24:00Z", UsageEventType.BACKGROUND),
            ),
        )

        controller.refresh()

        assertEquals(9, controller.state.apps.single().usedMinutes)
    }

    @Test
    fun `moving between activities in the same app keeps the app foreground`() {
        val controller = controllerWith(
            events = listOf(
                event("2026-08-21T01:00:00Z", UsageEventType.FOREGROUND, activityId = "feed"),
                event("2026-08-21T01:04:01Z", UsageEventType.BACKGROUND, activityId = "feed"),
                event("2026-08-21T01:04:02Z", UsageEventType.FOREGROUND, activityId = "details"),
                event("2026-08-21T01:10:00Z", UsageEventType.BACKGROUND, activityId = "details"),
            ),
        )

        controller.refresh()

        assertEquals(10, controller.state.apps.single().usedMinutes)
    }

    @Test
    fun `an app still in the foreground counts through the controlled current time`() {
        val controller = controllerWith(
            events = listOf(event("2026-08-21T03:54:01Z", UsageEventType.FOREGROUND)),
        )

        controller.refresh()

        assertEquals(6, controller.state.apps.single().usedMinutes)
        assertEquals(24, controller.state.apps.single().remainingMinutes)
    }

    @Test
    fun `zero usage preserves the full allowance`() {
        val controller = controllerWith(events = emptyList())

        controller.refresh()

        assertEquals(0, controller.state.apps.single().usedMinutes)
        assertEquals(30, controller.state.apps.single().remainingMinutes)
    }

    @Test
    fun `partial used minutes round up and the allowance boundary never goes negative`() {
        val controller = controllerWith(
            events = listOf(
                event("2026-08-21T01:00:00Z", UsageEventType.FOREGROUND),
                event("2026-08-21T01:29:00.001Z", UsageEventType.BACKGROUND),
            ),
        )

        controller.refresh()

        assertEquals(30, controller.state.apps.single().usedMinutes)
        assertEquals(0, controller.state.apps.single().remainingMinutes)
    }

    @Test
    fun `usage crossing local midnight counts only the new local day`() {
        val controller = controllerWith(
            events = listOf(
                event("2026-08-20T15:58:00Z", UsageEventType.FOREGROUND),
                event("2026-08-20T16:03:00Z", UsageEventType.BACKGROUND),
            ),
        )

        controller.refresh()

        assertEquals(3, controller.state.apps.single().usedMinutes)
    }

    @Test
    fun `foreground state older than yesterday is still carried across local midnight`() {
        val controller = controllerWith(
            events = listOf(
                event("2026-08-19T12:00:00Z", UsageEventType.FOREGROUND),
                event("2026-08-20T16:03:00Z", UsageEventType.BACKGROUND),
            ),
        )

        controller.refresh()

        assertEquals(3, controller.state.apps.single().usedMinutes)
    }

    @Test
    fun `revoked usage access marks protection failed and hides usage values`() {
        val controller = controllerWith(
            events = listOf(event("2026-08-21T01:00:00Z", UsageEventType.FOREGROUND)),
            usageAccessGranted = false,
        )

        controller.refresh()

        assertFalse(controller.state.protectionAvailable)
        assertTrue(controller.state.apps.isEmpty())
    }

    private fun controllerWith(
        events: List<UsageEvent>,
        usageAccessGranted: Boolean = true,
    ) = TodayUsageController(
        ruleStore = TestRuleStore(listOf(rule)),
        permissionGateway = TestPermissionGateway(usageAccessGranted),
        usageEvents = TestUsageEventSource(events),
        clock = Clock.fixed(now, zone),
    )

    private fun event(
        instant: String,
        type: UsageEventType,
        activityId: String = "default",
    ) = UsageEvent(
        packageName = rule.packageName,
        timestamp = Instant.parse(instant),
        type = type,
        activityId = activityId,
    )
}

private class TestRuleStore(
    private val rules: List<RestrictedAppRule>,
) : RestrictionRuleStore {
    override fun loadRules(): List<RestrictedAppRule> = rules
    override fun saveRule(rule: RestrictedAppRule) = Unit
}

private class TestPermissionGateway(
    private val usageAccessGranted: Boolean,
) : PermissionGateway {
    override fun readPermissions() = PermissionSnapshot(
        usageAccessGranted = usageAccessGranted,
        accessibilityGranted = true,
    )

    override fun openUsageAccessSettings() = Unit
    override fun openAccessibilitySettings() = Unit
}

private class TestUsageEventSource(
    private val events: List<UsageEvent>,
) : UsageEventSource {
    override fun eventsBetween(start: Instant, end: Instant): List<UsageEvent> =
        events.filter { it.timestamp >= start && it.timestamp <= end }
}

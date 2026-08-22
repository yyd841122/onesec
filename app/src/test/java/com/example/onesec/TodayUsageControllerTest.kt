package com.example.onesec

import java.time.Clock
import java.time.Duration
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
    fun `precise duration remains available independently of rounded display minutes`() {
        val events = listOf(
            event("2026-08-21T01:00:00Z", UsageEventType.FOREGROUND),
            event("2026-08-21T01:04:01Z", UsageEventType.BACKGROUND),
        )

        assertEquals(Duration.ofMinutes(4).plusSeconds(1), usedTodayDuration(rule.packageName, events, now, zone))
        assertEquals(5, usedTodayMinutes(rule.packageName, events, now, zone))
    }

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

    @Test
    fun `overview aggregates total usage rules pending changes and today's activity`() {
        val pending = PendingRelaxation.RemoveRule(
            InstalledApp(rule.packageName, rule.displayName),
            java.time.LocalDate.of(2026, 8, 22),
        )
        val controller = TodayUsageController(
            ruleStore = TestPolicyStore(
                listOf(rule),
                listOf(pending, PendingRelaxation.DisableProtection(java.time.LocalDate.of(2026, 8, 22))),
            ),
            permissionGateway = TestPermissionGateway(true),
            usageEvents = TestUsageEventSource(
                listOf(event("2026-08-21T01:00:00Z", UsageEventType.FOREGROUND), event("2026-08-21T01:12:00Z", UsageEventType.BACKGROUND)),
            ),
            historyStore = TestHistoryStore(TodayHistory(3, emergencyOverrideUsed = true)),
            clock = Clock.fixed(now, zone),
        )

        controller.refresh()

        assertEquals(12, controller.state.totalUsedMinutes)
        assertEquals(RestrictionLevel.HARD, controller.state.apps.single().level)
        assertEquals(pending, controller.state.apps.single().pendingRelaxation)
        assertEquals(3, controller.state.interventionCount)
        assertTrue(controller.state.emergencyOverrideUsed)
        assertEquals(
            java.time.LocalDate.of(2026, 8, 22),
            controller.state.globalPendingRelaxation?.effectiveDate,
        )
    }

    @Test
    fun `clear all local data returns the overview to its real initial state`() {
        var cleared = false
        var rules = listOf(rule)
        val controller = TodayUsageController(
            ruleStore = object : RestrictionRuleStore {
                override fun loadRules() = rules
                override fun saveRule(rule: RestrictedAppRule) = Unit
            },
            permissionGateway = TestPermissionGateway(true),
            usageEvents = TestUsageEventSource(emptyList()),
            clock = Clock.fixed(now, zone),
            localDataClearer = LocalDataClearer {
                cleared = true
                rules = emptyList()
            },
        )

        controller.clearAllLocalData()

        assertTrue(cleared)
        assertTrue(controller.state.apps.isEmpty())
        assertEquals(0, controller.state.totalUsedMinutes)
    }

    @Test
    fun `clear re-reads protection status instead of assuming permissions`() {
        val controller = TodayUsageController(
            ruleStore = TestRuleStore(emptyList()),
            permissionGateway = TestPermissionGateway(false),
            usageEvents = TestUsageEventSource(emptyList()),
            clock = Clock.fixed(now, zone),
            localDataClearer = LocalDataClearer {},
        )

        controller.clearAllLocalData()

        assertFalse(controller.state.protectionAvailable)
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

private class TestPolicyStore(
    private val rules: List<RestrictedAppRule>,
    private val pending: List<PendingRelaxation>,
) : RestrictionPolicyStore {
    override fun loadRules() = rules
    override fun saveRule(rule: RestrictedAppRule) = Unit
    override fun loadPendingRelaxations() = pending
    override fun schedulePendingRelaxation(pendingRelaxation: PendingRelaxation) = Unit
    override fun cancelPendingRelaxation(packageName: String) = Unit
    override fun removeRule(packageName: String) = Unit
    override fun isProtectionEnabled() = true
    override fun disableProtection() = Unit
}

private class TestHistoryStore(private val history: TodayHistory) : LocalHistoryStore {
    override fun recordUsage(packageName: String, localDate: java.time.LocalDate, usedMinutes: Int) = Unit
    override fun recordIntervention(packageName: String, at: Instant) = Unit
    override fun recordEmergencyOverride(record: EmergencyOverrideRecord) = Unit
    override fun today(onDate: java.time.LocalDate) = history
    override fun pruneBefore(cutoff: java.time.LocalDate) = Unit
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

package com.example.onesec

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppMonitorTest {
    private val now = Instant.parse("2026-08-21T04:00:00Z")
    private val rule = RestrictedAppRule(
        packageName = "com.example.video",
        displayName = "短视频",
        level = RestrictionLevel.HARD,
        dailyAllowance = DailyAllowance.ofMinutes(30),
    )

    @Test
    fun `foreground restricted app at its allowance presents an intervention`() {
        val presenter = RecordingInterventionPresenter()
        val monitor = ForegroundAppMonitor(
            ruleStore = MonitorRuleStore(rule),
            usageLookup = FixedTodayUsageLookup(30),
            protectionStatus = { true },
            exhaustedAllowances = MemoryExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = presenter,
            clock = Clock.fixed(now, ZoneId.of("Asia/Shanghai")),
        )

        val decision = monitor.onAppEnteredForeground(rule.packageName)

        assertTrue(decision is ProtectionDecision.Intervene)
        assertEquals(decision, presenter.presented.single())
    }

    @Test
    fun `foreground unrestricted or under-allowance app adds no friction`() {
        val presenter = RecordingInterventionPresenter()
        val monitor = ForegroundAppMonitor(
            ruleStore = MonitorRuleStore(rule),
            usageLookup = FixedTodayUsageLookup(29),
            protectionStatus = { true },
            exhaustedAllowances = MemoryExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = presenter,
            clock = Clock.fixed(now, ZoneId.of("Asia/Shanghai")),
        )

        assertEquals(ProtectionDecision.Allow, monitor.onAppEnteredForeground(rule.packageName))
        assertEquals(ProtectionDecision.Allow, monitor.onAppEnteredForeground("com.example.other"))
        assertTrue(presenter.presented.isEmpty())
    }

    @Test
    fun `an exhausted hard restriction stays active when usage events temporarily report less`() {
        val presenter = RecordingInterventionPresenter()
        val monitor = ForegroundAppMonitor(
            ruleStore = MonitorRuleStore(rule),
            usageLookup = SequenceTodayUsageLookup(30, 29),
            protectionStatus = { true },
            exhaustedAllowances = MemoryExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = presenter,
            clock = Clock.fixed(now, ZoneId.of("Asia/Shanghai")),
        )

        val firstDecision = monitor.onAppEnteredForeground(rule.packageName)
        val secondDecision = monitor.onAppEnteredForeground(rule.packageName)

        assertTrue(firstDecision is ProtectionDecision.Intervene)
        assertTrue(secondDecision is ProtectionDecision.Intervene)
        assertEquals(2, presenter.presented.size)
    }

    @Test
    fun `continued use is reevaluated exactly when the daily allowance is exhausted`() {
        val presenter = RecordingInterventionPresenter()
        val scheduler = RecordingExpiryScheduler()
        val monitor = ForegroundAppMonitor(
            ruleStore = MonitorRuleStore(rule.copy(dailyAllowance = DailyAllowance.ofMinutes(5))),
            usageLookup = SequenceTodayUsageLookup(
                java.time.Duration.ofMinutes(4).plusSeconds(1),
                java.time.Duration.ofMinutes(5),
            ),
            protectionStatus = { true },
            exhaustedAllowances = MemoryExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = presenter,
            clock = Clock.fixed(now, ZoneId.of("Asia/Shanghai")),
            expiryScheduler = scheduler,
        )

        assertEquals(ProtectionDecision.Allow, monitor.onAppEnteredForeground(rule.packageName))
        assertEquals(now.plusSeconds(59), scheduler.endsAt)
        scheduler.expire()

        assertEquals(RestrictionLevel.HARD, presenter.presented.single().level)
    }

    @Test
    fun `a transient external window does not cancel the foreground app allowance deadline`() {
        val scheduler = RecordingExpiryScheduler()
        var foregroundPackage = rule.packageName
        val monitor = ForegroundAppMonitor(
            ruleStore = MonitorRuleStore(rule),
            usageLookup = FixedTodayUsageLookup(4),
            protectionStatus = { true },
            exhaustedAllowances = MemoryExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = RecordingInterventionPresenter(),
            clock = Clock.fixed(now, ZoneId.of("Asia/Shanghai")),
            expiryScheduler = scheduler,
            foregroundPackageLookup = { foregroundPackage },
        )

        monitor.onAppEnteredForeground(rule.packageName)
        foregroundPackage = "com.example.advertising"
        monitor.onAppEnteredForeground(foregroundPackage)

        assertEquals(null, scheduler.cancelledPackageName)
    }

    @Test
    fun `allowance deadline does not intervene after the target app actually leaves foreground`() {
        val scheduler = RecordingExpiryScheduler()
        val presenter = RecordingInterventionPresenter()
        var foregroundPackage = rule.packageName
        val monitor = ForegroundAppMonitor(
            ruleStore = MonitorRuleStore(rule.copy(dailyAllowance = DailyAllowance.ofMinutes(5))),
            usageLookup = SequenceTodayUsageLookup(4, 5),
            protectionStatus = { true },
            exhaustedAllowances = MemoryExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = presenter,
            clock = Clock.fixed(now, ZoneId.of("Asia/Shanghai")),
            expiryScheduler = scheduler,
            foregroundPackageLookup = { foregroundPackage },
        )

        monitor.onAppEnteredForeground(rule.packageName)
        foregroundPackage = "com.example.other"
        scheduler.expire()

        assertTrue(presenter.presented.isEmpty())
    }

    @Test
    fun `continued soft restriction use is reevaluated when its access window expires`() {
        val softRule = rule.copy(level = RestrictionLevel.SOFT, dailyAllowance = DailyAllowance.ofMinutes(60))
        val presenter = RecordingInterventionPresenter()
        val windows = MutableAccessWindowStore(now.plusSeconds(300))
        val scheduler = RecordingExpiryScheduler()
        val monitor = ForegroundAppMonitor(
            ruleStore = MonitorRuleStore(softRule),
            usageLookup = FixedTodayUsageLookup(60),
            protectionStatus = { true },
            exhaustedAllowances = MemoryExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = presenter,
            clock = Clock.fixed(now, ZoneId.of("Asia/Shanghai")),
            accessWindows = windows,
            expiryScheduler = scheduler,
        )

        assertEquals(ProtectionDecision.Allow, monitor.onAppEnteredForeground(softRule.packageName))
        windows.endsAt = now
        scheduler.expire()

        assertEquals(RestrictionLevel.SOFT, presenter.presented.single().level)
    }

    @Test
    fun `leaving a soft restricted app makes its pending expiry harmless`() {
        val softRule = rule.copy(level = RestrictionLevel.SOFT, dailyAllowance = DailyAllowance.ofMinutes(60))
        val scheduler = RecordingExpiryScheduler()
        val presenter = RecordingInterventionPresenter()
        var foregroundPackage = softRule.packageName
        val monitor = ForegroundAppMonitor(
            ruleStore = MonitorRuleStore(softRule),
            usageLookup = FixedTodayUsageLookup(60),
            protectionStatus = { true },
            exhaustedAllowances = MemoryExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = presenter,
            clock = Clock.fixed(now, ZoneId.of("Asia/Shanghai")),
            accessWindows = MutableAccessWindowStore(now.plusSeconds(300)),
            expiryScheduler = scheduler,
            foregroundPackageLookup = { foregroundPackage },
        )

        monitor.onAppEnteredForeground(softRule.packageName)
        foregroundPackage = "com.example.other"
        monitor.onAppEnteredForeground(foregroundPackage)
        scheduler.expire()

        assertTrue(presenter.presented.isEmpty())
    }

    @Test
    fun `latest usage event restores the currently foreground app after reconstruction`() {
        val events = listOf(
            UsageEvent("com.example.video", now.minusSeconds(2), UsageEventType.FOREGROUND),
            UsageEvent("com.example.other", now.minusSeconds(1), UsageEventType.BACKGROUND),
            UsageEvent("com.example.video", now, UsageEventType.FOREGROUND),
        )

        assertEquals("com.example.video", currentForegroundPackage(events))
        assertEquals(
            null,
            currentForegroundPackage(events + UsageEvent("com.example.video", now.plusSeconds(1), UsageEventType.BACKGROUND)),
        )
    }

    @Test
    fun `activity transition ordering still identifies the foreground package`() {
        val events = listOf(
            UsageEvent("com.example.video", now.minusSeconds(2), UsageEventType.FOREGROUND, "OldActivity"),
            UsageEvent("com.example.video", now.minusSeconds(1), UsageEventType.FOREGROUND, "NewActivity"),
            UsageEvent("com.example.video", now, UsageEventType.BACKGROUND, "OldActivity"),
        )

        assertEquals("com.example.video", currentForegroundPackage(events))
    }
}

private class MutableAccessWindowStore(var endsAt: Instant?) : AccessWindowStore {
    override fun endsAt(packageName: String) = endsAt
    override fun save(packageName: String, endsAt: Instant) { this.endsAt = endsAt }
}

private class RecordingExpiryScheduler : TemporaryUseExpiryScheduler {
    private lateinit var callback: () -> Unit
    var cancelledPackageName: String? = null
    var endsAt: Instant? = null
    override fun schedule(packageName: String, endsAt: Instant, onExpired: () -> Unit) {
        this.endsAt = endsAt
        callback = onExpired
    }
    override fun cancel(packageName: String) { cancelledPackageName = packageName }
    fun expire() = callback()
}

private class MonitorRuleStore(
    private val rule: RestrictedAppRule,
) : RestrictionRuleStore {
    override fun loadRules() = listOf(rule)
    override fun saveRule(rule: RestrictedAppRule) = Unit
}

private class FixedTodayUsageLookup(
    private val usedMinutes: Int,
) : TodayUsageLookup {
    override fun usedDuration(packageName: String, now: Instant) = java.time.Duration.ofMinutes(usedMinutes.toLong())
}

private class SequenceTodayUsageLookup(
    vararg values: java.time.Duration,
) : TodayUsageLookup {
    private val values = ArrayDeque(values.toList())

    constructor(vararg values: Int) : this(*values.map { java.time.Duration.ofMinutes(it.toLong()) }.toTypedArray())

    override fun usedDuration(packageName: String, now: Instant): java.time.Duration = values.removeFirst()
}

private class MemoryExhaustedAllowanceStore : ExhaustedAllowanceStore {
    private val exhausted = mutableSetOf<Pair<String, LocalDate>>()

    override fun isExhausted(packageName: String, localDate: LocalDate) =
        packageName to localDate in exhausted

    override fun markExhausted(packageName: String, localDate: LocalDate) {
        exhausted += packageName to localDate
    }
}

private class RecordingInterventionPresenter : InterventionPresenter {
    val presented = mutableListOf<ProtectionDecision.Intervene>()

    override fun present(intervention: ProtectionDecision.Intervene) {
        presented += intervention
    }
}

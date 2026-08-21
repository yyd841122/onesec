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
    override fun usedMinutes(packageName: String, now: Instant) = usedMinutes
}

private class SequenceTodayUsageLookup(
    vararg values: Int,
) : TodayUsageLookup {
    private val values = ArrayDeque(values.toList())

    override fun usedMinutes(packageName: String, now: Instant): Int = values.removeFirst()
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

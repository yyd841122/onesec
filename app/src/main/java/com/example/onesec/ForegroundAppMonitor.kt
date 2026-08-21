package com.example.onesec

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

fun interface TodayUsageLookup {
    fun usedMinutes(packageName: String, now: Instant): Int
}

class UsageEventsTodayUsageLookup(
    private val usageEvents: UsageEventSource,
    private val zoneId: ZoneId,
) : TodayUsageLookup {
    override fun usedMinutes(packageName: String, now: Instant): Int = usedTodayMinutes(
        packageName = packageName,
        events = usageEvents.eventsBetween(Instant.EPOCH, now),
        now = now,
        zoneId = zoneId,
    )
}

fun interface ProtectionStatusProvider {
    fun protectionAvailable(): Boolean
}

fun interface InterventionPresenter {
    fun present(intervention: ProtectionDecision.Intervene)
}

class ForegroundAppMonitor(
    private val ruleStore: RestrictionRuleStore,
    private val usageLookup: TodayUsageLookup,
    private val protectionStatus: ProtectionStatusProvider,
    private val decisionEngine: RestrictionDecisionEngine,
    private val presenter: InterventionPresenter,
    private val clock: Clock,
) {
    fun onAppEnteredForeground(packageName: String): ProtectionDecision {
        val rule = ruleStore.loadRules().firstOrNull { it.packageName == packageName }
            ?: return ProtectionDecision.Allow
        val now = clock.instant()
        val decision = decisionEngine.decide(
            RestrictionDecisionRequest(
                now = now,
                zoneId = clock.zone,
                restrictedApp = InstalledApp(rule.packageName, rule.displayName),
                usedMinutes = usageLookup.usedMinutes(packageName, now),
                rule = rule,
                protectionAvailable = protectionStatus.protectionAvailable(),
            ),
        )
        if (decision is ProtectionDecision.Intervene) presenter.present(decision)
        return decision
    }
}

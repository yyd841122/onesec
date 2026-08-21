package com.example.onesec

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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

fun currentForegroundPackage(events: List<UsageEvent>): String? {
    val activeActivities = mutableMapOf<Pair<String?, String>, Instant>()
    events.sortedBy(UsageEvent::timestamp).forEach { event ->
        val activity = event.packageName to event.activityId
        when (event.type) {
            UsageEventType.FOREGROUND -> activeActivities[activity] = event.timestamp
            UsageEventType.BACKGROUND -> activeActivities.remove(activity)
            UsageEventType.SCREEN_LOCKED,
            UsageEventType.SCREEN_UNLOCKED,
            -> Unit
        }
    }
    return activeActivities.maxByOrNull { it.value }?.key?.first
}

fun interface InterventionPresenter {
    fun present(intervention: ProtectionDecision.Intervene)
}

interface AccessWindowExpiryScheduler {
    fun schedule(packageName: String, endsAt: Instant, onExpired: () -> Unit)

    fun cancel(packageName: String)
}

interface ExhaustedAllowanceStore {
    fun isExhausted(packageName: String, localDate: LocalDate): Boolean

    fun markExhausted(packageName: String, localDate: LocalDate)
}

class ForegroundAppMonitor(
    private val ruleStore: RestrictionRuleStore,
    private val usageLookup: TodayUsageLookup,
    private val protectionStatus: ProtectionStatusProvider,
    private val exhaustedAllowances: ExhaustedAllowanceStore,
    private val decisionEngine: RestrictionDecisionEngine,
    private val presenter: InterventionPresenter,
    private val clock: Clock,
    private val accessWindows: AccessWindowStore? = null,
    private val expiryScheduler: AccessWindowExpiryScheduler? = null,
) {
    private var scheduledPackageName: String? = null

    fun onAppEnteredForeground(packageName: String): ProtectionDecision {
        scheduledPackageName?.takeIf { it != packageName }?.let { previousPackageName ->
            expiryScheduler?.cancel(previousPackageName)
            scheduledPackageName = null
        }
        val rule = ruleStore.loadRules().firstOrNull { it.packageName == packageName }
            ?: return ProtectionDecision.Allow
        val now = clock.instant()
        val localDate = now.atZone(clock.zone).toLocalDate()
        val protectionAvailable = protectionStatus.protectionAvailable()
        val reportedUsedMinutes = if (protectionAvailable) {
            usageLookup.usedMinutes(packageName, now)
        } else {
            0
        }
        val usedMinutes = if (exhaustedAllowances.isExhausted(packageName, localDate)) {
            maxOf(reportedUsedMinutes, rule.dailyAllowance.minutes)
        } else {
            reportedUsedMinutes
        }
        val accessWindowEndsAt = accessWindows?.endsAt(packageName)
        val decision = decisionEngine.decide(
            RestrictionDecisionRequest(
                now = now,
                zoneId = clock.zone,
                restrictedApp = InstalledApp(rule.packageName, rule.displayName),
                usedMinutes = usedMinutes,
                rule = rule,
                protectionAvailable = protectionAvailable,
                accessWindowEndsAt = accessWindowEndsAt,
            ),
        )
        if (decision is ProtectionDecision.Intervene) {
            exhaustedAllowances.markExhausted(packageName, localDate)
            presenter.present(decision)
        } else if (
            decision == ProtectionDecision.Allow &&
            rule.level == RestrictionLevel.SOFT &&
            usedMinutes >= rule.dailyAllowance.minutes &&
            accessWindowEndsAt?.isAfter(now) == true
        ) {
            scheduledPackageName = packageName
            expiryScheduler?.schedule(packageName, accessWindowEndsAt) {
                scheduledPackageName = null
                onAppEnteredForeground(packageName)
            }
        }
        return decision
    }
}

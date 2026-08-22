package com.example.onesec

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun interface TodayUsageLookup {
    fun usedDuration(packageName: String, now: Instant): java.time.Duration
}

class UsageEventsTodayUsageLookup(
    private val usageEvents: UsageEventSource,
    private val zoneId: ZoneId,
) : TodayUsageLookup {
    override fun usedDuration(packageName: String, now: Instant): java.time.Duration = usedTodayDuration(
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

interface TemporaryUseExpiryScheduler {
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
    private val expiryScheduler: TemporaryUseExpiryScheduler? = null,
    private val emergencyOverrides: EmergencyOverrideManager? = null,
    private val foregroundPackageLookup: (() -> String?)? = null,
    private val historyStore: LocalHistoryStore? = null,
) {
    private var scheduledPackageName: String? = null

    fun onAppEnteredForeground(packageName: String): ProtectionDecision {
        val rule = ruleStore.loadRules().firstOrNull { it.packageName == packageName }
            ?: return ProtectionDecision.Allow
        val now = clock.instant()
        val localDate = now.atZone(clock.zone).toLocalDate()
        val protectionAvailable = protectionStatus.protectionAvailable()
        val reportedUsedDuration = if (protectionAvailable) {
            usageLookup.usedDuration(packageName, now)
        } else {
            java.time.Duration.ZERO
        }
        val allowanceDuration = java.time.Duration.ofMinutes(rule.dailyAllowance.minutes.toLong())
        val usedDuration = if (exhaustedAllowances.isExhausted(packageName, localDate)) {
            maxOf(reportedUsedDuration, allowanceDuration)
        } else {
            reportedUsedDuration
        }
        val usedMinutes = if (usedDuration.isZero) 0 else ((usedDuration.toMillis() + 59_999L) / 60_000L).toInt()
        val accessWindowEndsAt = accessWindows?.endsAt(packageName)
        val emergencyOverrideEndsAt = emergencyOverrides?.activeWindowEndsAt(packageName, now)
        val decision = decisionEngine.decide(
            RestrictionDecisionRequest(
                now = now,
                zoneId = clock.zone,
                restrictedApp = InstalledApp(rule.packageName, rule.displayName),
                usedMinutes = usedMinutes,
                rule = rule,
                protectionAvailable = protectionAvailable,
                accessWindowEndsAt = accessWindowEndsAt,
                emergencyOverrideEndsAt = emergencyOverrideEndsAt,
                allowanceExhausted = usedDuration >= allowanceDuration,
            ),
        )
        if (decision is ProtectionDecision.Intervene) {
            exhaustedAllowances.markExhausted(packageName, localDate)
            historyStore?.recordIntervention(packageName, now)
            presenter.present(decision)
        } else if (
            decision == ProtectionDecision.Allow &&
            usedDuration >= allowanceDuration &&
            (accessWindowEndsAt?.isAfter(now) == true || emergencyOverrideEndsAt?.isAfter(now) == true)
        ) {
            val endsAt = if (rule.level == RestrictionLevel.SOFT) accessWindowEndsAt else emergencyOverrideEndsAt
            scheduledPackageName = packageName
            expiryScheduler?.schedule(packageName, checkNotNull(endsAt)) {
                scheduledPackageName = null
                if (foregroundPackageLookup == null || foregroundPackageLookup.invoke() == packageName) {
                    onAppEnteredForeground(packageName)
                }
            }
        } else if (decision == ProtectionDecision.Allow && usedDuration < allowanceDuration) {
            scheduledPackageName = packageName
            expiryScheduler?.schedule(packageName, now.plus(allowanceDuration.minus(usedDuration))) {
                scheduledPackageName = null
                if (foregroundPackageLookup == null || foregroundPackageLookup.invoke() == packageName) {
                    onAppEnteredForeground(packageName)
                }
            }
        }
        return decision
    }
}

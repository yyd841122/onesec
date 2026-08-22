package com.example.onesec

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

enum class UsageEventType {
    FOREGROUND,
    BACKGROUND,
    SCREEN_LOCKED,
    SCREEN_UNLOCKED,
}

data class UsageEvent(
    val packageName: String?,
    val timestamp: Instant,
    val type: UsageEventType,
    val activityId: String = "default",
)

interface UsageEventSource {
    fun eventsBetween(start: Instant, end: Instant): List<UsageEvent>
}

data class TodayAppUsage(
    val packageName: String,
    val displayName: String,
    val usedMinutes: Int,
    val remainingMinutes: Int,
    val level: RestrictionLevel = RestrictionLevel.HARD,
    val pendingRelaxation: PendingRelaxation? = null,
)

data class TodayUsageState(
    val protectionAvailable: Boolean,
    val apps: List<TodayAppUsage>,
    val totalUsedMinutes: Int = 0,
    val interventionCount: Int = 0,
    val emergencyOverrideUsed: Boolean = false,
)

class TodayUsageController(
    private val ruleStore: RestrictionRuleStore,
    private val permissionGateway: PermissionGateway,
    private val usageEvents: UsageEventSource,
    private val clock: Clock,
    private val historyStore: LocalHistoryStore? = null,
    private val localDataClearer: LocalDataClearer? = null,
) {
    var state = TodayUsageState(protectionAvailable = false, apps = emptyList())
        private set

    fun refresh() {
        val rules = ruleStore.loadRules()
        val protectionAvailable = permissionGuidanceState(
            permissionGateway.readPermissions(),
        ).protectionAvailable
        if (!protectionAvailable) {
            state = TodayUsageState(protectionAvailable = false, apps = emptyList())
            return
        }

        val now = clock.instant()
        val events = usageEvents.eventsBetween(Instant.EPOCH, now)
        val today = now.atZone(clock.zone).toLocalDate()
        historyStore?.pruneBefore(today.minusDays(89))
        val history = historyStore?.today(today) ?: TodayHistory()
        val pending = (ruleStore as? RestrictionPolicyStore)?.loadPendingRelaxations().orEmpty()
        val apps = rules.map { rule ->
            val usedMinutes = usedTodayMinutes(rule.packageName, events, now, clock.zone)
            historyStore?.recordUsage(rule.packageName, today, usedMinutes)
            TodayAppUsage(
                packageName = rule.packageName,
                displayName = rule.displayName,
                usedMinutes = usedMinutes,
                remainingMinutes = (rule.dailyAllowance.minutes - usedMinutes).coerceAtLeast(0),
                level = rule.level,
                pendingRelaxation = pending.firstOrNull { it.packageNameOrNull == rule.packageName },
            )
        }
        state = TodayUsageState(
            protectionAvailable = true,
            apps = apps,
            totalUsedMinutes = apps.sumOf(TodayAppUsage::usedMinutes),
            interventionCount = history.interventionCount,
            emergencyOverrideUsed = history.emergencyOverrideUsed,
        )
    }

    fun clearAllLocalData() {
        localDataClearer?.clearAll()
        state = TodayUsageState(protectionAvailable = true, apps = emptyList())
    }
}

private val PendingRelaxation.packageNameOrNull: String?
    get() = when (this) {
        is PendingRelaxation.ReplaceRule -> replacement.packageName
        is PendingRelaxation.RemoveRule -> app.packageName
        is PendingRelaxation.DisableProtection -> null
    }

fun usedTodayMinutes(
    packageName: String,
    events: List<UsageEvent>,
    now: Instant,
    zoneId: ZoneId,
): Int {
    val usedMillis = usedTodayDuration(packageName, events, now, zoneId).toMillis()
    return if (usedMillis == 0L) 0 else ((usedMillis + 59_999L) / 60_000L).toInt()
}

fun usedTodayDuration(
    packageName: String,
    events: List<UsageEvent>,
    now: Instant,
    zoneId: ZoneId,
): Duration {
    val todayStart = now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
    return Duration.ofMillis(foregroundMillis(packageName, events, todayStart, now))
}

private fun foregroundMillis(
    packageName: String,
    events: List<UsageEvent>,
    dayStart: Instant,
    now: Instant,
): Long {
    val foregroundActivities = mutableMapOf<String, Int>()
    var screenInteractive = true
    var countingSince: Instant? = null
    var pendingBackgroundAt: Instant? = null
    var totalMillis = 0L

    fun stopCounting(at: Instant) {
        countingSince?.let { start ->
            totalMillis += java.time.Duration.between(maxOf(start, dayStart), at).toMillis()
                .coerceAtLeast(0)
        }
        countingSince = null
    }

    events.sortedBy(UsageEvent::timestamp).forEach { event ->
        if (
            pendingBackgroundAt != null &&
            event.type == UsageEventType.FOREGROUND &&
            event.packageName != packageName
        ) {
            stopCounting(pendingBackgroundAt!!)
            pendingBackgroundAt = null
        }
        when {
            event.type == UsageEventType.FOREGROUND && event.packageName == packageName -> {
                pendingBackgroundAt?.let { pausedAt ->
                    if (Duration.between(pausedAt, event.timestamp) > ACTIVITY_HANDOFF_WINDOW) {
                        stopCounting(pausedAt)
                    }
                }
                foregroundActivities[event.activityId] =
                    foregroundActivities.getOrDefault(event.activityId, 0) + 1
                pendingBackgroundAt = null
                if (screenInteractive && countingSince == null) countingSince = event.timestamp
            }
            event.type == UsageEventType.BACKGROUND && event.packageName == packageName -> {
                val resumedCount = foregroundActivities.getOrDefault(event.activityId, 0)
                if (resumedCount <= 1) {
                    foregroundActivities.remove(event.activityId)
                } else {
                    foregroundActivities[event.activityId] = resumedCount - 1
                }
                if (foregroundActivities.isEmpty()) pendingBackgroundAt = event.timestamp
            }
            event.type == UsageEventType.SCREEN_LOCKED -> {
                screenInteractive = false
                stopCounting(pendingBackgroundAt ?: event.timestamp)
                pendingBackgroundAt = null
            }
            event.type == UsageEventType.SCREEN_UNLOCKED -> {
                screenInteractive = true
                if (foregroundActivities.isNotEmpty() && countingSince == null) {
                    countingSince = event.timestamp
                }
            }
        }
    }

    stopCounting(pendingBackgroundAt ?: now)
    return totalMillis
}

private val ACTIVITY_HANDOFF_WINDOW: Duration = Duration.ofSeconds(5)

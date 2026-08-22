package com.example.onesec

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import java.time.Clock
import java.time.Duration

class OneSecAccessibilityService : AccessibilityService() {
    private val clock = Clock.systemDefaultZone()
    private val usageEvents by lazy { AndroidUsageEventSource(this) }
    private val monitor: ForegroundAppMonitor by lazy {
        createForegroundAppMonitor(
            context = this,
            clock = clock,
            usageEvents = usageEvents,
            expiryScheduler = AndroidTemporaryUseExpiryScheduler(this),
        )
    }
    private val eventHandler by lazy { AndroidForegroundEventHandler(monitor) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        eventHandler.onAccessibilityEvent(event)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val now = clock.instant()
        currentForegroundPackage(usageEvents.eventsBetween(now.minus(Duration.ofDays(1)), now))
            ?.let(monitor::onAppEnteredForeground)
    }

    override fun onInterrupt() = Unit
}

class AndroidTemporaryUseExpiryScheduler(
    context: Context,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : TemporaryUseExpiryScheduler {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

    override fun schedule(packageName: String, endsAt: java.time.Instant, onExpired: () -> Unit) {
        entries.remove(packageName)?.let(handler::removeCallbacks)
        val delayMillis = java.time.Duration.between(java.time.Instant.now(), endsAt).toMillis().coerceAtLeast(0)
        val deadlineMillis = endsAt.toEpochMilli()
        val expiry = ScheduledExpiry(packageName, deadlineMillis, onExpired)
        entries[packageName] = expiry
        handler.postDelayed(expiry, delayMillis)
        val alarm = alarmIntent(packageName, deadlineMillis)
        val triggerAt = SystemClock.elapsedRealtime() + delayMillis
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, alarm)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, alarm)
        }
    }


    override fun cancel(packageName: String) {
        entries.remove(packageName)?.let(handler::removeCallbacks)
        alarmManager.cancel(alarmIntent(packageName))
    }

    private fun alarmIntent(
        packageName: String,
        deadlineMillis: Long = Long.MIN_VALUE,
    ): PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        packageName.hashCode(),
        Intent(applicationContext, AllowanceDeadlineWakeReceiver::class.java)
            .setAction("${applicationContext.packageName}.ALLOWANCE_DEADLINE")
            .setPackage(applicationContext.packageName)
            .putExtra(EXTRA_PACKAGE_NAME, packageName)
            .putExtra(EXTRA_DEADLINE_MILLIS, deadlineMillis),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val EXTRA_PACKAGE_NAME = "restricted_package_name"
        private const val EXTRA_DEADLINE_MILLIS = "deadline_millis"
        private val entries = mutableMapOf<String, ScheduledExpiry>()
        private val completedDeadlines = mutableMapOf<String, Long>()

        fun packageName(intent: Intent): String? = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        fun deadline(intent: Intent): Long = intent.getLongExtra(EXTRA_DEADLINE_MILLIS, Long.MIN_VALUE)
        fun dispatch(packageName: String, deadlineMillis: Long): Boolean {
            entries[packageName]
                ?.takeIf { it.deadlineMillis == deadlineMillis }
                ?.let {
                    it.run()
                    return true
                }
            return completedDeadlines[packageName] == deadlineMillis
        }

        private class ScheduledExpiry(
            private val packageName: String,
            val deadlineMillis: Long,
            private val onExpired: () -> Unit,
        ) : Runnable {
            private var delivered = false

            override fun run() {
                if (delivered) return
                delivered = true
                entries.remove(packageName, this)
                completedDeadlines[packageName] = deadlineMillis
                onExpired()
            }
        }
    }
}

class AllowanceDeadlineWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduledPackage = AndroidTemporaryUseExpiryScheduler.packageName(intent) ?: return
        val deadlineMillis = AndroidTemporaryUseExpiryScheduler.deadline(intent)
        if (deadlineMillis == Long.MIN_VALUE) return
        if (AndroidTemporaryUseExpiryScheduler.dispatch(scheduledPackage, deadlineMillis)) return
        val clock = Clock.systemDefaultZone()
        val usageEvents = AndroidUsageEventSource(context)
        val now = clock.instant()
        val foregroundPackage = currentForegroundPackage(
            usageEvents.eventsBetween(now.minus(Duration.ofDays(1)), now),
        ) ?: return
        if (foregroundPackage != scheduledPackage) return
        createForegroundAppMonitor(context, clock, usageEvents, expiryScheduler = null)
            .onAppEnteredForeground(foregroundPackage)
    }
}

private fun createForegroundAppMonitor(
    context: Context,
    clock: Clock,
    usageEvents: AndroidUsageEventSource,
    expiryScheduler: TemporaryUseExpiryScheduler?,
): ForegroundAppMonitor {
    val permissionGateway = AndroidPermissionGateway(context)
    val historyStore = SharedPreferencesLocalHistoryStore(context, clock.zone)
    val historyLookup = UsageEventsTodayUsageLookup(usageEvents, clock.zone)
    return ForegroundAppMonitor(
        ruleStore = SharedPreferencesRestrictionRuleStore(context),
        usageLookup = historyLookup,
        protectionStatus = {
            permissionGuidanceState(permissionGateway.readPermissions()).protectionAvailable
        },
        exhaustedAllowances = SharedPreferencesExhaustedAllowanceStore(context),
        decisionEngine = DefaultRestrictionDecisionEngine,
        presenter = AndroidInterventionPresenter(context),
        clock = clock,
        accessWindows = SharedPreferencesAccessWindowStore(context),
        expiryScheduler = expiryScheduler,
        emergencyOverrides = EmergencyOverrideManager(
            SharedPreferencesEmergencyOverrideStore(context),
            clock.zone,
            historyStore,
        ),
        foregroundPackageLookup = {
            val now = clock.instant()
            currentForegroundPackage(usageEvents.eventsBetween(now.minus(Duration.ofDays(1)), now))
        },
        historyStore = historyStore,
        usageHistoryLookup = historyLookup::usedDurationOn,
    )
}

class AndroidForegroundEventHandler(
    private val monitor: ForegroundAppMonitor,
) {
    private var foregroundPackageName: String? = null

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == foregroundPackageName) return
        foregroundPackageName?.let(monitor::onAppLeftForeground)
        foregroundPackageName = packageName
        monitor.onAppEnteredForeground(packageName)
    }
}

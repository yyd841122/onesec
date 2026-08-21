package com.example.onesec

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper
import java.time.Clock
import java.time.Duration

class OneSecAccessibilityService : AccessibilityService() {
    private val clock = Clock.systemDefaultZone()
    private val usageEvents by lazy { AndroidUsageEventSource(this) }
    private val monitor: ForegroundAppMonitor by lazy {
        val permissionGateway = AndroidPermissionGateway(this)
        ForegroundAppMonitor(
            ruleStore = SharedPreferencesRestrictionRuleStore(this),
            usageLookup = UsageEventsTodayUsageLookup(usageEvents, clock.zone),
            protectionStatus = {
                permissionGuidanceState(permissionGateway.readPermissions()).protectionAvailable
            },
            exhaustedAllowances = SharedPreferencesExhaustedAllowanceStore(this),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = AndroidInterventionPresenter(this),
            clock = clock,
            accessWindows = SharedPreferencesAccessWindowStore(this),
            expiryScheduler = AndroidTemporaryUseExpiryScheduler(),
            emergencyOverrides = EmergencyOverrideManager(
                SharedPreferencesEmergencyOverrideStore(this),
                clock.zone,
            ),
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
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : TemporaryUseExpiryScheduler {
    private val callbacks = mutableMapOf<String, Runnable>()

    override fun schedule(packageName: String, endsAt: java.time.Instant, onExpired: () -> Unit) {
        callbacks.remove(packageName)?.let(handler::removeCallbacks)
        val callback = Runnable {
            callbacks.remove(packageName)
            onExpired()
        }
        callbacks[packageName] = callback
        handler.postDelayed(callback, java.time.Duration.between(java.time.Instant.now(), endsAt).toMillis().coerceAtLeast(0))
    }


    override fun cancel(packageName: String) {
        callbacks.remove(packageName)?.let(handler::removeCallbacks)
    }
}

class AndroidForegroundEventHandler(
    private val monitor: ForegroundAppMonitor,
) {
    private var foregroundPackageName: String? = null

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == foregroundPackageName) return
        foregroundPackageName = packageName
        monitor.onAppEnteredForeground(packageName)
    }
}

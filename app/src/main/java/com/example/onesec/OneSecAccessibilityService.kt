package com.example.onesec

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.time.Clock

class OneSecAccessibilityService : AccessibilityService() {
    private val monitor: ForegroundAppMonitor by lazy {
        val clock = Clock.systemDefaultZone()
        val permissionGateway = AndroidPermissionGateway(this)
        ForegroundAppMonitor(
            ruleStore = SharedPreferencesRestrictionRuleStore(this),
            usageLookup = UsageEventsTodayUsageLookup(AndroidUsageEventSource(this), clock.zone),
            protectionStatus = {
                permissionGuidanceState(permissionGateway.readPermissions()).protectionAvailable
            },
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = AndroidInterventionPresenter(this),
            clock = clock,
        )
    }
    private val eventHandler by lazy { AndroidForegroundEventHandler(monitor) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        eventHandler.onAccessibilityEvent(event)
    }

    override fun onInterrupt() = Unit
}

class AndroidForegroundEventHandler(
    private val monitor: ForegroundAppMonitor,
) {
    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        monitor.onAppEnteredForeground(packageName)
    }
}

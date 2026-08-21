package com.example.onesec

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MonitoringRecoveryCoordinator(
    private val context: Context,
    private val ruleStore: RestrictionRuleStore = SharedPreferencesRestrictionRuleStore(context),
    private val permissionSnapshot: () -> PermissionSnapshot = {
        AndroidPermissionGateway(context).readPermissions()
    },
    private val healthStore: RecoveryHealthStore = SharedPreferencesRecoveryHealthStore(context),
) {

    fun restore() {
        val permissions = permissionSnapshot()
        when (
            decideStartupRecovery(
                RecoveryRequest(
                    hasSavedRules = ruleStore.loadRules().isNotEmpty(),
                    corePermissionsAvailable = permissionGuidanceState(permissions).protectionAvailable,
                ),
            )
        ) {
            RecoveryDecision.NothingToRestore -> stopNotificationService(RecoveryHealth.NOT_REQUIRED)
            RecoveryDecision.NeedsRepair -> stopNotificationService(RecoveryHealth.NEEDS_REPAIR)
            RecoveryDecision.StartMonitoring -> {
                // Keep the failure state until the service proves it reached the foreground.
                healthStore.writeRecoveryHealth(RecoveryHealth.NEEDS_REPAIR)
                runCatching {
                    context.startForegroundService(
                        Intent(context, ProtectionNotificationService::class.java),
                    )
                }.onFailure {
                    healthStore.writeRecoveryHealth(RecoveryHealth.NEEDS_REPAIR)
                }
            }
        }
    }

    private fun stopNotificationService(health: RecoveryHealth) {
        context.stopService(Intent(context, ProtectionNotificationService::class.java))
        healthStore.writeRecoveryHealth(health)
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MonitoringRecoveryCoordinator(context.applicationContext).restore()
        }
    }
}

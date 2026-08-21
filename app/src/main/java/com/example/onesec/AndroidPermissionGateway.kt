package com.example.onesec

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

class AndroidPermissionGateway(
    private val context: Context,
) : PermissionGateway {
    override fun readPermissions(): PermissionSnapshot =
        PermissionSnapshot(
            usageAccessGranted = hasUsageAccess(),
            accessibilityGranted = isAccessibilityServiceEnabled(),
        )

    override fun openUsageAccessSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    override fun openAccessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    override fun openBatteryOptimizationSettings() {
        runCatching {
            context.startActivity(batteryOptimizationIntent(context.packageName))
        }.onFailure {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    override fun openBackgroundRunSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        return appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        ) == 1
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val serviceName = ComponentName(context, OneSecAccessibilityService::class.java)
            .flattenToString()

        return accessibilityEnabled && enabledServices
            .split(':')
            .any { it.equals(serviceName, ignoreCase = true) }
    }
}

internal fun batteryOptimizationIntent(packageName: String): Intent = Intent(
    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    Uri.parse("package:$packageName"),
)

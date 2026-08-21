package com.example.onesec

data class PermissionSnapshot(
    val usageAccessGranted: Boolean,
    val accessibilityGranted: Boolean,
)

data class PermissionGuidanceState(
    val usageAccessGranted: Boolean,
    val accessibilityGranted: Boolean,
    val protectionHealth: ProtectionHealth,
    val recoveryHealth: RecoveryHealth = RecoveryHealth.NOT_REQUIRED,
) {
    val protectionAvailable: Boolean
        get() = protectionHealth == ProtectionHealth.AVAILABLE

    val usageDataReliable: Boolean
        get() = protectionAvailable

    val protectionStatus: String
        get() = when {
            recoveryHealth == RecoveryHealth.NEEDS_REPAIR -> "保护需要修复"
            protectionAvailable -> "保护可用"
            else -> "保护失效"
        }

    val statusExplanation: String
        get() = when {
            recoveryHealth == RecoveryHealth.NEEDS_REPAIR ->
                "系统未能在重启后恢复保护，请检查后台运行设置。"
            protectionAvailable -> "两项核心权限均有效，使用数据可靠。"
            else -> "使用数据不可靠：请修复下方缺失的核心权限。"
        }
}

enum class ProtectionHealth {
    AVAILABLE,
    UNAVAILABLE,
}

fun permissionGuidanceState(
    snapshot: PermissionSnapshot,
    recoveryHealth: RecoveryHealth = RecoveryHealth.NOT_REQUIRED,
): PermissionGuidanceState {
    val protectionAvailable = snapshot.usageAccessGranted && snapshot.accessibilityGranted &&
        recoveryHealth != RecoveryHealth.NEEDS_REPAIR
    return PermissionGuidanceState(
        usageAccessGranted = snapshot.usageAccessGranted,
        accessibilityGranted = snapshot.accessibilityGranted,
        protectionHealth = if (protectionAvailable) {
            ProtectionHealth.AVAILABLE
        } else {
            ProtectionHealth.UNAVAILABLE
        },
        recoveryHealth = recoveryHealth,
    )
}

interface PermissionGateway {
    fun readPermissions(): PermissionSnapshot

    fun openUsageAccessSettings()

    fun openAccessibilitySettings()

    fun openBatteryOptimizationSettings() = Unit

    fun openBackgroundRunSettings() = Unit
}

class PermissionGuidanceController(
    private val gateway: PermissionGateway,
    private val recoveryHealthProvider: RecoveryHealthProvider =
        RecoveryHealthProvider { RecoveryHealth.NOT_REQUIRED },
) {
    var state: PermissionGuidanceState = readState()
        private set

    fun refresh() {
        state = readState()
    }

    fun openUsageAccessSettings() = gateway.openUsageAccessSettings()

    fun openAccessibilitySettings() = gateway.openAccessibilitySettings()

    fun openBatteryOptimizationSettings() = gateway.openBatteryOptimizationSettings()

    fun openBackgroundRunSettings() = gateway.openBackgroundRunSettings()

    private fun readState() = permissionGuidanceState(
        snapshot = gateway.readPermissions(),
        recoveryHealth = recoveryHealthProvider.readRecoveryHealth(),
    )
}

package com.example.onesec

data class PermissionSnapshot(
    val usageAccessGranted: Boolean,
    val accessibilityGranted: Boolean,
)

data class PermissionGuidanceState(
    val usageAccessGranted: Boolean,
    val accessibilityGranted: Boolean,
    val health: ProtectionHealth,
) {
    val protectionAvailable: Boolean
        get() = health == ProtectionHealth.AVAILABLE

    val usageDataReliable: Boolean
        get() = protectionAvailable

    val protectionStatus: String
        get() = if (protectionAvailable) "保护可用" else "保护失效"
}

enum class ProtectionHealth {
    AVAILABLE,
    UNAVAILABLE,
}

fun permissionGuidanceState(snapshot: PermissionSnapshot): PermissionGuidanceState {
    val protectionAvailable = snapshot.usageAccessGranted && snapshot.accessibilityGranted
    return PermissionGuidanceState(
        usageAccessGranted = snapshot.usageAccessGranted,
        accessibilityGranted = snapshot.accessibilityGranted,
        health = if (protectionAvailable) {
            ProtectionHealth.AVAILABLE
        } else {
            ProtectionHealth.UNAVAILABLE
        },
    )
}

interface PermissionGateway {
    fun readPermissions(): PermissionSnapshot

    fun openUsageAccessSettings()

    fun openAccessibilitySettings()
}

class PermissionGuidanceController(
    private val gateway: PermissionGateway,
) {
    var state: PermissionGuidanceState = permissionGuidanceState(gateway.readPermissions())
        private set

    fun refresh() {
        state = permissionGuidanceState(gateway.readPermissions())
    }

    fun openUsageAccessSettings() = gateway.openUsageAccessSettings()

    fun openAccessibilitySettings() = gateway.openAccessibilitySettings()
}

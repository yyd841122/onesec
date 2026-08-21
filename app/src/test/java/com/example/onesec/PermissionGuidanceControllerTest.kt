package com.example.onesec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGuidanceControllerTest {
    @Test
    fun `protection is available only when both core permissions are available`() {
        val expectations = listOf(
            PermissionSnapshot(usageAccessGranted = false, accessibilityGranted = false) to false,
            PermissionSnapshot(usageAccessGranted = true, accessibilityGranted = false) to false,
            PermissionSnapshot(usageAccessGranted = false, accessibilityGranted = true) to false,
            PermissionSnapshot(usageAccessGranted = true, accessibilityGranted = true) to true,
        )

        expectations.forEach { (snapshot, protectionAvailable) ->
            val state = permissionGuidanceState(snapshot)

            assertEquals(protectionAvailable, state.protectionAvailable)
            assertEquals(protectionAvailable, state.usageDataReliable)
            if (protectionAvailable) {
                assertEquals("保护可用", state.protectionStatus)
            } else {
                assertEquals("保护失效", state.protectionStatus)
            }
        }
    }

    @Test
    fun `refresh reads the latest permission state after returning from settings`() {
        val gateway = FakePermissionGateway(
            PermissionSnapshot(usageAccessGranted = false, accessibilityGranted = false),
        )
        val controller = PermissionGuidanceController(gateway)

        assertFalse(controller.state.protectionAvailable)

        gateway.snapshot = PermissionSnapshot(usageAccessGranted = true, accessibilityGranted = true)
        controller.refresh()

        assertTrue(controller.state.protectionAvailable)
        assertEquals(2, gateway.readCount)
    }
}

private class FakePermissionGateway(
    var snapshot: PermissionSnapshot,
) : PermissionGateway {
    var readCount = 0

    override fun readPermissions(): PermissionSnapshot {
        readCount += 1
        return snapshot
    }

    override fun openUsageAccessSettings() = Unit

    override fun openAccessibilitySettings() = Unit
}

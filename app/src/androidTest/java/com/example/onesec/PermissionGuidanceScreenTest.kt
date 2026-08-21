package com.example.onesec

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionGuidanceScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingPermissionsAreExplainedAndTheirSettingsCanBeOpened() {
        var usageSettingsOpenCount = 0
        var accessibilitySettingsOpenCount = 0

        composeRule.setContent {
            OneSecApp(
                state = permissionGuidanceState(
                    PermissionSnapshot(usageAccessGranted = false, accessibilityGranted = false),
                ),
                onOpenUsageAccessSettings = { usageSettingsOpenCount += 1 },
                onOpenAccessibilitySettings = { accessibilitySettingsOpenCount += 1 },
            )
        }

        composeRule.onNodeWithText("保护失效").assertIsDisplayed()
        composeRule.onNodeWithText("使用数据不可靠：请修复下方缺失的核心权限。")
            .assertIsDisplayed()
        composeRule.onNodeWithText("使用情况访问权限用于统计应用前台用时。")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("无障碍权限用于识别当前应用并触发拦截。")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText("打开使用情况访问设置").performScrollTo().performClick()
        composeRule.onNodeWithText("打开无障碍设置").performScrollTo().performClick()

        assertEquals(1, usageSettingsOpenCount)
        assertEquals(1, accessibilitySettingsOpenCount)
    }

    @Test
    fun returningFromSettingsRefreshesProtectionStatus() {
        val lifecycleOwner = TestLifecycleOwner()
        val gateway = FakePermissionGateway(
            PermissionSnapshot(usageAccessGranted = false, accessibilityGranted = false),
        )
        val controller = PermissionGuidanceController(gateway)
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                PermissionGuidanceRoute(controller)
            }
        }
        composeRule.onNodeWithText("保护失效").assertIsDisplayed()

        gateway.snapshot = PermissionSnapshot(
            usageAccessGranted = true,
            accessibilityGranted = true,
        )
        composeRule.runOnIdle {
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        composeRule.onNodeWithText("保护可用").assertIsDisplayed()
    }
}

private class TestLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle = registry
}

private class FakePermissionGateway(
    var snapshot: PermissionSnapshot,
) : PermissionGateway {
    override fun readPermissions(): PermissionSnapshot = snapshot

    override fun openUsageAccessSettings() = Unit

    override fun openAccessibilitySettings() = Unit
}

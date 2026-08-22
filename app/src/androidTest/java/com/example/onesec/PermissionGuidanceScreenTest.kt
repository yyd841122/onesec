package com.example.onesec

import androidx.compose.runtime.CompositionLocalProvider
import android.provider.Settings
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
        composeRule.onNodeWithText("统计受限应用在前台的实际使用时间。")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("识别当前应用并在额度耗尽后触发限制。")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText("使用设置").performScrollTo().performClick()
        composeRule.onNodeWithText("无障碍设置").performScrollTo().performClick()

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

    @Test
    fun colorOsBackgroundGuidanceOpensBatteryAndAppSettings() {
        var batterySettingsOpenCount = 0
        var appSettingsOpenCount = 0

        composeRule.setContent {
            OneSecApp(
                state = permissionGuidanceState(PermissionSnapshot(true, true)),
                onOpenUsageAccessSettings = {},
                onOpenAccessibilitySettings = {},
                onOpenBatteryOptimizationSettings = { batterySettingsOpenCount += 1 },
                onOpenBackgroundRunSettings = { appSettingsOpenCount += 1 },
            )
        }

        composeRule.onNodeWithText("允许后台持续保护").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("建议关闭电池优化，并允许自启动、关联启动和后台活动。")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("电池优化").performScrollTo().performClick()
        composeRule.onNodeWithText("应用设置").performScrollTo().performClick()

        assertEquals(1, batterySettingsOpenCount)
        assertEquals(1, appSettingsOpenCount)
    }

    @Test
    fun batteryOptimizationIntentTargetsOneSecDirectly() {
        val intent = batteryOptimizationIntent("com.example.onesec")

        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
        assertEquals("package:com.example.onesec", intent.data.toString())
    }

    @Test
    fun exactAlarmGuidanceOpensOneSecSpecialAccessSettings() {
        var opens = 0
        composeRule.setContent {
            OneSecApp(
                state = permissionGuidanceState(PermissionSnapshot(true, true, exactAlarmsGranted = false)),
                onOpenUsageAccessSettings = {},
                onOpenAccessibilitySettings = {},
                onOpenExactAlarmSettings = { opens += 1 },
            )
        }

        composeRule.onNodeWithText("精确到期提醒").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("提醒设置").performScrollTo().performClick()
        assertEquals(1, opens)

        val intent = exactAlarmPermissionIntent("com.example.onesec")
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, intent.action)
        assertEquals("package:com.example.onesec", intent.data.toString())
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

package com.example.onesec

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class TodayUsageScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reliableUsageShowsUsedAndRemainingAllowance() {
        composeRule.setContent {
            TodayUsageScreen(
                state = TodayUsageState(
                    protectionAvailable = true,
                    apps = listOf(TodayAppUsage("com.example.video", "短视频", 12, 18)),
                ),
                onBack = {},
            )
        }

        composeRule.onNodeWithText("今日已用 12 分钟").assertIsDisplayed()
        composeRule.onNodeWithText("剩余每日额度 18 分钟").assertIsDisplayed()
        composeRule.onNodeWithText("不足一分钟的用时按一分钟计入。").assertIsDisplayed()
    }

    @Test
    fun revokedUsageAccessHidesApparentlyReliableRemainingAllowance() {
        composeRule.setContent {
            TodayUsageScreen(
                state = TodayUsageState(protectionAvailable = false, apps = emptyList()),
                onBack = {},
            )
        }

        composeRule.onNodeWithText("保护失效").assertIsDisplayed()
        composeRule.onNodeWithText("核心权限已失效，今日用量不可靠。修复权限后再显示剩余额度。")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("剩余每日额度 0 分钟").assertCountEquals(0)
    }

    @Test
    fun overviewShowsTotalsRuleDetailsActivityAndPendingChange() {
        composeRule.setContent {
            TodayUsageScreen(
                state = TodayUsageState(
                    protectionAvailable = true,
                    apps = listOf(
                        TodayAppUsage(
                            "com.example.video",
                            "短视频",
                            12,
                            18,
                            RestrictionLevel.HARD,
                            PendingRelaxation.RemoveRule(
                                InstalledApp("com.example.video", "短视频"),
                                java.time.LocalDate.of(2026, 8, 22),
                            ),
                        ),
                    ),
                    totalUsedMinutes = 12,
                    interventionCount = 3,
                    emergencyOverrideUsed = true,
                ),
                onBack = {},
                onClearAllLocalData = {},
            )
        }

        composeRule.onNodeWithText("全部受限应用今日总用时 12 分钟").assertIsDisplayed()
        composeRule.onNodeWithText("强限制").assertIsDisplayed()
        composeRule.onNodeWithText("今日拦截 3 次").assertIsDisplayed()
        composeRule.onNodeWithText("今日紧急解锁：已使用").assertIsDisplayed()
        composeRule.onNodeWithText("待生效：移除限制（2026-08-22 生效）").assertIsDisplayed()
    }

    @Test
    fun clearAllLocalDataRequiresExplicitConfirmation() {
        var clears = 0
        composeRule.setContent {
            TodayUsageScreen(
                state = TodayUsageState(true, emptyList()),
                onBack = {},
                onClearAllLocalData = { clears += 1 },
            )
        }

        composeRule.onNodeWithText("清除全部本地数据").performClick()
        assertEquals(0, clears)
        composeRule.onNodeWithText("确认清除").performClick()
        assertEquals(1, clears)
    }
}

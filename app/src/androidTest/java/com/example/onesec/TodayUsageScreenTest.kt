package com.example.onesec

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test

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
}

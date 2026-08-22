package com.example.onesec

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RestrictionSetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun slowCatalogShowsImmediateLoadingFeedbackWithoutBlockingTheClick() {
        val controller = RestrictionSetupController(
            appCatalog = SlowScreenAppCatalog(
                delayMillis = 1_500,
                apps = listOf(InstalledApp("com.example.video", "短视频")),
            ),
            ruleStore = ScreenRuleStore(),
        )
        composeRule.setContent { RestrictionSetupRoute(controller, onBack = {}) }

        val startedAt = System.nanoTime()
        composeRule.onNodeWithText("选择受限应用").performClick()
        val clickMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertTrue("点击被应用扫描阻塞了 ${clickMillis}ms", clickMillis < 500)
        composeRule.onNodeWithText("正在加载应用…").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching {
                composeRule.onNodeWithText("短视频").assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun userCanChooseAnAppAdjustAllowanceAndSaveAHardRestriction() {
        val controller = RestrictionSetupController(
            appCatalog = ScreenAppCatalog(
                listOf(InstalledApp("com.example.video", "短视频")),
            ),
            ruleStore = ScreenRuleStore(),
        )
        composeRule.setContent {
            RestrictionSetupRoute(controller = controller, onBack = {})
        }

        composeRule.onNodeWithText("选择受限应用").performClick()
        composeRule.waitForText("短视频")
        composeRule.onNodeWithText("短视频").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("短视频 图标").assertIsDisplayed()
        composeRule.onNodeWithText("30 分钟").assertIsDisplayed()

        composeRule.onNodeWithText("＋ 5 分钟").performClick()
        composeRule.onNodeWithText("保存强限制").performClick()

        composeRule.onNodeWithText("短视频").assertIsDisplayed()
        composeRule.onNodeWithText("强限制 · 每日 35 分钟").assertIsDisplayed()
    }

    @Test
    fun userCanSaveASoftRestrictionWithTheSixtyMinuteDefault() {
        val controller = RestrictionSetupController(
            appCatalog = ScreenAppCatalog(listOf(InstalledApp("com.example.video", "短视频"))),
            ruleStore = ScreenRuleStore(),
        )
        composeRule.setContent { RestrictionSetupRoute(controller, onBack = {}) }

        composeRule.onNodeWithText("选择受限应用").performClick()
        composeRule.waitForText("短视频")
        composeRule.onNodeWithText("短视频").performClick()
        composeRule.onNodeWithText("弱限制").performClick()
        composeRule.onNodeWithText("60 分钟").assertIsDisplayed()
        composeRule.onNodeWithText("保存弱限制").performClick()

        composeRule.onNodeWithText("弱限制 · 每日 60 分钟").assertIsDisplayed()
    }

    @Test
    fun currentRuleAndTomorrowRelaxationAreClearlySeparated() {
        val current = RestrictedAppRule(
            "com.example.video",
            "短视频",
            RestrictionLevel.HARD,
            DailyAllowance.ofMinutes(30),
        )
        val controller = RestrictionSetupController(
            appCatalog = ScreenAppCatalog(listOf(InstalledApp(current.packageName, current.displayName))),
            ruleStore = ScreenRuleStore(current),
            clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC),
        )
        composeRule.setContent { RestrictionSetupRoute(controller, onBack = {}) }

        composeRule.onNodeWithText("选择受限应用").performClick()
        composeRule.waitForText("短视频")
        composeRule.onNodeWithText("短视频").performClick()
        repeat(3) { composeRule.onNodeWithText("＋ 5 分钟").performClick() }
        composeRule.onNodeWithText("保存强限制").performClick()

        composeRule.onNodeWithText("当前生效规则").assertIsDisplayed()
        composeRule.onNodeWithText("强限制 · 每日 30 分钟").assertIsDisplayed()
        composeRule.onNodeWithText("次日即将生效").assertIsDisplayed()
        composeRule.onNodeWithText("短视频：改为强限制，每日额度 45 分钟（2026-08-22 生效）")
            .assertIsDisplayed()
    }

    @Test
    fun existingHardRuleCanBeEditedToCreateTomorrowSoftRestriction() {
        val current = RestrictedAppRule(
            "com.example.video",
            "短视频",
            RestrictionLevel.HARD,
            DailyAllowance.ofMinutes(30),
        )
        val controller = RestrictionSetupController(
            appCatalog = ScreenAppCatalog(emptyList()),
            ruleStore = ScreenRuleStore(current),
            clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC),
        )
        composeRule.setContent { RestrictionSetupRoute(controller, onBack = {}) }

        composeRule.onNodeWithText("编辑规则").performClick()
        composeRule.onNodeWithText("30 分钟").assertIsDisplayed()
        composeRule.onNodeWithText("弱限制").performClick()
        composeRule.onNodeWithText("保存弱限制").performClick()

        composeRule.onNodeWithText("强限制 · 每日 30 分钟").assertIsDisplayed()
        composeRule.onNodeWithText("次日即将生效").assertIsDisplayed()
        composeRule.onNodeWithText("短视频：改为弱限制，每日额度 60 分钟（2026-08-22 生效）")
            .assertIsDisplayed()
    }

    @Test
    fun scheduledRemovalLocksRepeatedAndConflictingRuleActions() {
        val current = RestrictedAppRule(
            "com.example.video",
            "短视频",
            RestrictionLevel.HARD,
            DailyAllowance.ofMinutes(30),
        )
        val controller = RestrictionSetupController(
            appCatalog = ScreenAppCatalog(emptyList()),
            ruleStore = ScreenRuleStore(current),
            clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC),
        )
        composeRule.setContent { RestrictionSetupRoute(controller, onBack = {}) }

        composeRule.onNodeWithText("次日移除限制").performClick()

        composeRule.onNodeWithText("已安排 2026-08-22 移除").assertIsNotEnabled()
        composeRule.onNodeWithText("规则已安排删除").assertIsNotEnabled()
    }

    @Test
    fun scheduledProtectionDisableCannotBeSubmittedTwice() {
        val controller = RestrictionSetupController(
            appCatalog = ScreenAppCatalog(emptyList()),
            ruleStore = ScreenRuleStore(),
            clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC),
        )
        composeRule.setContent { RestrictionSetupRoute(controller, onBack = {}) }

        composeRule.onNodeWithText("次日关闭保护").performClick()

        composeRule.onNodeWithText("已安排 2026-08-22 关闭保护").assertIsNotEnabled()
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.waitForText(text: String) {
    waitUntil(timeoutMillis = 3_000) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

private class ScreenAppCatalog(
    private val apps: List<InstalledApp>,
) : AppCatalog {
    override fun manageableApps(): List<InstalledApp> = apps
}

private class SlowScreenAppCatalog(
    private val delayMillis: Long,
    private val apps: List<InstalledApp>,
) : AppCatalog {
    override fun manageableApps(): List<InstalledApp> {
        Thread.sleep(delayMillis)
        return apps
    }
}

private class ScreenRuleStore(initialRule: RestrictedAppRule? = null) : RestrictionPolicyStore {
    private val rules = mutableListOf<RestrictedAppRule>().apply {
        if (initialRule != null) add(initialRule)
    }
    private val pending = mutableListOf<PendingRelaxation>()

    override fun loadRules(): List<RestrictedAppRule> = rules.toList()

    override fun saveRule(rule: RestrictedAppRule) {
        rules.removeAll { it.packageName == rule.packageName }
        rules.add(rule)
    }

    override fun loadPendingRelaxations(): List<PendingRelaxation> = pending.toList()

    override fun schedulePendingRelaxation(pendingRelaxation: PendingRelaxation) {
        pending.clear()
        pending.add(pendingRelaxation)
    }

    override fun cancelPendingRelaxation(packageName: String) {
        pending.removeAll { relaxation ->
            when (relaxation) {
                is PendingRelaxation.ReplaceRule -> relaxation.replacement.packageName == packageName
                is PendingRelaxation.RemoveRule -> relaxation.app.packageName == packageName
                is PendingRelaxation.DisableProtection -> false
            }
        }
    }

    override fun removeRule(packageName: String) {
        rules.removeAll { it.packageName == packageName }
    }

    override fun isProtectionEnabled(): Boolean = true

    override fun disableProtection() = Unit
}

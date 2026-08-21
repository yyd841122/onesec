package com.example.onesec

import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class InterventionIntegrationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun foregroundAccessibilityEventLaunchesTheVisibleProductionInterventionActivity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = Instant.parse("2026-08-21T04:00:00Z")
        val zoneId = ZoneId.of("Asia/Shanghai")
        val rule = RestrictedAppRule(
            packageName = "com.example.video",
            displayName = "短视频",
            level = RestrictionLevel.HARD,
            dailyAllowance = DailyAllowance.ofMinutes(30),
        )
        val presenter = CountingInterventionPresenter(AndroidInterventionPresenter(context))
        val monitor = ForegroundAppMonitor(
            ruleStore = IntegrationRuleStore(rule),
            usageLookup = TodayUsageLookup { _, _ -> 30 },
            protectionStatus = { true },
            exhaustedAllowances = IntegrationExhaustedAllowanceStore(),
            decisionEngine = DefaultRestrictionDecisionEngine,
            presenter = presenter,
            clock = Clock.fixed(now, zoneId),
        )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val handler = AndroidForegroundEventHandler(monitor)
            repeat(5) {
                val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
                event.packageName = rule.packageName
                handler.onAccessibilityEvent(event)
                event.recycle()
            }
        }

        org.junit.Assert.assertEquals(1, presenter.presentationCount)

        composeRule.waitUntilAtLeastOneExists(hasText("强限制已生效"), timeoutMillis = 5_000)
        composeRule.onNodeWithText("强限制已生效").assertIsDisplayed()
        composeRule.onNodeWithText("短视频").assertIsDisplayed()
        composeRule.onNodeWithText("今日已用 30 分钟").assertIsDisplayed()
        composeRule.onNodeWithText("下次重置：08月22日 00:00").assertIsDisplayed()
        composeRule.onNodeWithText("返回桌面").assertIsDisplayed()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<InterventionActivity>()
                .single()
            activity.onBackPressedDispatcher.onBackPressed()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            check(
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .none { it is InterventionActivity },
            )
        }
    }
}

private class CountingInterventionPresenter(
    private val delegate: InterventionPresenter,
) : InterventionPresenter {
    var presentationCount = 0
    override fun present(intervention: ProtectionDecision.Intervene) {
        presentationCount += 1
        delegate.present(intervention)
    }
}

private class IntegrationExhaustedAllowanceStore : ExhaustedAllowanceStore {
    override fun isExhausted(packageName: String, localDate: java.time.LocalDate) = false

    override fun markExhausted(packageName: String, localDate: java.time.LocalDate) = Unit
}

private class IntegrationRuleStore(
    private val rule: RestrictedAppRule,
) : RestrictionRuleStore {
    override fun loadRules() = listOf(rule)
    override fun saveRule(rule: RestrictedAppRule) = Unit
}

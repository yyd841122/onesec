package com.example.onesec

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import java.time.Instant
import java.time.ZoneId
import java.time.Clock
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EmergencyOverrideScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emergencyOverrideIsASecondaryActionRequiringWaitAndReason() {
        var confirmedReason: String? = null
        val clock = MutableTestClock(Instant.parse("2026-08-21T04:00:00Z"))
        val intervention = ProtectionDecision.Intervene(
            InstalledApp("com.example.video", "短视频"), 30,
            Instant.parse("2026-08-21T16:00:00Z"), RestrictionLevel.HARD,
        )
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            InterventionScreen(
                intervention, ZoneId.of("Asia/Shanghai"), {},
                emergencyOverrideAvailable = true,
                onEmergencyOverrideConfirmed = { confirmedReason = it },
                clock = clock,
            )
        }

        composeRule.onNodeWithText("需要紧急使用？").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(100)
        clock.now = clock.now.plusSeconds(60)
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithText("请填写紧急使用原因").assertIsDisplayed()
        composeRule.onNodeWithText("确认紧急解锁").assertIsNotEnabled()
        composeRule.onNodeWithText("原因").performTextInput("必要联系")
        composeRule.onNodeWithText("确认紧急解锁").performClick()

        assertEquals("必要联系", confirmedReason)
    }
}

private class MutableTestClock(var now: Instant) : Clock() {
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant() = now
}

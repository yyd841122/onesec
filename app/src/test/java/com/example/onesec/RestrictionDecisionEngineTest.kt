package com.example.onesec

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RestrictionDecisionEngineTest(
    private val caseName: String,
    private val usedMinutes: Int,
    private val protectionAvailable: Boolean,
    private val expectedDecision: ProtectionDecision,
) {
    private val rule = RestrictedAppRule(
        packageName = "com.example.video",
        displayName = "短视频",
        level = RestrictionLevel.HARD,
        dailyAllowance = DailyAllowance.ofMinutes(30),
    )

    @Test
    fun `returns the user-observable protection decision`() {
        val request = RestrictionDecisionRequest(
            now = Instant.parse("2026-08-21T04:00:00Z"),
            zoneId = ZoneId.of("Asia/Shanghai"),
            restrictedApp = InstalledApp(rule.packageName, rule.displayName),
            usedMinutes = usedMinutes,
            rule = rule,
            protectionAvailable = protectionAvailable,
        )

        assertEquals(caseName, expectedDecision, DefaultRestrictionDecisionEngine.decide(request))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = listOf(
            arrayOf("before allowance", 29, true, ProtectionDecision.Allow),
            arrayOf(
                "at allowance",
                30,
                true,
                ProtectionDecision.Intervene(
                    app = InstalledApp("com.example.video", "短视频"),
                    usedMinutes = 30,
                    resetsAt = Instant.parse("2026-08-21T16:00:00Z"),
                ),
            ),
            arrayOf(
                "after allowance",
                31,
                true,
                ProtectionDecision.Intervene(
                    app = InstalledApp("com.example.video", "短视频"),
                    usedMinutes = 31,
                    resetsAt = Instant.parse("2026-08-21T16:00:00Z"),
                ),
            ),
            arrayOf("permission unavailable", 31, false, ProtectionDecision.ProtectionUnavailable),
        )
    }
}

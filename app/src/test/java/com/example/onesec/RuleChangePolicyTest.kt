package com.example.onesec

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleChangePolicyTest {
    private val today = LocalDate.of(2026, 8, 21)
    private val app = InstalledApp("com.example.video", "短视频")

    @Test
    fun `new and stricter rules apply immediately`() {
        val newHardRule = rule(RestrictionLevel.HARD, 30)
        val lowerAllowance = rule(RestrictionLevel.HARD, 20)
        val hardFromSoft = rule(RestrictionLevel.HARD, 60)

        assertEquals(
            RuleChangeDecision.ApplyNow(newHardRule),
            decideRuleChange(RuleChange.Replace(current = null, replacement = newHardRule), today),
        )
        assertEquals(
            RuleChangeDecision.ApplyNow(lowerAllowance),
            decideRuleChange(
                RuleChange.Replace(current = rule(RestrictionLevel.HARD, 30), lowerAllowance),
                today,
            ),
        )
        assertEquals(
            RuleChangeDecision.ApplyNow(hardFromSoft),
            decideRuleChange(
                RuleChange.Replace(current = rule(RestrictionLevel.SOFT, 60), hardFromSoft),
                today,
            ),
        )
    }

    @Test
    fun `allowance increase removal and disabling protection wait until tomorrow`() {
        val current = rule(RestrictionLevel.HARD, 30)
        val tomorrow = LocalDate.of(2026, 8, 22)

        assertEquals(
            RuleChangeDecision.Schedule(
                PendingRelaxation.ReplaceRule(rule(RestrictionLevel.HARD, 45), tomorrow),
            ),
            decideRuleChange(
                RuleChange.Replace(current, rule(RestrictionLevel.HARD, 45)),
                today,
            ),
        )
        assertEquals(
            RuleChangeDecision.Schedule(PendingRelaxation.RemoveRule(app, tomorrow)),
            decideRuleChange(RuleChange.Remove(current), today),
        )
        assertEquals(
            RuleChangeDecision.Schedule(PendingRelaxation.DisableProtection(tomorrow)),
            decideRuleChange(RuleChange.DisableProtection, today),
        )
    }

    @Test
    fun `mixed changes tighten today and defer only the relaxation`() {
        val tomorrow = LocalDate.of(2026, 8, 22)
        val hardFortyFive = rule(RestrictionLevel.HARD, 45)
        assertEquals(
            RuleChangeDecision.ApplyNowAndSchedule(
                immediateRule = rule(RestrictionLevel.HARD, 30),
                pendingRelaxation = PendingRelaxation.ReplaceRule(hardFortyFive, tomorrow),
            ),
            decideRuleChange(
                RuleChange.Replace(rule(RestrictionLevel.SOFT, 30), hardFortyFive),
                today,
            ),
        )

        val softTwenty = rule(RestrictionLevel.SOFT, 20)
        assertEquals(
            RuleChangeDecision.ApplyNowAndSchedule(
                immediateRule = rule(RestrictionLevel.HARD, 20),
                pendingRelaxation = PendingRelaxation.ReplaceRule(softTwenty, tomorrow),
            ),
            decideRuleChange(
                RuleChange.Replace(rule(RestrictionLevel.HARD, 30), softTwenty),
                today,
            ),
        )
    }

    @Test
    fun `pending relaxation applies once when the clock crosses local midnight`() {
        val current = rule(RestrictionLevel.HARD, 30)
        val relaxed = rule(RestrictionLevel.HARD, 45)
        val pending = PendingRelaxation.ReplaceRule(relaxed, LocalDate.of(2026, 8, 22))

        val beforeMidnight = resolvePendingRelaxations(
            rules = listOf(current),
            protectionEnabled = true,
            pendingRelaxations = listOf(pending),
            onDate = LocalDate.of(2026, 8, 21),
        )
        assertEquals(listOf(current), beforeMidnight.rules)
        assertEquals(listOf(pending), beforeMidnight.pendingRelaxations)

        val afterMidnight = resolvePendingRelaxations(
            rules = beforeMidnight.rules,
            protectionEnabled = beforeMidnight.protectionEnabled,
            pendingRelaxations = beforeMidnight.pendingRelaxations,
            onDate = LocalDate.of(2026, 8, 22),
        )
        assertEquals(listOf(relaxed), afterMidnight.rules)
        assertEquals(emptyList<PendingRelaxation>(), afterMidnight.pendingRelaxations)

        assertEquals(
            afterMidnight,
            resolvePendingRelaxations(
                afterMidnight.rules,
                afterMidnight.protectionEnabled,
                afterMidnight.pendingRelaxations,
                LocalDate.of(2026, 8, 23),
            ),
        )
    }

    private fun rule(level: RestrictionLevel, minutes: Int) = RestrictedAppRule(
        packageName = app.packageName,
        displayName = app.displayName,
        level = level,
        dailyAllowance = DailyAllowance.ofMinutes(minutes),
    )
}

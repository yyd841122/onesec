package com.example.onesec

import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPreferencesRestrictionRuleStoreTest {
    @Test
    fun ruleIsRestoredFromASeparateStoreInstance() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "restriction_rule_store_test"
        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val expected = RestrictedAppRule(
            packageName = "com.example.video",
            displayName = "短视频",
            level = RestrictionLevel.HARD,
            dailyAllowance = DailyAllowance.ofMinutes(35),
        )

        SharedPreferencesRestrictionRuleStore(context, preferencesName).saveRule(expected)
        val restored = SharedPreferencesRestrictionRuleStore(context, preferencesName).loadRules()

        assertEquals(listOf(expected), restored)
        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun savingASecondRuleKeepsTheFirstRule() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "multiple_restriction_rules_test"
        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val store = SharedPreferencesRestrictionRuleStore(context, preferencesName)
        store.saveRule(
            RestrictedAppRule(
                packageName = "com.example.video",
                displayName = "短视频",
                level = RestrictionLevel.HARD,
                dailyAllowance = DailyAllowance.ofMinutes(30),
            ),
        )
        store.saveRule(
            RestrictedAppRule(
                packageName = "com.example.game",
                displayName = "游戏",
                level = RestrictionLevel.HARD,
                dailyAllowance = DailyAllowance.ofMinutes(45),
            ),
        )

        val restored = SharedPreferencesRestrictionRuleStore(context, preferencesName).loadRules()

        assertEquals(setOf("com.example.video", "com.example.game"), restored.map { it.packageName }.toSet())
        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun pendingAllowanceIncreaseSurvivesReconstructionAndAppliesExactlyOnceOnTheNextDate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "pending_relaxation_store_test"
        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        val current = RestrictedAppRule(
            "com.example.video",
            "短视频",
            RestrictionLevel.HARD,
            DailyAllowance.ofMinutes(30),
        )
        val relaxed = current.copy(dailyAllowance = DailyAllowance.ofMinutes(45))
        val effectiveDate = LocalDate.of(2026, 8, 22)
        val beforeMidnight = Clock.fixed(Instant.parse("2026-08-21T15:59:59Z"), ZoneOffset.ofHours(8))
        val afterMidnight = Clock.fixed(Instant.parse("2026-08-21T16:00:01Z"), ZoneOffset.ofHours(8))

        SharedPreferencesRestrictionRuleStore(context, preferencesName, beforeMidnight).apply {
            saveRule(current)
            schedulePendingRelaxation(PendingRelaxation.ReplaceRule(relaxed, effectiveDate))
        }
        val beforeRestart = SharedPreferencesRestrictionRuleStore(context, preferencesName, beforeMidnight)
        assertEquals(listOf(current), beforeRestart.loadRules())
        assertEquals(1, beforeRestart.loadPendingRelaxations().size)

        val afterRestart = SharedPreferencesRestrictionRuleStore(context, preferencesName, afterMidnight)
        assertEquals(listOf(relaxed), afterRestart.loadRules())
        assertEquals(emptyList<PendingRelaxation>(), afterRestart.loadPendingRelaxations())
        assertEquals(listOf(relaxed), afterRestart.loadRules())

        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }
}

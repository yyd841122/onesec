package com.example.onesec

import androidx.test.core.app.ApplicationProvider
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
}

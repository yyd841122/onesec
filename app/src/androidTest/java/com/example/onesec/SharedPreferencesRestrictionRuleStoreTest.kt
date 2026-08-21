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
        val restored = SharedPreferencesRestrictionRuleStore(context, preferencesName).loadRule()

        assertEquals(expected, restored)
        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

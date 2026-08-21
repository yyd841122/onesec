package com.example.onesec

import android.content.Context

class SharedPreferencesRestrictionRuleStore(
    context: Context,
    preferencesName: String = DEFAULT_FILE_NAME,
) : RestrictionRuleStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun loadRule(): RestrictedAppRule? {
        val packageName = preferences.getString(KEY_PACKAGE_NAME, null) ?: return null
        val displayName = preferences.getString(KEY_DISPLAY_NAME, null) ?: return null
        val allowance = preferences.getInt(KEY_DAILY_ALLOWANCE, 0)
        if (allowance <= 0) return null
        val level = preferences.getString(KEY_RESTRICTION_LEVEL, null)
            ?.let { storedLevel -> RestrictionLevel.entries.find { it.name == storedLevel } }
            ?: return null

        return RestrictedAppRule(
            packageName = packageName,
            displayName = displayName,
            level = level,
            dailyAllowance = DailyAllowance.ofMinutes(allowance),
        )
    }

    override fun saveRule(rule: RestrictedAppRule) {
        preferences.edit()
            .putString(KEY_PACKAGE_NAME, rule.packageName)
            .putString(KEY_DISPLAY_NAME, rule.displayName)
            .putString(KEY_RESTRICTION_LEVEL, rule.level.name)
            .putInt(KEY_DAILY_ALLOWANCE, rule.dailyAllowance.minutes)
            .commit()
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "restriction_rules"
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_RESTRICTION_LEVEL = "restriction_level"
        const val KEY_DAILY_ALLOWANCE = "daily_allowance_minutes"
    }
}

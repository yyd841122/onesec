package com.example.onesec

import android.content.Context

class SharedPreferencesRestrictionRuleStore(
    context: Context,
    preferencesName: String = DEFAULT_FILE_NAME,
) : RestrictionRuleStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun loadRules(): List<RestrictedAppRule> {
        val storedRules = preferences.getStringSet(KEY_RULE_PACKAGES, emptySet()).orEmpty()
            .mapNotNull(::loadStoredRule)
            .sortedBy(RestrictedAppRule::displayName)
        if (storedRules.isNotEmpty()) return storedRules

        return listOfNotNull(loadLegacyRule())
    }

    private fun loadStoredRule(packageName: String): RestrictedAppRule? {
        val prefix = rulePrefix(packageName)
        val displayName = preferences.getString(prefix + DISPLAY_NAME_SUFFIX, null) ?: return null
        val allowance = preferences.getInt(prefix + DAILY_ALLOWANCE_SUFFIX, 0)
        if (allowance <= 0) return null
        val level = preferences.getString(prefix + RESTRICTION_LEVEL_SUFFIX, null)
            ?.let { storedLevel -> RestrictionLevel.entries.find { it.name == storedLevel } }
            ?: return null

        return RestrictedAppRule(
            packageName = packageName,
            displayName = displayName,
            level = level,
            dailyAllowance = DailyAllowance.ofMinutes(allowance),
        )
    }

    private fun loadLegacyRule(): RestrictedAppRule? {
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
        val rules = loadRules()
            .filterNot { it.packageName == rule.packageName }
            .plus(rule)
        val previousPackages = preferences.getStringSet(KEY_RULE_PACKAGES, emptySet()).orEmpty()
        val editor = preferences.edit()
            .putStringSet(KEY_RULE_PACKAGES, rules.mapTo(mutableSetOf()) { it.packageName })

        (previousPackages - rules.map { it.packageName }.toSet()).forEach { removedPackage ->
            val prefix = rulePrefix(removedPackage)
            editor
                .remove(prefix + DISPLAY_NAME_SUFFIX)
                .remove(prefix + RESTRICTION_LEVEL_SUFFIX)
                .remove(prefix + DAILY_ALLOWANCE_SUFFIX)
        }
        rules.forEach { storedRule ->
            val prefix = rulePrefix(storedRule.packageName)
            editor
                .putString(prefix + DISPLAY_NAME_SUFFIX, storedRule.displayName)
                .putString(prefix + RESTRICTION_LEVEL_SUFFIX, storedRule.level.name)
                .putInt(prefix + DAILY_ALLOWANCE_SUFFIX, storedRule.dailyAllowance.minutes)
        }
        editor
            .remove(KEY_PACKAGE_NAME)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_RESTRICTION_LEVEL)
            .remove(KEY_DAILY_ALLOWANCE)
            .commit()
    }

    private fun rulePrefix(packageName: String) = "rule.$packageName."

    private companion object {
        const val DEFAULT_FILE_NAME = "restriction_rules"
        const val KEY_RULE_PACKAGES = "rule_packages"
        const val DISPLAY_NAME_SUFFIX = "display_name"
        const val RESTRICTION_LEVEL_SUFFIX = "restriction_level"
        const val DAILY_ALLOWANCE_SUFFIX = "daily_allowance_minutes"
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_RESTRICTION_LEVEL = "restriction_level"
        const val KEY_DAILY_ALLOWANCE = "daily_allowance_minutes"
    }
}

package com.example.onesec

import android.content.Context
import android.content.SharedPreferences
import java.time.Clock
import java.time.LocalDate

class SharedPreferencesRestrictionRuleStore(
    context: Context,
    preferencesName: String = DEFAULT_FILE_NAME,
    private val clock: Clock = Clock.systemDefaultZone(),
) : RestrictionPolicyStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun loadRules(): List<RestrictedAppRule> {
        applyDuePendingRelaxations()
        if (!preferences.getBoolean(KEY_PROTECTION_ENABLED, true)) return emptyList()
        return loadStoredRules()
    }

    private fun loadStoredRules(): List<RestrictedAppRule> {
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

    override fun loadPendingRelaxations(): List<PendingRelaxation> {
        applyDuePendingRelaxations()
        return loadPendingRelaxationsWithoutApplying().sortedBy { it.effectiveDate }
    }

    override fun schedulePendingRelaxation(pendingRelaxation: PendingRelaxation) {
        when (pendingRelaxation) {
            is PendingRelaxation.ReplaceRule -> {
                val rule = pendingRelaxation.replacement
                val prefix = pendingPrefix(rule.packageName)
                val packages = preferences.getStringSet(KEY_PENDING_PACKAGES, emptySet()).orEmpty() +
                    rule.packageName
                preferences.edit()
                    .putStringSet(KEY_PENDING_PACKAGES, packages)
                    .putString(prefix + PENDING_TYPE_SUFFIX, PendingType.REPLACE.name)
                    .putLong(prefix + EFFECTIVE_DATE_SUFFIX, pendingRelaxation.effectiveDate.toEpochDay())
                    .putString(prefix + DISPLAY_NAME_SUFFIX, rule.displayName)
                    .putString(prefix + RESTRICTION_LEVEL_SUFFIX, rule.level.name)
                    .putInt(prefix + DAILY_ALLOWANCE_SUFFIX, rule.dailyAllowance.minutes)
                    .commit()
            }
            is PendingRelaxation.RemoveRule -> {
                val prefix = pendingPrefix(pendingRelaxation.app.packageName)
                val packages = preferences.getStringSet(KEY_PENDING_PACKAGES, emptySet()).orEmpty() +
                    pendingRelaxation.app.packageName
                preferences.edit()
                    .putStringSet(KEY_PENDING_PACKAGES, packages)
                    .putString(prefix + PENDING_TYPE_SUFFIX, PendingType.REMOVE.name)
                    .putLong(prefix + EFFECTIVE_DATE_SUFFIX, pendingRelaxation.effectiveDate.toEpochDay())
                    .putString(prefix + DISPLAY_NAME_SUFFIX, pendingRelaxation.app.displayName)
                    .commit()
            }
            is PendingRelaxation.DisableProtection -> preferences.edit()
                .putLong(KEY_DISABLE_PROTECTION_ON, pendingRelaxation.effectiveDate.toEpochDay())
                .commit()
        }
    }

    override fun cancelPendingRelaxation(packageName: String) {
        clearPendingRelaxationForPackage(packageName)
    }

    private fun clearPendingRelaxationForPackage(packageName: String) {
        val packages = preferences.getStringSet(KEY_PENDING_PACKAGES, emptySet()).orEmpty() - packageName
        val prefix = pendingPrefix(packageName)
        preferences.edit()
            .putStringSet(KEY_PENDING_PACKAGES, packages)
            .remove(prefix + PENDING_TYPE_SUFFIX)
            .remove(prefix + EFFECTIVE_DATE_SUFFIX)
            .remove(prefix + DISPLAY_NAME_SUFFIX)
            .remove(prefix + RESTRICTION_LEVEL_SUFFIX)
            .remove(prefix + DAILY_ALLOWANCE_SUFFIX)
            .commit()
    }

    override fun removeRule(packageName: String) {
        val rules = loadStoredRules().filterNot { it.packageName == packageName }
        val prefix = rulePrefix(packageName)
        preferences.edit()
            .putStringSet(KEY_RULE_PACKAGES, rules.mapTo(mutableSetOf()) { it.packageName })
            .remove(prefix + DISPLAY_NAME_SUFFIX)
            .remove(prefix + RESTRICTION_LEVEL_SUFFIX)
            .remove(prefix + DAILY_ALLOWANCE_SUFFIX)
            .commit()
    }

    override fun isProtectionEnabled(): Boolean {
        applyDuePendingRelaxations()
        return preferences.getBoolean(KEY_PROTECTION_ENABLED, true)
    }

    override fun disableProtection() {
        preferences.edit().putBoolean(KEY_PROTECTION_ENABLED, false).commit()
    }

    private fun applyDuePendingRelaxations() {
        val today = LocalDate.now(clock)
        val pending = loadPendingRelaxationsWithoutApplying()
        val currentRules = loadStoredRules()
        val protectionEnabled = preferences.getBoolean(KEY_PROTECTION_ENABLED, true)
        val resolved = resolvePendingRelaxations(
            rules = currentRules,
            protectionEnabled = protectionEnabled,
            pendingRelaxations = pending,
            onDate = today,
        )
        if (pending.none { !it.effectiveDate.isAfter(today) }) return

        val editor = preferences.edit()
        replaceStoredRules(resolved.rules, editor)
        replacePendingRelaxations(pending, resolved.pendingRelaxations, editor)
        editor
            .putBoolean(KEY_PROTECTION_ENABLED, resolved.protectionEnabled)
            .commit()
    }

    private fun loadPendingRelaxationsWithoutApplying(): List<PendingRelaxation> {
        val changes = preferences.getStringSet(KEY_PENDING_PACKAGES, emptySet()).orEmpty()
            .mapNotNull(::loadPendingRuleChange)
        val disable = preferences.getLong(KEY_DISABLE_PROTECTION_ON, NO_DATE)
            .takeUnless { it == NO_DATE }
            ?.let(LocalDate::ofEpochDay)
            ?.let(PendingRelaxation::DisableProtection)
        return changes + listOfNotNull(disable)
    }

    private fun loadPendingRuleChange(packageName: String): PendingRelaxation? {
        val prefix = pendingPrefix(packageName)
        val effectiveDate = preferences.getLong(prefix + EFFECTIVE_DATE_SUFFIX, NO_DATE)
            .takeUnless { it == NO_DATE }
            ?.let(LocalDate::ofEpochDay)
            ?: return null
        val displayName = preferences.getString(prefix + DISPLAY_NAME_SUFFIX, null) ?: return null
        return when (preferences.getString(prefix + PENDING_TYPE_SUFFIX, null)) {
            PendingType.REMOVE.name -> PendingRelaxation.RemoveRule(
                InstalledApp(packageName, displayName),
                effectiveDate,
            )
            PendingType.REPLACE.name -> {
                val level = preferences.getString(prefix + RESTRICTION_LEVEL_SUFFIX, null)
                    ?.let { stored -> RestrictionLevel.entries.find { it.name == stored } }
                    ?: return null
                val allowance = preferences.getInt(prefix + DAILY_ALLOWANCE_SUFFIX, 0)
                if (allowance <= 0) return null
                PendingRelaxation.ReplaceRule(
                    RestrictedAppRule(
                        packageName,
                        displayName,
                        level,
                        DailyAllowance.ofMinutes(allowance),
                    ),
                    effectiveDate,
                )
            }
            else -> null
        }
    }

    private fun replaceStoredRules(
        rules: List<RestrictedAppRule>,
        editor: SharedPreferences.Editor,
    ) {
        val previousPackages = preferences.getStringSet(KEY_RULE_PACKAGES, emptySet()).orEmpty()
        val packages = rules.mapTo(mutableSetOf()) { it.packageName }
        editor.putStringSet(KEY_RULE_PACKAGES, packages)
        (previousPackages - packages).forEach { packageName ->
            val prefix = rulePrefix(packageName)
            editor
                .remove(prefix + DISPLAY_NAME_SUFFIX)
                .remove(prefix + RESTRICTION_LEVEL_SUFFIX)
                .remove(prefix + DAILY_ALLOWANCE_SUFFIX)
        }
        rules.forEach { rule ->
            val prefix = rulePrefix(rule.packageName)
            editor
                .putString(prefix + DISPLAY_NAME_SUFFIX, rule.displayName)
                .putString(prefix + RESTRICTION_LEVEL_SUFFIX, rule.level.name)
                .putInt(prefix + DAILY_ALLOWANCE_SUFFIX, rule.dailyAllowance.minutes)
        }
    }

    private fun replacePendingRelaxations(
        previous: List<PendingRelaxation>,
        remaining: List<PendingRelaxation>,
        editor: SharedPreferences.Editor,
    ) {
        previous.mapNotNull { it.pendingPackageName }.toSet().forEach { packageName ->
            val prefix = pendingPrefix(packageName)
            editor
                .remove(prefix + PENDING_TYPE_SUFFIX)
                .remove(prefix + EFFECTIVE_DATE_SUFFIX)
                .remove(prefix + DISPLAY_NAME_SUFFIX)
                .remove(prefix + RESTRICTION_LEVEL_SUFFIX)
                .remove(prefix + DAILY_ALLOWANCE_SUFFIX)
        }
        editor
            .putStringSet(
                KEY_PENDING_PACKAGES,
                remaining.mapNotNullTo(mutableSetOf()) { it.pendingPackageName },
            )
            .remove(KEY_DISABLE_PROTECTION_ON)
        remaining.forEach { pending ->
            when (pending) {
                is PendingRelaxation.ReplaceRule -> {
                    val prefix = pendingPrefix(pending.replacement.packageName)
                    editor
                        .putString(prefix + PENDING_TYPE_SUFFIX, PendingType.REPLACE.name)
                        .putLong(prefix + EFFECTIVE_DATE_SUFFIX, pending.effectiveDate.toEpochDay())
                        .putString(prefix + DISPLAY_NAME_SUFFIX, pending.replacement.displayName)
                        .putString(prefix + RESTRICTION_LEVEL_SUFFIX, pending.replacement.level.name)
                        .putInt(prefix + DAILY_ALLOWANCE_SUFFIX, pending.replacement.dailyAllowance.minutes)
                }
                is PendingRelaxation.RemoveRule -> {
                    val prefix = pendingPrefix(pending.app.packageName)
                    editor
                        .putString(prefix + PENDING_TYPE_SUFFIX, PendingType.REMOVE.name)
                        .putLong(prefix + EFFECTIVE_DATE_SUFFIX, pending.effectiveDate.toEpochDay())
                        .putString(prefix + DISPLAY_NAME_SUFFIX, pending.app.displayName)
                }
                is PendingRelaxation.DisableProtection -> editor.putLong(
                    KEY_DISABLE_PROTECTION_ON,
                    pending.effectiveDate.toEpochDay(),
                )
            }
        }
    }

    private val PendingRelaxation.pendingPackageName: String?
        get() = when (this) {
            is PendingRelaxation.ReplaceRule -> replacement.packageName
            is PendingRelaxation.RemoveRule -> app.packageName
            is PendingRelaxation.DisableProtection -> null
        }

    private fun clearPendingRelaxation(pending: PendingRelaxation) {
        if (pending is PendingRelaxation.DisableProtection) {
            preferences.edit().remove(KEY_DISABLE_PROTECTION_ON).commit()
            return
        }
        val packageName = when (pending) {
            is PendingRelaxation.ReplaceRule -> pending.replacement.packageName
            is PendingRelaxation.RemoveRule -> pending.app.packageName
            is PendingRelaxation.DisableProtection -> error("handled above")
        }
        clearPendingRelaxationForPackage(packageName)
    }

    private fun rulePrefix(packageName: String) = "rule.$packageName."

    private fun pendingPrefix(packageName: String) = "pending.$packageName."

    private enum class PendingType { REPLACE, REMOVE }

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
        const val KEY_PENDING_PACKAGES = "pending_packages"
        const val PENDING_TYPE_SUFFIX = "type"
        const val EFFECTIVE_DATE_SUFFIX = "effective_date"
        const val KEY_DISABLE_PROTECTION_ON = "disable_protection_on"
        const val KEY_PROTECTION_ENABLED = "protection_enabled"
        const val NO_DATE = Long.MIN_VALUE
    }
}

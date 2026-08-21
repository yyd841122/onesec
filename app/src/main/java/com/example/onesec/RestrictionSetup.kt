package com.example.onesec

import java.time.Clock
import java.time.LocalDate

data class InstalledApp(
    val packageName: String,
    val displayName: String,
    val iconPng: ByteArray? = null,
)

data class AppCandidate(
    val packageName: String,
    val displayName: String,
    val isSystemApp: Boolean,
    val isHomeApp: Boolean,
    val iconPng: ByteArray? = null,
)

data class DailyAllowance private constructor(
    val minutes: Int,
) {
    companion object {
        val DEFAULT_HARD = DailyAllowance(30)
        val DEFAULT_SOFT = DailyAllowance(60)

        fun ofMinutes(minutes: Int): DailyAllowance = DailyAllowance(minutes.coerceIn(5, 1_440))
    }
}

fun manageableApps(
    candidates: List<AppCandidate>,
    ownPackageName: String,
): List<InstalledApp> =
    candidates
        .filterNot { candidate ->
            candidate.packageName == ownPackageName ||
                candidate.isSystemApp ||
                candidate.isHomeApp
        }
        .distinctBy(AppCandidate::packageName)
        .map { candidate ->
            InstalledApp(
                packageName = candidate.packageName,
                displayName = candidate.displayName,
                iconPng = candidate.iconPng,
            )
        }
        .sortedBy(InstalledApp::displayName)

enum class RestrictionLevel {
    SOFT,
    HARD,
}

data class RestrictedAppRule(
    val packageName: String,
    val displayName: String,
    val level: RestrictionLevel,
    val dailyAllowance: DailyAllowance,
)

data class RestrictionEditorState(
    val app: InstalledApp,
    val level: RestrictionLevel = RestrictionLevel.HARD,
    val dailyAllowance: DailyAllowance = DailyAllowance.DEFAULT_HARD,
)

data class RestrictionSetupState(
    val apps: List<InstalledApp> = emptyList(),
    val savedRules: List<RestrictedAppRule> = emptyList(),
    val editor: RestrictionEditorState? = null,
    val pendingRelaxations: List<PendingRelaxation> = emptyList(),
    val protectionEnabled: Boolean = true,
)

interface AppCatalog {
    fun manageableApps(): List<InstalledApp>
}

interface RestrictionRuleStore {
    fun loadRules(): List<RestrictedAppRule>

    fun saveRule(rule: RestrictedAppRule)
}

interface RestrictionPolicyStore : RestrictionRuleStore {
    fun loadPendingRelaxations(): List<PendingRelaxation>

    fun schedulePendingRelaxation(pendingRelaxation: PendingRelaxation)

    fun cancelPendingRelaxation(packageName: String)

    fun removeRule(packageName: String)

    fun isProtectionEnabled(): Boolean

    fun disableProtection()
}

class RestrictionSetupController(
    private val appCatalog: AppCatalog,
    private val ruleStore: RestrictionPolicyStore,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onRuleSaved: () -> Unit = {},
) {
    var state = readState()
        private set

    fun openAppCatalog() {
        state = state.copy(apps = appCatalog.manageableApps(), editor = null)
    }

    fun selectApp(packageName: String) {
        val app = state.apps.firstOrNull { it.packageName == packageName } ?: return
        val current = state.savedRules.firstOrNull { it.packageName == packageName }
        state = state.copy(
            editor = RestrictionEditorState(
                app = app,
                level = current?.level ?: RestrictionLevel.HARD,
                dailyAllowance = current?.dailyAllowance ?: DailyAllowance.DEFAULT_HARD,
            ),
        )
    }

    fun changeDailyAllowance(minutes: Int) {
        val editor = state.editor ?: return
        state = state.copy(editor = editor.copy(dailyAllowance = DailyAllowance.ofMinutes(minutes)))
    }

    fun changeRestrictionLevel(level: RestrictionLevel) {
        val editor = state.editor ?: return
        val defaultAllowance = when (level) {
            RestrictionLevel.SOFT -> DailyAllowance.DEFAULT_SOFT
            RestrictionLevel.HARD -> DailyAllowance.DEFAULT_HARD
        }
        state = state.copy(editor = editor.copy(level = level, dailyAllowance = defaultAllowance))
    }

    fun saveRule() {
        val editor = state.editor ?: return
        val rule = RestrictedAppRule(
            packageName = editor.app.packageName,
            displayName = editor.app.displayName,
            level = editor.level,
            dailyAllowance = editor.dailyAllowance,
        )
        when (
            val decision = decideRuleChange(
                RuleChange.Replace(
                    current = state.savedRules.firstOrNull { it.packageName == rule.packageName },
                    replacement = rule,
                ),
                LocalDate.now(clock),
            )
        ) {
            is RuleChangeDecision.ApplyNow -> {
                ruleStore.cancelPendingRelaxation(decision.rule.packageName)
                ruleStore.saveRule(decision.rule)
            }
            is RuleChangeDecision.Schedule ->
                ruleStore.schedulePendingRelaxation(decision.pendingRelaxation)
            is RuleChangeDecision.ApplyNowAndSchedule -> {
                ruleStore.saveRule(decision.immediateRule)
                ruleStore.schedulePendingRelaxation(decision.pendingRelaxation)
            }
        }
        onRuleSaved()
        state = readState(apps = state.apps)
    }

    fun cancelSelection() {
        state = state.copy(editor = null)
    }

    fun tightenToHardRestriction(packageName: String) {
        val current = state.savedRules.firstOrNull { it.packageName == packageName } ?: return
        val replacement = current.copy(level = RestrictionLevel.HARD)
        when (val decision = decideRuleChange(RuleChange.Replace(current, replacement), LocalDate.now(clock))) {
            is RuleChangeDecision.ApplyNow -> {
                ruleStore.cancelPendingRelaxation(decision.rule.packageName)
                ruleStore.saveRule(decision.rule)
            }
            is RuleChangeDecision.Schedule -> ruleStore.schedulePendingRelaxation(decision.pendingRelaxation)
            is RuleChangeDecision.ApplyNowAndSchedule -> {
                ruleStore.saveRule(decision.immediateRule)
                ruleStore.schedulePendingRelaxation(decision.pendingRelaxation)
            }
        }
        state = readState(apps = state.apps)
        onRuleSaved()
    }

    fun removeRule(packageName: String) {
        val current = state.savedRules.firstOrNull { it.packageName == packageName } ?: return
        val decision = decideRuleChange(RuleChange.Remove(current), LocalDate.now(clock))
        if (decision is RuleChangeDecision.Schedule) {
            ruleStore.schedulePendingRelaxation(decision.pendingRelaxation)
        }
        state = readState(apps = state.apps)
    }

    fun disableProtection() {
        val decision = decideRuleChange(RuleChange.DisableProtection, LocalDate.now(clock))
        if (decision is RuleChangeDecision.Schedule) {
            ruleStore.schedulePendingRelaxation(decision.pendingRelaxation)
        }
        state = readState(apps = state.apps)
    }

    fun refresh() {
        state = readState(apps = state.apps)
    }

    private fun readState(apps: List<InstalledApp> = emptyList()) = RestrictionSetupState(
        apps = apps,
        savedRules = ruleStore.loadRules(),
        pendingRelaxations = ruleStore.loadPendingRelaxations(),
        protectionEnabled = ruleStore.isProtectionEnabled(),
    )
}

package com.example.onesec

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
    val savedRule: RestrictedAppRule? = null,
    val editor: RestrictionEditorState? = null,
)

interface AppCatalog {
    fun manageableApps(): List<InstalledApp>
}

interface RestrictionRuleStore {
    fun loadRule(): RestrictedAppRule?

    fun saveRule(rule: RestrictedAppRule)
}

class RestrictionSetupController(
    private val appCatalog: AppCatalog,
    private val ruleStore: RestrictionRuleStore,
) {
    var state = RestrictionSetupState(savedRule = ruleStore.loadRule())
        private set

    fun openAppCatalog() {
        state = state.copy(apps = appCatalog.manageableApps(), editor = null)
    }

    fun selectApp(packageName: String) {
        val app = state.apps.firstOrNull { it.packageName == packageName } ?: return
        state = state.copy(editor = RestrictionEditorState(app))
    }

    fun changeDailyAllowance(minutes: Int) {
        val editor = state.editor ?: return
        state = state.copy(editor = editor.copy(dailyAllowance = DailyAllowance.ofMinutes(minutes)))
    }

    fun saveRule() {
        val editor = state.editor ?: return
        val rule = RestrictedAppRule(
            packageName = editor.app.packageName,
            displayName = editor.app.displayName,
            level = editor.level,
            dailyAllowance = editor.dailyAllowance,
        )
        ruleStore.saveRule(rule)
        state = state.copy(savedRule = rule, editor = null)
    }
}

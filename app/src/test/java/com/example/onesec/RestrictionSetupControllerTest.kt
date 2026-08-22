package com.example.onesec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RestrictionSetupControllerTest {
    @Test
    fun `existing rule can be opened directly for editing without the app catalog`() {
        val current = RestrictedAppRule(
            "com.example.video",
            "短视频",
            RestrictionLevel.HARD,
            DailyAllowance.ofMinutes(30),
        )
        val controller = RestrictionSetupController(
            appCatalog = FakeAppCatalog(emptyList()),
            ruleStore = FakeRestrictionRuleStore(current),
        )

        controller.editRule(current.packageName)

        assertEquals(current.level, controller.state.editor?.level)
        assertEquals(current.dailyAllowance, controller.state.editor?.dailyAllowance)
        assertEquals(current.packageName, controller.state.editor?.app?.packageName)
    }

    @Test
    fun `user can choose a soft restriction with its sixty minute default`() {
        val controller = RestrictionSetupController(
            FakeAppCatalog(listOf(InstalledApp("com.example.video", "短视频"))),
            FakeRestrictionRuleStore(),
        )

        controller.openAppCatalog()
        controller.selectApp("com.example.video")
        controller.changeRestrictionLevel(RestrictionLevel.SOFT)

        assertEquals(RestrictionLevel.SOFT, controller.state.editor?.level)
        assertEquals(DailyAllowance.ofMinutes(60), controller.state.editor?.dailyAllowance)
    }
    @Test
    fun `catalog excludes OneSec and unsuitable system entry points`() {
        val candidates = listOf(
            AppCandidate("com.example.onesec", "OneSec", isSystemApp = false, isHomeApp = false),
            AppCandidate("com.android.settings", "设置", isSystemApp = true, isHomeApp = false),
            AppCandidate("com.example.launcher", "桌面", isSystemApp = false, isHomeApp = true),
            AppCandidate("com.example.video", "短视频", isSystemApp = false, isHomeApp = false),
        )

        val manageable = manageableApps(candidates, ownPackageName = "com.example.onesec")

        assertEquals(listOf("com.example.video"), manageable.map { it.packageName })
    }

    @Test
    fun `user selects an installed app adjusts the default allowance and saves a hard restriction`() {
        val catalog = FakeAppCatalog(
            listOf(InstalledApp("com.example.video", "短视频")),
        )
        val rules = FakeRestrictionRuleStore()
        var recoveryRequests = 0
        val controller = RestrictionSetupController(catalog, rules) { recoveryRequests += 1 }

        controller.openAppCatalog()
        assertEquals(listOf("短视频"), controller.state.apps.map { it.displayName })

        controller.selectApp("com.example.video")
        assertEquals(DailyAllowance.DEFAULT_HARD, controller.state.editor?.dailyAllowance)
        assertEquals(RestrictionLevel.HARD, controller.state.editor?.level)

        controller.changeDailyAllowance(45)
        controller.saveRule()

        assertEquals(
            RestrictedAppRule(
                packageName = "com.example.video",
                displayName = "短视频",
                level = RestrictionLevel.HARD,
                dailyAllowance = DailyAllowance.ofMinutes(45),
            ),
            rules.savedRules.single(),
        )
        assertEquals(rules.savedRules, controller.state.savedRules)
        assertNull(controller.state.editor)
        assertEquals(1, recoveryRequests)
    }

    @Test
    fun `saved restriction is restored when OneSec starts again`() {
        val savedRule = RestrictedAppRule(
            packageName = "com.example.game",
            displayName = "游戏",
            level = RestrictionLevel.HARD,
            dailyAllowance = DailyAllowance.ofMinutes(25),
        )

        val restartedController = RestrictionSetupController(
            FakeAppCatalog(emptyList()),
            FakeRestrictionRuleStore(savedRule),
        )

        assertEquals(listOf(savedRule), restartedController.state.savedRules)
    }

    @Test
    fun `saving another app keeps both restrictions and a new selection can be cancelled`() {
        val catalog = FakeAppCatalog(
            listOf(
                InstalledApp("com.example.video", "短视频"),
                InstalledApp("com.example.game", "游戏"),
            ),
        )
        val controller = RestrictionSetupController(catalog, FakeRestrictionRuleStore())

        controller.openAppCatalog()
        controller.selectApp("com.example.video")
        controller.saveRule()
        controller.openAppCatalog()
        controller.selectApp("com.example.game")
        controller.saveRule()

        assertEquals(
            listOf("com.example.video", "com.example.game"),
            controller.state.savedRules.map { it.packageName },
        )

        controller.openAppCatalog()
        controller.selectApp("com.example.video")
        controller.cancelSelection()

        assertEquals(
            listOf("com.example.video", "com.example.game"),
            controller.state.savedRules.map { it.packageName },
        )
        assertNull(controller.state.editor)
    }

    @Test
    fun `increasing allowance keeps current rule and shows tomorrow pending relaxation`() {
        val current = RestrictedAppRule(
            "com.example.video",
            "短视频",
            RestrictionLevel.HARD,
            DailyAllowance.ofMinutes(30),
        )
        val rules = FakeRestrictionRuleStore(current)
        val controller = RestrictionSetupController(
            appCatalog = FakeAppCatalog(listOf(InstalledApp(current.packageName, current.displayName))),
            ruleStore = rules,
            clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC),
        )

        controller.openAppCatalog()
        controller.selectApp(current.packageName)
        controller.changeDailyAllowance(45)
        controller.saveRule()

        assertEquals(listOf(current), controller.state.savedRules)
        assertEquals(
            listOf(
                PendingRelaxation.ReplaceRule(
                    current.copy(dailyAllowance = DailyAllowance.ofMinutes(45)),
                    java.time.LocalDate.of(2026, 8, 22),
                ),
            ),
            controller.state.pendingRelaxations,
        )
    }

    @Test
    fun `removing hard restriction and disabling protection stay pending today`() {
        val current = RestrictedAppRule(
            "com.example.video",
            "短视频",
            RestrictionLevel.HARD,
            DailyAllowance.ofMinutes(30),
        )
        val rules = FakeRestrictionRuleStore(current)
        val controller = RestrictionSetupController(
            appCatalog = FakeAppCatalog(emptyList()),
            ruleStore = rules,
            clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC),
        )

        controller.removeRule(current.packageName)
        controller.disableProtection()

        assertEquals(listOf(current), controller.state.savedRules)
        assertEquals(2, controller.state.pendingRelaxations.size)
        assertEquals(true, controller.state.protectionEnabled)
    }

    @Test
    fun `immediate tightening cancels an older pending relaxation for the same app`() {
        val current = RestrictedAppRule(
            "com.example.video",
            "短视频",
            RestrictionLevel.HARD,
            DailyAllowance.ofMinutes(30),
        )
        val rules = FakeRestrictionRuleStore(current)
        val controller = RestrictionSetupController(
            appCatalog = FakeAppCatalog(listOf(InstalledApp(current.packageName, current.displayName))),
            ruleStore = rules,
        )

        controller.openAppCatalog()
        controller.selectApp(current.packageName)
        controller.changeDailyAllowance(45)
        controller.saveRule()
        controller.openAppCatalog()
        controller.selectApp(current.packageName)
        controller.changeDailyAllowance(20)
        controller.saveRule()

        assertEquals(20, controller.state.savedRules.single().dailyAllowance.minutes)
        assertEquals(emptyList<PendingRelaxation>(), controller.state.pendingRelaxations)
    }
}

private class FakeAppCatalog(
    private val apps: List<InstalledApp>,
) : AppCatalog {
    override fun manageableApps(): List<InstalledApp> = apps
}

private class FakeRestrictionRuleStore(
    initialRule: RestrictedAppRule? = null,
) : RestrictionPolicyStore {
    val savedRules = mutableListOf<RestrictedAppRule>().apply {
        if (initialRule != null) add(initialRule)
    }
    val pendingRelaxations = mutableListOf<PendingRelaxation>()

    override fun loadRules(): List<RestrictedAppRule> = savedRules.toList()

    override fun saveRule(rule: RestrictedAppRule) {
        savedRules.removeAll { it.packageName == rule.packageName }
        savedRules.add(rule)
    }

    override fun loadPendingRelaxations(): List<PendingRelaxation> = pendingRelaxations.toList()

    override fun schedulePendingRelaxation(pendingRelaxation: PendingRelaxation) {
        pendingRelaxations.removeAll { existing ->
            when {
                existing is PendingRelaxation.DisableProtection &&
                    pendingRelaxation is PendingRelaxation.DisableProtection -> true
                existing is PendingRelaxation.ReplaceRule &&
                    pendingRelaxation is PendingRelaxation.ReplaceRule ->
                    existing.replacement.packageName == pendingRelaxation.replacement.packageName
                existing is PendingRelaxation.RemoveRule &&
                    pendingRelaxation is PendingRelaxation.RemoveRule ->
                    existing.app.packageName == pendingRelaxation.app.packageName
                else -> false
            }
        }
        pendingRelaxations.add(pendingRelaxation)
    }

    override fun cancelPendingRelaxation(packageName: String) {
        pendingRelaxations.removeAll { pending ->
            when (pending) {
                is PendingRelaxation.ReplaceRule -> pending.replacement.packageName == packageName
                is PendingRelaxation.RemoveRule -> pending.app.packageName == packageName
                is PendingRelaxation.DisableProtection -> false
            }
        }
    }

    override fun removeRule(packageName: String) {
        savedRules.removeAll { it.packageName == packageName }
    }

    override fun isProtectionEnabled(): Boolean = true

    override fun disableProtection() = Unit
}

package com.example.onesec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestrictionSetupControllerTest {
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
        val controller = RestrictionSetupController(catalog, rules)

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
}

private class FakeAppCatalog(
    private val apps: List<InstalledApp>,
) : AppCatalog {
    override fun manageableApps(): List<InstalledApp> = apps
}

private class FakeRestrictionRuleStore(
    initialRule: RestrictedAppRule? = null,
) : RestrictionRuleStore {
    val savedRules = mutableListOf<RestrictedAppRule>().apply {
        if (initialRule != null) add(initialRule)
    }

    override fun loadRules(): List<RestrictedAppRule> = savedRules.toList()

    override fun saveRule(rule: RestrictedAppRule) {
        savedRules.removeAll { it.packageName == rule.packageName }
        savedRules.add(rule)
    }
}

package com.example.onesec

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupRecoveryIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun bootAdapterPreservesSavedProtectionStateWhenRecoveryNeedsRepair() {
        clearProductionPreferences()
        val rule = RestrictedAppRule(
            packageName = "com.example.video",
            displayName = "短视频",
            level = RestrictionLevel.HARD,
            dailyAllowance = DailyAllowance.ofMinutes(30),
        )
        val exhaustedDate = LocalDate.of(2026, 8, 21)
        SharedPreferencesRestrictionRuleStore(context).saveRule(rule)
        SharedPreferencesExhaustedAllowanceStore(context)
            .markExhausted(rule.packageName, exhaustedDate)

        BootCompletedReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertEquals(listOf(rule), SharedPreferencesRestrictionRuleStore(context).loadRules())
        assertTrue(
            SharedPreferencesExhaustedAllowanceStore(context)
                .isExhausted(rule.packageName, exhaustedDate),
        )
        assertEquals(
            RecoveryHealth.NEEDS_REPAIR,
            SharedPreferencesRecoveryHealthStore(context).readRecoveryHealth(),
        )
        clearProductionPreferences()
    }

    @Test
    fun protectionNotificationIsQuietOngoingAndAccuratelyDescribesProtection() {
        createProtectionNotificationChannel(context)

        val notification = protectionNotification(context)

        assertEquals("OneSec 正在保护", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(
            "已保存的应用限制正在持续生效",
            notification.extras.getString(Notification.EXTRA_TEXT),
        )
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(PROTECTION_NOTIFICATION_CHANNEL_ID, notification.channelId)
    }

    @Test
    fun recoveryCoordinatorStartsTheForegroundServiceWhenRulesAndPermissionsAreAvailable() {
        clearProductionPreferences()
        val ruleStore = SharedPreferencesRestrictionRuleStore(context)
        ruleStore.saveRule(
            RestrictedAppRule(
                packageName = "com.example.video",
                displayName = "短视频",
                level = RestrictionLevel.HARD,
                dailyAllowance = DailyAllowance.ofMinutes(30),
            ),
        )
        val healthStore = SharedPreferencesRecoveryHealthStore(context)

        val coordinator = MonitoringRecoveryCoordinator(
            context = context,
            ruleStore = ruleStore,
            permissionSnapshot = { PermissionSnapshot(true, true) },
            healthStore = healthStore,
        )
        coordinator.restore()

        val deadline = SystemClock.uptimeMillis() + 2_000
        while (healthStore.readRecoveryHealth() != RecoveryHealth.MONITORING &&
            SystemClock.uptimeMillis() < deadline
        ) {
            SystemClock.sleep(25)
        }
        assertEquals(RecoveryHealth.MONITORING, healthStore.readRecoveryHealth())

        healthStore.writeRecoveryHealth(RecoveryHealth.NEEDS_REPAIR)
        coordinator.restore()
        val restartDeadline = SystemClock.uptimeMillis() + 2_000
        while (healthStore.readRecoveryHealth() != RecoveryHealth.MONITORING &&
            SystemClock.uptimeMillis() < restartDeadline
        ) {
            SystemClock.sleep(25)
        }
        assertEquals(RecoveryHealth.MONITORING, healthStore.readRecoveryHealth())

        context.stopService(Intent(context, ProtectionNotificationService::class.java))
        clearProductionPreferences()
    }

    private fun clearProductionPreferences() {
        listOf("restriction_rules", "exhausted_allowances", "monitoring_recovery").forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}

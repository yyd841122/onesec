package com.example.onesec

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class ProtectionNotificationService : Service() {
    private val healthStore by lazy { SharedPreferencesRecoveryHealthStore(this) }

    override fun onCreate() {
        super.onCreate()
        createProtectionNotificationChannel(this)
        startForeground(NOTIFICATION_ID, protectionNotification(this))
        healthStore.writeRecoveryHealth(RecoveryHealth.MONITORING)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        healthStore.writeRecoveryHealth(RecoveryHealth.MONITORING)
        return START_STICKY
    }

    override fun onDestroy() {
        if (SharedPreferencesRestrictionRuleStore(this).loadRules().isNotEmpty()) {
            healthStore.writeRecoveryHealth(RecoveryHealth.NEEDS_REPAIR)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val NOTIFICATION_ID = 1001
    }
}

internal const val PROTECTION_NOTIFICATION_CHANNEL_ID = "protection_monitoring"

internal fun createProtectionNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        PROTECTION_NOTIFICATION_CHANNEL_ID,
        "保护状态",
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = "OneSec 持续保护状态"
        setShowBadge(false)
        setSound(null, null)
        enableVibration(false)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

internal fun protectionNotification(context: Context): Notification {
    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return Notification.Builder(context, PROTECTION_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("OneSec 正在保护")
        .setContentText("已保存的应用限制正在持续生效")
        .setContentIntent(openApp)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()
}

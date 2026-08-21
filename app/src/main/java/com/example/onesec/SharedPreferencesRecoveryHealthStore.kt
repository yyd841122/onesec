package com.example.onesec

import android.content.Context

interface RecoveryHealthStore : RecoveryHealthProvider {
    fun writeRecoveryHealth(health: RecoveryHealth)
}

class SharedPreferencesRecoveryHealthStore(
    context: Context,
    preferencesName: String = DEFAULT_FILE_NAME,
) : RecoveryHealthStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun readRecoveryHealth(): RecoveryHealth = preferences
        .getString(KEY_HEALTH, null)
        ?.let { stored -> RecoveryHealth.entries.find { it.name == stored } }
        ?: RecoveryHealth.NOT_REQUIRED

    override fun writeRecoveryHealth(health: RecoveryHealth) {
        preferences.edit().putString(KEY_HEALTH, health.name).commit()
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "monitoring_recovery"
        const val KEY_HEALTH = "recovery_health"
    }
}

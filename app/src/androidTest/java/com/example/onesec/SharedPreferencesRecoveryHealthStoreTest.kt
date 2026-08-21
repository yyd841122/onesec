package com.example.onesec

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPreferencesRecoveryHealthStoreTest {
    @Test
    fun recoveryHealthSurvivesProcessReconstructionWithoutChangingRulesOrUsageState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferencesName = "recovery_health_store_test"
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()

        val store = SharedPreferencesRecoveryHealthStore(context, preferencesName)
        store.writeRecoveryHealth(RecoveryHealth.NEEDS_REPAIR)

        assertEquals(
            RecoveryHealth.NEEDS_REPAIR,
            SharedPreferencesRecoveryHealthStore(context, preferencesName).readRecoveryHealth(),
        )
    }
}

package com.example.onesec

import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesExhaustedAllowanceStoreTest {
    @Test
    fun exhaustedAllowanceSurvivesProcessReconstructionAndExpiresOnTheNextLocalDate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "exhausted_allowance_store_test"
        val packageName = "com.example.video"
        val exhaustedDate = LocalDate.of(2026, 8, 21)
        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        SharedPreferencesExhaustedAllowanceStore(context, preferencesName)
            .markExhausted(packageName, exhaustedDate)
        val reconstructedStore = SharedPreferencesExhaustedAllowanceStore(context, preferencesName)

        assertTrue(reconstructedStore.isExhausted(packageName, exhaustedDate))
        assertFalse(reconstructedStore.isExhausted(packageName, exhaustedDate.plusDays(1)))

        context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

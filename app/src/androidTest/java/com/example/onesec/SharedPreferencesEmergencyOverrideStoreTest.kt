package com.example.onesec

import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPreferencesEmergencyOverrideStoreTest {
    @Test
    fun successfulOverrideAndReasonSurviveReconstruction() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "emergency-override-test-${System.nanoTime()}"
        val record = EmergencyOverrideRecord(
            "com.example.video", "必要联系", Instant.parse("2026-08-21T04:00:00Z"),
            Instant.parse("2026-08-21T04:05:00Z"), LocalDate.parse("2026-08-21"),
        )
        SharedPreferencesEmergencyOverrideStore(context, name).save(record)
        assertEquals(record, SharedPreferencesEmergencyOverrideStore(context, name).load())
    }
}

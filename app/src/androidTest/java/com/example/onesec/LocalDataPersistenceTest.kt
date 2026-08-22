package com.example.onesec

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun retentionDeletesRecordsBeforeTheNinetyDayBoundary() {
        val name = "history-retention-test-${System.nanoTime()}"
        val store = SharedPreferencesLocalHistoryStore(context, ZoneId.of("Asia/Shanghai"), name)
        store.recordIntervention("old", Instant.parse("2026-05-23T15:59:59Z"))
        store.recordIntervention("boundary", Instant.parse("2026-05-23T16:00:00Z"))

        store.pruneBefore(LocalDate.of(2026, 5, 24))

        assertEquals(1, store.today(LocalDate.of(2026, 5, 24)).interventionCount)
        assertEquals(0, store.today(LocalDate.of(2026, 5, 23)).interventionCount)
    }

    @Test
    fun clearRemovesEveryOneSecLocalDataStore() {
        val names = listOf(
            "restriction_rules",
            "emergency_override",
            "access_windows",
            "exhausted_allowances",
            "monitoring_recovery",
            SharedPreferencesLocalHistoryStore.PREFERENCES_NAME,
        )
        names.forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putString("test", "value").commit()
        }

        AndroidLocalDataClearer(context).clearAll()

        names.forEach { name -> assertTrue(context.getSharedPreferences(name, Context.MODE_PRIVATE).all.isEmpty()) }
    }
}

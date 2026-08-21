package com.example.onesec

import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPreferencesAccessWindowStoreTest {
    @Test
    fun accessWindowSurvivesProcessReconstructionWithItsOriginalDeadline() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "access-window-test-${System.nanoTime()}"
        val expected = Instant.parse("2026-08-21T04:05:15Z")

        SharedPreferencesAccessWindowStore(context, preferencesName)
            .save("com.example.video", expected)

        assertEquals(
            expected,
            SharedPreferencesAccessWindowStore(context, preferencesName)
                .endsAt("com.example.video"),
        )
    }
}

package com.example.onesec

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalHistoryTest {
    @Test
    fun `retention keeps today and the preceding 89 local dates`() {
        val records = listOf(
            HistoryRecord.Intervention("old", Instant.parse("2026-05-23T15:59:59Z")),
            HistoryRecord.Intervention("boundary", Instant.parse("2026-05-23T16:00:00Z")),
            HistoryRecord.Intervention("today", Instant.parse("2026-08-21T04:00:00Z")),
        )

        val retained = retainHistory(records, LocalDate.of(2026, 8, 21), ZoneId.of("Asia/Shanghai"))

        assertEquals(listOf("boundary", "today"), retained.map { it.packageName })
    }
}

package com.example.onesec

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertThrows

class EmergencyOverrideManagerTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-08-21T04:00:00Z")

    @Test
    fun `override requires sixty continuous seconds and a non-empty reason`() {
        val session = EmergencyOverrideSession(now)
        assertEquals(WaitDecision.Waiting(1), session.waitDecision(now.plusMillis(59_999)))
        assertEquals(EmergencyOverrideDecision.WaitingRequired, session.confirm(now.plusMillis(59_999), "工作"))
        assertEquals(EmergencyOverrideDecision.ReasonRequired, session.confirm(now.plusSeconds(60), "  "))
        assertEquals(EmergencyOverrideDecision.Ready("工作"), session.confirm(now.plusSeconds(60), " 工作 "))
    }

    @Test
    fun `one successful override blocks every hard restricted app until next local date`() {
        val store = MemoryEmergencyOverrideStore()
        val manager = EmergencyOverrideManager(store, zone)

        val result = manager.grant("com.example.video", "必要联系", now)

        assertEquals(Instant.parse("2026-08-21T04:05:00Z"), result.endsAt)
        assertEquals(false, manager.isAvailable(now.plusSeconds(1)))
        assertEquals(true, manager.isAvailable(Instant.parse("2026-08-21T16:00:00Z")))
        assertEquals("必要联系", store.record?.reason)
        assertEquals(null, manager.activeWindowEndsAt("com.example.other", now.plusSeconds(1)))
        assertEquals(result.endsAt, manager.activeWindowEndsAt("com.example.video", now.plusSeconds(1)))
        assertEquals(null, manager.activeWindowEndsAt("com.example.video", result.endsAt))
        assertThrows(IllegalStateException::class.java) { manager.grant("com.example.other", "另一原因", now) }
    }

    @Test
    fun `manager rejects an empty reason`() {
        val manager = EmergencyOverrideManager(MemoryEmergencyOverrideStore(), zone)
        assertThrows(IllegalArgumentException::class.java) { manager.grant("com.example.video", "  ", now) }
    }
}

private class MemoryEmergencyOverrideStore : EmergencyOverrideStore {
    var record: EmergencyOverrideRecord? = null
    override fun load() = record
    override fun save(record: EmergencyOverrideRecord) { this.record = record }
}

package com.example.onesec

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessWindowManagerTest {
    @Test
    fun `wait remains incomplete before fifteen seconds and completes at the boundary`() {
        val startedAt = Instant.parse("2026-08-21T04:00:00Z")
        val session = SoftRestrictionWaitSession(startedAt)

        assertEquals(WaitDecision.Waiting(15), session.decide(startedAt))
        assertEquals(WaitDecision.Waiting(1), session.decide(startedAt.plusMillis(14_999)))
        assertEquals(WaitDecision.Complete, session.decide(startedAt.plusSeconds(15)))
    }

    @Test
    fun `waiting completion creates exactly a five minute access window`() {
        val store = MemoryAccessWindowStore()
        val now = Instant.parse("2026-08-21T04:00:15Z")

        val endsAt = AccessWindowManager(store).grant("com.example.video", now)

        assertEquals(Instant.parse("2026-08-21T04:05:15Z"), endsAt)
        assertEquals(endsAt, store.endsAt("com.example.video"))
    }
}

private class MemoryAccessWindowStore : AccessWindowStore {
    private val windows = mutableMapOf<String, Instant>()

    override fun endsAt(packageName: String) = windows[packageName]

    override fun save(packageName: String, endsAt: Instant) {
        windows[packageName] = endsAt
    }
}

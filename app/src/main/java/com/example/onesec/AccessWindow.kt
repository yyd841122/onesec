package com.example.onesec

import android.content.Context
import java.time.Duration
import java.time.Instant

interface AccessWindowStore {
    fun endsAt(packageName: String): Instant?

    fun save(packageName: String, endsAt: Instant)
}

class AccessWindowManager(
    private val store: AccessWindowStore,
) {
    fun grant(packageName: String, now: Instant): Instant {
        val endsAt = now.plus(ACCESS_WINDOW_DURATION)
        store.save(packageName, endsAt)
        return endsAt
    }

    companion object {
        val WAIT_DURATION: Duration = Duration.ofSeconds(15)
        val ACCESS_WINDOW_DURATION: Duration = Duration.ofMinutes(5)
    }
}

sealed interface WaitDecision {
    data class Waiting(val remainingSeconds: Int) : WaitDecision
    data object Complete : WaitDecision
}

class TimedWaitSession(private val startedAt: Instant, private val duration: Duration) {
    fun decide(now: Instant): WaitDecision {
        val elapsed = Duration.between(startedAt, now).coerceAtLeast(Duration.ZERO)
        if (elapsed >= duration) return WaitDecision.Complete
        val remaining = duration.minus(elapsed)
        return WaitDecision.Waiting((remaining.toMillis() + 999).div(1_000).toInt())
    }
}

class SoftRestrictionWaitSession(startedAt: Instant) {
    private val wait = TimedWaitSession(startedAt, AccessWindowManager.WAIT_DURATION)
    fun decide(now: Instant) = wait.decide(now)
}

class SharedPreferencesAccessWindowStore(
    context: Context,
    preferencesName: String = DEFAULT_FILE_NAME,
) : AccessWindowStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun endsAt(packageName: String): Instant? {
        val epochMillis = preferences.getLong(key(packageName), NO_WINDOW)
        return if (epochMillis == NO_WINDOW) null else Instant.ofEpochMilli(epochMillis)
    }

    override fun save(packageName: String, endsAt: Instant) {
        preferences.edit().putLong(key(packageName), endsAt.toEpochMilli()).commit()
    }

    private fun key(packageName: String) = "access_window_ends_at.$packageName"

    private companion object {
        const val DEFAULT_FILE_NAME = "access_windows"
        const val NO_WINDOW = Long.MIN_VALUE
    }
}

package com.example.onesec

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class EmergencyOverrideRecord(
    val packageName: String,
    val reason: String,
    val grantedAt: Instant,
    val endsAt: Instant,
    val localDate: LocalDate,
    val result: EmergencyOverrideResult = EmergencyOverrideResult.SUCCESS,
)

enum class EmergencyOverrideResult { SUCCESS }

interface EmergencyOverrideStore {
    fun load(): EmergencyOverrideRecord?
    fun save(record: EmergencyOverrideRecord)
}

sealed interface EmergencyOverrideDecision {
    data object WaitingRequired : EmergencyOverrideDecision
    data object ReasonRequired : EmergencyOverrideDecision
    data class Ready(val reason: String) : EmergencyOverrideDecision
}

class EmergencyOverrideSession(private val startedAt: Instant) {
    private val wait = TimedWaitSession(startedAt, WAIT_DURATION)
    fun waitDecision(now: Instant) = wait.decide(now)
    fun confirm(now: Instant, reason: String): EmergencyOverrideDecision {
        if (wait.decide(now) != WaitDecision.Complete) return EmergencyOverrideDecision.WaitingRequired
        val normalized = reason.trim()
        return if (normalized.isEmpty()) EmergencyOverrideDecision.ReasonRequired
        else EmergencyOverrideDecision.Ready(normalized)
    }

    companion object { val WAIT_DURATION: Duration = Duration.ofSeconds(60) }
}

class EmergencyOverrideManager(
    private val store: EmergencyOverrideStore,
    private val zoneId: ZoneId,
    private val historyStore: LocalHistoryStore? = null,
) {
    fun isAvailable(now: Instant): Boolean = store.load()?.localDate != now.atZone(zoneId).toLocalDate()

    fun activeWindowEndsAt(packageName: String, now: Instant): Instant? = store.load()
        ?.takeIf { it.packageName == packageName && it.result == EmergencyOverrideResult.SUCCESS && it.endsAt.isAfter(now) }
        ?.endsAt

    fun grant(packageName: String, reason: String, now: Instant): EmergencyOverrideRecord {
        check(isAvailable(now)) { "Emergency override already used today" }
        require(reason.isNotBlank()) { "Emergency override reason is required" }
        val record = EmergencyOverrideRecord(
            packageName = packageName,
            reason = reason.trim(),
            grantedAt = now,
            endsAt = now.plus(Duration.ofMinutes(5)),
            localDate = now.atZone(zoneId).toLocalDate(),
        )
        store.save(record)
        historyStore?.recordEmergencyOverride(record)
        return record
    }
}

class SharedPreferencesEmergencyOverrideStore(
    context: Context,
    preferencesName: String = "emergency_override",
) : EmergencyOverrideStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    override fun load(): EmergencyOverrideRecord? {
        val packageName = preferences.getString("package_name", null) ?: return null
        return EmergencyOverrideRecord(
            packageName = packageName,
            reason = preferences.getString("reason", "").orEmpty(),
            grantedAt = Instant.ofEpochMilli(preferences.getLong("granted_at", 0)),
            endsAt = Instant.ofEpochMilli(preferences.getLong("ends_at", 0)),
            localDate = LocalDate.ofEpochDay(preferences.getLong("local_date", 0)),
            result = preferences.getString("result", null)
                ?.let(EmergencyOverrideResult::valueOf)
                ?: EmergencyOverrideResult.SUCCESS,
        )
    }
    override fun save(record: EmergencyOverrideRecord) {
        preferences.edit()
            .putString("package_name", record.packageName)
            .putString("reason", record.reason)
            .putLong("granted_at", record.grantedAt.toEpochMilli())
            .putLong("ends_at", record.endsAt.toEpochMilli())
            .putLong("local_date", record.localDate.toEpochDay())
            .putString("result", record.result.name)
            .commit()
    }
}

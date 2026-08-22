package com.example.onesec

import android.content.Context
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64
import java.util.UUID

sealed interface HistoryRecord {
    val packageName: String

    data class Intervention(
        override val packageName: String,
        val at: Instant,
    ) : HistoryRecord

    data class Usage(
        override val packageName: String,
        val localDate: LocalDate,
        val usedMinutes: Int,
    ) : HistoryRecord

    data class EmergencyOverride(
        override val packageName: String,
        val localDate: LocalDate,
    ) : HistoryRecord
}

data class TodayHistory(
    val interventionCount: Int = 0,
    val emergencyOverrideUsed: Boolean = false,
)

interface LocalHistoryStore {
    fun recordUsage(packageName: String, localDate: LocalDate, usedMinutes: Int)
    fun recordIntervention(packageName: String, at: Instant)
    fun recordEmergencyOverride(record: EmergencyOverrideRecord)
    fun today(onDate: LocalDate): TodayHistory
    fun pruneBefore(cutoff: LocalDate)
}

fun retainHistory(
    records: List<HistoryRecord>,
    today: LocalDate,
    zoneId: ZoneId,
): List<HistoryRecord> {
    val cutoff = today.minusDays(89)
    return records.filter { record ->
        val date = when (record) {
            is HistoryRecord.Intervention -> record.at.atZone(zoneId).toLocalDate()
            is HistoryRecord.Usage -> record.localDate
            is HistoryRecord.EmergencyOverride -> record.localDate
        }
        !date.isBefore(cutoff)
    }
}

class SharedPreferencesLocalHistoryStore(
    context: Context,
    private val zoneId: ZoneId,
    preferencesName: String = PREFERENCES_NAME,
) : LocalHistoryStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun recordUsage(packageName: String, localDate: LocalDate, usedMinutes: Int) {
        val prefix = "U|${localDate.toEpochDay()}|${encode(packageName)}|"
        val records = preferences.getStringSet(KEY_RECORDS, emptySet()).orEmpty()
            .filterNot { it.startsWith(prefix) }
            .plus(prefix + usedMinutes.coerceAtLeast(0))
            .toSet()
        preferences.edit().putStringSet(KEY_RECORDS, records).commit()
    }

    override fun recordIntervention(packageName: String, at: Instant) {
        append("I|${UUID.randomUUID()}|${at.toEpochMilli()}|${encode(packageName)}")
    }

    override fun recordEmergencyOverride(record: EmergencyOverrideRecord) {
        append("E|${UUID.randomUUID()}|${record.localDate.toEpochDay()}|${encode(record.packageName)}")
    }

    override fun today(onDate: LocalDate): TodayHistory {
        val records = loadRecords()
        return TodayHistory(
            interventionCount = records.count {
                it is HistoryRecord.Intervention && it.at.atZone(zoneId).toLocalDate() == onDate
            },
            emergencyOverrideUsed = records.any {
                it is HistoryRecord.EmergencyOverride && it.localDate == onDate
            },
        )
    }

    override fun pruneBefore(cutoff: LocalDate) {
        val retained = preferences.getStringSet(KEY_RECORDS, emptySet()).orEmpty().filter { encoded ->
            val record = decode(encoded) ?: return@filter false
            val date = when (record) {
                is HistoryRecord.Intervention -> record.at.atZone(zoneId).toLocalDate()
                is HistoryRecord.Usage -> record.localDate
                is HistoryRecord.EmergencyOverride -> record.localDate
            }
            !date.isBefore(cutoff)
        }.toSet()
        preferences.edit().putStringSet(KEY_RECORDS, retained).commit()
    }

    private fun append(record: String) {
        preferences.edit()
            .putStringSet(KEY_RECORDS, preferences.getStringSet(KEY_RECORDS, emptySet()).orEmpty() + record)
            .commit()
    }

    private fun loadRecords() = preferences.getStringSet(KEY_RECORDS, emptySet()).orEmpty().mapNotNull(::decode)

    private fun decode(value: String): HistoryRecord? {
        val parts = value.split('|')
        if (parts.size != 4) return null
        return when (parts[0]) {
            "I" -> HistoryRecord.Intervention(decodeText(parts[3]), Instant.ofEpochMilli(parts[2].toLong()))
            "E" -> HistoryRecord.EmergencyOverride(decodeText(parts[3]), LocalDate.ofEpochDay(parts[2].toLong()))
            "U" -> HistoryRecord.Usage(decodeText(parts[2]), LocalDate.ofEpochDay(parts[1].toLong()), parts[3].toInt())
            else -> null
        }
    }

    private fun encode(value: String) = Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun decodeText(value: String) = String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)

    companion object {
        const val PREFERENCES_NAME = "local_history"
        private const val KEY_RECORDS = "records"
    }
}

fun interface LocalDataClearer {
    fun clearAll()
}

class AndroidLocalDataClearer(private val context: Context) : LocalDataClearer {
    override fun clearAll() {
        PREFERENCE_FILES.forEach { name -> context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit() }
    }

    private companion object {
        val PREFERENCE_FILES = listOf(
            "restriction_rules",
            "emergency_override",
            "access_windows",
            "exhausted_allowances",
            "monitoring_recovery",
            SharedPreferencesLocalHistoryStore.PREFERENCES_NAME,
        )
    }
}

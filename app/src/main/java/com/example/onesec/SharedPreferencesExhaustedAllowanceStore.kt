package com.example.onesec

import android.content.Context
import java.time.LocalDate

class SharedPreferencesExhaustedAllowanceStore(
    context: Context,
    preferencesName: String = DEFAULT_FILE_NAME,
) : ExhaustedAllowanceStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun isExhausted(packageName: String, localDate: LocalDate): Boolean =
        preferences.getLong(key(packageName), NO_EXHAUSTED_DATE) == localDate.toEpochDay()

    override fun markExhausted(packageName: String, localDate: LocalDate) {
        preferences.edit().putLong(key(packageName), localDate.toEpochDay()).commit()
    }

    private fun key(packageName: String) = "exhausted_on.$packageName"

    private companion object {
        const val DEFAULT_FILE_NAME = "exhausted_allowances"
        const val NO_EXHAUSTED_DATE = Long.MIN_VALUE
    }
}

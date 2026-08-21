package com.example.onesec

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.Instant

class AndroidUsageEventSource(
    context: Context,
) : UsageEventSource {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    override fun eventsBetween(start: Instant, end: Instant): List<UsageEvent> {
        val result = mutableListOf<UsageEvent>()
        val usageEvents = usageStatsManager.queryEvents(start.toEpochMilli(), end.toEpochMilli())
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.FOREGROUND
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                -> UsageEventType.BACKGROUND
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_LOCKED
                UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventType.SCREEN_UNLOCKED
                else -> null
            }
            if (type != null) {
                result += UsageEvent(
                    packageName = event.packageName,
                    timestamp = Instant.ofEpochMilli(event.timeStamp),
                    type = type,
                    activityId = event.className ?: event.packageName.orEmpty(),
                )
            }
        }
        return result
    }
}

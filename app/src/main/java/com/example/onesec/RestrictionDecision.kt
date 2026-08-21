package com.example.onesec

import java.time.Instant
import java.time.ZoneId

data class RestrictionDecisionRequest(
    val now: Instant,
    val zoneId: ZoneId,
    val restrictedApp: InstalledApp,
    val usedMinutes: Int,
    val rule: RestrictedAppRule,
    val protectionAvailable: Boolean,
    val accessWindowEndsAt: Instant? = null,
)

sealed interface ProtectionDecision {
    data object Allow : ProtectionDecision

    data object ProtectionUnavailable : ProtectionDecision

    data class Intervene(
        val app: InstalledApp,
        val usedMinutes: Int,
        val resetsAt: Instant,
        val level: RestrictionLevel = RestrictionLevel.HARD,
    ) : ProtectionDecision
}

interface RestrictionDecisionEngine {
    fun decide(request: RestrictionDecisionRequest): ProtectionDecision
}

object DefaultRestrictionDecisionEngine : RestrictionDecisionEngine {
    override fun decide(request: RestrictionDecisionRequest): ProtectionDecision {
        if (!request.protectionAvailable) return ProtectionDecision.ProtectionUnavailable
        if (request.usedMinutes < request.rule.dailyAllowance.minutes) return ProtectionDecision.Allow
        if (request.rule.level == RestrictionLevel.SOFT && request.accessWindowEndsAt?.isAfter(request.now) == true) {
            return ProtectionDecision.Allow
        }

        val resetsAt = request.now
            .atZone(request.zoneId)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(request.zoneId)
            .toInstant()
        return ProtectionDecision.Intervene(
            app = request.restrictedApp,
            usedMinutes = request.usedMinutes,
            resetsAt = resetsAt,
            level = request.rule.level,
        )
    }
}

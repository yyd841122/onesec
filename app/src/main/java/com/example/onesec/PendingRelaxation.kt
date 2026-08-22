package com.example.onesec

import java.time.LocalDate

sealed interface RuleChange {
    data class Replace(
        val current: RestrictedAppRule?,
        val replacement: RestrictedAppRule,
    ) : RuleChange

    data class Remove(val current: RestrictedAppRule) : RuleChange

    data object DisableProtection : RuleChange
}

sealed interface PendingRelaxation {
    val effectiveDate: LocalDate
    val affectedPackageName: String?
        get() = when (this) {
            is ReplaceRule -> replacement.packageName
            is RemoveRule -> app.packageName
            is DisableProtection -> null
        }

    data class ReplaceRule(
        val replacement: RestrictedAppRule,
        override val effectiveDate: LocalDate,
    ) : PendingRelaxation

    data class RemoveRule(
        val app: InstalledApp,
        override val effectiveDate: LocalDate,
    ) : PendingRelaxation

    data class DisableProtection(
        override val effectiveDate: LocalDate,
    ) : PendingRelaxation
}

sealed interface RuleChangeDecision {
    data class ApplyNow(val rule: RestrictedAppRule) : RuleChangeDecision
    data class Schedule(val pendingRelaxation: PendingRelaxation) : RuleChangeDecision
    data class ApplyNowAndSchedule(
        val immediateRule: RestrictedAppRule,
        val pendingRelaxation: PendingRelaxation,
    ) : RuleChangeDecision
}

data class EffectiveRestrictionPolicy(
    val rules: List<RestrictedAppRule>,
    val protectionEnabled: Boolean,
    val pendingRelaxations: List<PendingRelaxation>,
)

fun resolvePendingRelaxations(
    rules: List<RestrictedAppRule>,
    protectionEnabled: Boolean,
    pendingRelaxations: List<PendingRelaxation>,
    onDate: LocalDate,
): EffectiveRestrictionPolicy {
    var effectiveRules = rules
    var effectiveProtectionEnabled = protectionEnabled
    val (due, remaining) = pendingRelaxations.partition { !it.effectiveDate.isAfter(onDate) }
    due.sortedBy { it.effectiveDate }.forEach { pending ->
        when (pending) {
            is PendingRelaxation.ReplaceRule -> effectiveRules = effectiveRules
                .filterNot { it.packageName == pending.replacement.packageName }
                .plus(pending.replacement)
            is PendingRelaxation.RemoveRule -> effectiveRules = effectiveRules
                .filterNot { it.packageName == pending.app.packageName }
            is PendingRelaxation.DisableProtection -> effectiveProtectionEnabled = false
        }
    }
    return EffectiveRestrictionPolicy(
        rules = effectiveRules.sortedBy(RestrictedAppRule::displayName),
        protectionEnabled = effectiveProtectionEnabled,
        pendingRelaxations = remaining.sortedBy { it.effectiveDate },
    )
}

fun decideRuleChange(change: RuleChange, requestedOn: LocalDate): RuleChangeDecision {
    val effectiveDate = requestedOn.plusDays(1)
    return when (change) {
        is RuleChange.Replace -> {
            val current = change.current
            val replacement = change.replacement
            if (current == null) return RuleChangeDecision.ApplyNow(replacement)

            val immediateRule = replacement.copy(
                level = if (replacement.level.strictness >= current.level.strictness) {
                    replacement.level
                } else {
                    current.level
                },
                dailyAllowance = DailyAllowance.ofMinutes(
                    minOf(current.dailyAllowance.minutes, replacement.dailyAllowance.minutes),
                ),
            )
            val pendingRelaxation = PendingRelaxation.ReplaceRule(replacement, effectiveDate)
            when {
                immediateRule == replacement -> RuleChangeDecision.ApplyNow(replacement)
                immediateRule == current -> RuleChangeDecision.Schedule(pendingRelaxation)
                else -> RuleChangeDecision.ApplyNowAndSchedule(immediateRule, pendingRelaxation)
            }
        }
        is RuleChange.Remove -> RuleChangeDecision.Schedule(
            PendingRelaxation.RemoveRule(
                InstalledApp(change.current.packageName, change.current.displayName),
                effectiveDate,
            ),
        )
        RuleChange.DisableProtection -> RuleChangeDecision.Schedule(
            PendingRelaxation.DisableProtection(effectiveDate),
        )
    }
}

private val RestrictionLevel.strictness: Int
    get() = when (this) {
        RestrictionLevel.SOFT -> 0
        RestrictionLevel.HARD -> 1
    }

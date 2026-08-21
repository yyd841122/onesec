package com.example.onesec

data class RecoveryRequest(
    val hasSavedRules: Boolean,
    val corePermissionsAvailable: Boolean,
)

sealed interface RecoveryDecision {
    data object NothingToRestore : RecoveryDecision
    data object NeedsRepair : RecoveryDecision
    data object StartMonitoring : RecoveryDecision
}

enum class RecoveryHealth {
    NOT_REQUIRED,
    MONITORING,
    NEEDS_REPAIR,
}

fun decideStartupRecovery(request: RecoveryRequest): RecoveryDecision = when {
    !request.hasSavedRules -> RecoveryDecision.NothingToRestore
    !request.corePermissionsAvailable -> RecoveryDecision.NeedsRepair
    else -> RecoveryDecision.StartMonitoring
}

fun interface RecoveryHealthProvider {
    fun readRecoveryHealth(): RecoveryHealth
}

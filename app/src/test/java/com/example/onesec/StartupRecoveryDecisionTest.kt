package com.example.onesec

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupRecoveryDecisionTest {
    @Test
    fun `boot recovery starts monitoring only for saved rules with core permissions`() {
        val cases = listOf(
            RecoveryRequest(hasSavedRules = false, corePermissionsAvailable = false) to
                RecoveryDecision.NothingToRestore,
            RecoveryRequest(hasSavedRules = false, corePermissionsAvailable = true) to
                RecoveryDecision.NothingToRestore,
            RecoveryRequest(hasSavedRules = true, corePermissionsAvailable = false) to
                RecoveryDecision.NeedsRepair,
            RecoveryRequest(hasSavedRules = true, corePermissionsAvailable = true) to
                RecoveryDecision.StartMonitoring,
        )

        cases.forEach { (request, expected) ->
            assertEquals(expected, decideStartupRecovery(request))
        }
    }
}

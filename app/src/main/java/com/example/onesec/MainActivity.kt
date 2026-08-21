package com.example.onesec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.time.Clock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionGateway = AndroidPermissionGateway(this)
        val ruleStore = SharedPreferencesRestrictionRuleStore(this)
        val permissionController = PermissionGuidanceController(permissionGateway)
        val restrictionController = RestrictionSetupController(
            appCatalog = AndroidAppCatalog(this),
            ruleStore = ruleStore,
        )
        val todayUsageController = TodayUsageController(
            ruleStore = ruleStore,
            permissionGateway = permissionGateway,
            usageEvents = AndroidUsageEventSource(this),
            clock = Clock.systemDefaultZone(),
        )
        setContent {
            OneSecRoot(permissionController, restrictionController, todayUsageController)
        }
    }
}

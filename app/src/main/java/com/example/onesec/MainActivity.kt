package com.example.onesec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionController = PermissionGuidanceController(AndroidPermissionGateway(this))
        val restrictionController = RestrictionSetupController(
            appCatalog = AndroidAppCatalog(this),
            ruleStore = SharedPreferencesRestrictionRuleStore(this),
        )
        setContent {
            OneSecRoot(permissionController, restrictionController)
        }
    }
}

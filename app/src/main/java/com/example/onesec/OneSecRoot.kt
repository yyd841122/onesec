package com.example.onesec

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private enum class OneSecPage {
    PERMISSIONS,
    RESTRICTIONS,
}

@Composable
fun OneSecRoot(
    permissionController: PermissionGuidanceController,
    restrictionController: RestrictionSetupController,
) {
    var page by remember { mutableStateOf(OneSecPage.PERMISSIONS) }
    when (page) {
        OneSecPage.PERMISSIONS -> PermissionGuidanceRoute(
            controller = permissionController,
            onSelectRestrictedApp = { page = OneSecPage.RESTRICTIONS },
        )
        OneSecPage.RESTRICTIONS -> RestrictionSetupRoute(
            controller = restrictionController,
            onBack = { page = OneSecPage.PERMISSIONS },
        )
    }
}

package com.example.onesec

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private enum class OneSecPage {
    PERMISSIONS,
    RESTRICTIONS,
    TODAY,
}

@Composable
fun OneSecRoot(
    permissionController: PermissionGuidanceController,
    restrictionController: RestrictionSetupController,
    todayUsageController: TodayUsageController,
) {
    var page by remember { mutableStateOf(OneSecPage.PERMISSIONS) }
    when (page) {
        OneSecPage.PERMISSIONS -> PermissionGuidanceRoute(
            controller = permissionController,
            onSelectRestrictedApp = { page = OneSecPage.RESTRICTIONS },
            onOpenToday = { page = OneSecPage.TODAY },
        )
        OneSecPage.RESTRICTIONS -> RestrictionSetupRoute(
            controller = restrictionController,
            onBack = { page = OneSecPage.PERMISSIONS },
        )
        OneSecPage.TODAY -> TodayUsageRoute(
            controller = todayUsageController,
            onBack = { page = OneSecPage.PERMISSIONS },
        )
    }
}

package com.example.onesec

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun PermissionGuidanceRoute(
    controller: PermissionGuidanceController,
    onSelectRestrictedApp: () -> Unit = {},
    onOpenToday: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var state by remember(controller) { mutableStateOf(controller.state) }

    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                controller.refresh()
                state = controller.state
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OneSecApp(
        state = state,
        onOpenUsageAccessSettings = controller::openUsageAccessSettings,
        onOpenAccessibilitySettings = controller::openAccessibilitySettings,
        onSelectRestrictedApp = onSelectRestrictedApp,
        onOpenToday = onOpenToday,
    )
}

@Composable
fun OneSecApp(
    state: PermissionGuidanceState,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSelectRestrictedApp: () -> Unit = {},
    onOpenToday: () -> Unit = {},
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "OneSec 权限引导",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = state.protectionStatus,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (state.protectionAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (state.usageDataReliable) {
                        "两项核心权限均有效，使用数据可靠。"
                    } else {
                        "使用数据不可靠：请修复下方缺失的核心权限。"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.usageDataReliable) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                PermissionCard(
                    title = "使用情况访问权限",
                    explanation = "使用情况访问权限用于统计应用前台用时。",
                    granted = state.usageAccessGranted,
                    buttonLabel = "打开使用情况访问设置",
                    onOpenSettings = onOpenUsageAccessSettings,
                )
                PermissionCard(
                    title = "无障碍权限",
                    explanation = "无障碍权限用于识别当前应用并触发拦截。",
                    granted = state.accessibilityGranted,
                    buttonLabel = "打开无障碍设置",
                    onOpenSettings = onOpenAccessibilitySettings,
                )
                Button(onClick = onSelectRestrictedApp) {
                    Text("选择受限应用")
                }
                Button(onClick = onOpenToday) {
                    Text("查看今日概览")
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    explanation: String,
    granted: Boolean,
    buttonLabel: String,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (granted) "已授予" else "未授予",
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = onOpenSettings) {
                Text(buttonLabel)
            }
        }
    }
}

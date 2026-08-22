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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

@Composable
fun PermissionGuidanceRoute(
    controller: PermissionGuidanceController,
    onSelectRestrictedApp: () -> Unit = {},
    onOpenToday: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var state by remember(controller) { mutableStateOf(controller.state) }

    LaunchedEffect(controller, state.recoveryHealth) {
        if (state.recoveryHealth == RecoveryHealth.NEEDS_REPAIR &&
            state.usageAccessGranted && state.accessibilityGranted
        ) {
            repeat(10) {
                delay(500)
                controller.refresh()
                state = controller.state
                if (state.recoveryHealth != RecoveryHealth.NEEDS_REPAIR) return@LaunchedEffect
            }
        }
    }

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
        onOpenExactAlarmSettings = controller::openExactAlarmSettings,
        onOpenBatteryOptimizationSettings = controller::openBatteryOptimizationSettings,
        onOpenBackgroundRunSettings = controller::openBackgroundRunSettings,
        onSelectRestrictedApp = onSelectRestrictedApp,
        onOpenToday = onOpenToday,
    )
}

@Composable
fun OneSecApp(
    state: PermissionGuidanceState,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit = {},
    onOpenBatteryOptimizationSettings: () -> Unit = {},
    onOpenBackgroundRunSettings: () -> Unit = {},
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
                    text = state.statusExplanation,
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("OPPO / ColorOS 后台保护", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "允许 OneSec 后台运行和自启动，并关闭电池优化。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "在 ColorOS 11.1 中，还请在应用管理中允许自启动、关联启动和后台活动。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = onOpenBatteryOptimizationSettings) {
                            Text("打开电池优化设置")
                        }
                        Button(onClick = onOpenBackgroundRunSettings) {
                            Text("打开 OneSec 应用设置")
                        }
                    }
                }
                PermissionCard(
                    title = "无障碍权限",
                    explanation = "无障碍权限用于识别当前应用并触发拦截。",
                    granted = state.accessibilityGranted,
                    buttonLabel = "打开无障碍设置",
                    onOpenSettings = onOpenAccessibilitySettings,
                )
                PermissionCard(
                    title = "精确到期提醒",
                    explanation = "用于在每日额度、使用窗口或紧急解锁到期时立即重新执行限制。",
                    granted = state.exactAlarmsGranted,
                    buttonLabel = "打开闹钟和提醒设置",
                    onOpenSettings = onOpenExactAlarmSettings,
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

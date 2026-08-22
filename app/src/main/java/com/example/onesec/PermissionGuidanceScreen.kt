package com.example.onesec

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    OneSecTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                ScreenHeader(
                    eyebrow = "OneSec",
                    title = "专注保护",
                    description = "让每一次打开，都成为一次主动选择。",
                )
                ProtectionOverviewCard(state)
                SectionTitle(
                    title = "必要权限",
                    detail = "${state.grantedPermissionCount()}/3 已完成",
                )
                PermissionCard(
                    title = "使用情况访问权限",
                    explanation = "统计受限应用在前台的实际使用时间。",
                    granted = state.usageAccessGranted,
                    buttonLabel = "使用设置",
                    onOpenSettings = onOpenUsageAccessSettings,
                )
                PermissionCard(
                    title = "无障碍权限",
                    explanation = "识别当前应用并在额度耗尽后触发限制。",
                    granted = state.accessibilityGranted,
                    buttonLabel = "无障碍设置",
                    onOpenSettings = onOpenAccessibilitySettings,
                )
                PermissionCard(
                    title = "精确到期提醒",
                    explanation = "在额度、窗口或紧急解锁到期时准时恢复限制。",
                    granted = state.exactAlarmsGranted,
                    buttonLabel = "提醒设置",
                    onOpenSettings = onOpenExactAlarmSettings,
                )
                SectionTitle("设备保障", "ColorOS")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionTitle("允许后台持续保护")
                        Text(
                            "建议关闭电池优化，并允许自启动、关联启动和后台活动。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(onClick = onOpenBatteryOptimizationSettings, modifier = Modifier.weight(1f)) {
                                Text("电池优化")
                            }
                            OutlinedButton(onClick = onOpenBackgroundRunSettings, modifier = Modifier.weight(1f)) {
                                Text("应用设置")
                            }
                        }
                    }
                }
                SectionTitle("管理")
                Button(onClick = onSelectRestrictedApp, modifier = Modifier.fillMaxWidth()) {
                    Text("选择受限应用")
                }
                OutlinedButton(onClick = onOpenToday, modifier = Modifier.fillMaxWidth()) {
                    Text("查看今日概览")
                }
            }
        }
    }
}

private fun PermissionGuidanceState.grantedPermissionCount(): Int = listOf(
    usageAccessGranted,
    accessibilityGranted,
    exactAlarmsGranted,
).count { it }

@Composable
private fun ProtectionOverviewCard(state: PermissionGuidanceState) {
    val progress = state.grantedPermissionCount() / 3f
    val containerColor = if (state.protectionAvailable) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                )
                Text("${state.grantedPermissionCount()}/3", style = MaterialTheme.typography.titleMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                StatusPill(state.protectionStatus, state.protectionAvailable)
                Text(
                    text = state.statusExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.protectionAvailable) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (granted) "已完成" else "需要授权",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text(buttonLabel)
            }
        }
    }
}

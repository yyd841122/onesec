package com.example.onesec

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun TodayUsageRoute(
    controller: TodayUsageController,
    onBack: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var state by remember(controller) { mutableStateOf(controller.state) }

    DisposableEffect(lifecycleOwner, controller) {
        fun refresh() {
            controller.refresh()
            state = controller.state
        }
        refresh()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    TodayUsageScreen(
        state = state,
        onBack = onBack,
        onClearAllLocalData = {
            controller.clearAllLocalData()
            state = controller.state
        },
    )
}

@Composable
fun TodayUsageScreen(
    state: TodayUsageState,
    onBack: () -> Unit,
    onClearAllLocalData: () -> Unit = {},
) {
    var confirmingClear by remember { mutableStateOf(false) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("今日概览", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                if (!state.protectionAvailable) {
                    Text(
                        "保护失效",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "核心权限已失效，今日用量不可靠。修复权限后再显示剩余额度。",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (state.apps.isEmpty()) {
                    Text("尚未选择受限应用")
                } else {
                    Text(
                        "全部受限应用今日总用时 ${state.totalUsedMinutes} 分钟",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("今日拦截 ${state.interventionCount} 次")
                    Text("今日紧急解锁：${if (state.emergencyOverrideUsed) "已使用" else "未使用"}")
                    state.globalPendingRelaxation?.let { pending ->
                        Text("待生效：${pending.overviewText()}", color = MaterialTheme.colorScheme.primary)
                    }
                    Text("不足一分钟的用时按一分钟计入。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.apps.forEach { app -> TodayAppUsageCard(app) }
                }
                OutlinedButton(onClick = { confirmingClear = true }) {
                    Text("清除全部本地数据")
                }
                OutlinedButton(onClick = onBack) {
                    Text("返回权限引导")
                }
            }
        }
    }
    if (confirmingClear) {
        AlertDialog(
            onDismissRequest = { confirmingClear = false },
            title = { Text("清除全部本地数据？") },
            text = { Text("规则、历史、使用窗口、待生效变更和紧急解锁记录都会永久删除，且无法撤销。") },
            confirmButton = {
                Button(onClick = {
                    confirmingClear = false
                    onClearAllLocalData()
                }) { Text("确认清除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmingClear = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TodayAppUsageCard(app: TodayAppUsage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(app.displayName, style = MaterialTheme.typography.titleLarge)
            Text(if (app.level == RestrictionLevel.HARD) "强限制" else "弱限制")
            Text("今日已用 ${app.usedMinutes} 分钟")
            Text("剩余每日额度 ${app.remainingMinutes} 分钟", fontWeight = FontWeight.Bold)
            app.pendingRelaxation?.let { pending ->
                Text("待生效：${pending.overviewText()}", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun PendingRelaxation.overviewText(): String = when (this) {
    is PendingRelaxation.ReplaceRule ->
        "变更为${if (replacement.level == RestrictionLevel.HARD) "强限制" else "弱限制"}，每日额度 ${replacement.dailyAllowance.minutes} 分钟（$effectiveDate 生效）"
    is PendingRelaxation.RemoveRule -> "移除限制（$effectiveDate 生效）"
    is PendingRelaxation.DisableProtection -> "关闭保护（$effectiveDate 生效）"
}

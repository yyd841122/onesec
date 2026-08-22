package com.example.onesec

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RestrictionSetupRoute(
    controller: RestrictionSetupController,
    onBack: () -> Unit,
) {
    var state by remember(controller) { mutableStateOf(controller.state) }
    var showingCatalog by remember { mutableStateOf(false) }

    RestrictionSetupScreen(
        state = state,
        showingCatalog = showingCatalog,
        onOpenCatalog = {
            controller.openAppCatalog()
            state = controller.state
            showingCatalog = true
        },
        onSelectApp = { packageName ->
            controller.selectApp(packageName)
            state = controller.state
            showingCatalog = false
        },
        onEditRule = { packageName ->
            controller.editRule(packageName)
            state = controller.state
        },
        onChangeAllowance = { minutes ->
            controller.changeDailyAllowance(minutes)
            state = controller.state
        },
        onChangeLevel = { level ->
            controller.changeRestrictionLevel(level)
            state = controller.state
        },
        onSave = {
            controller.saveRule()
            state = controller.state
        },
        onTightenToHard = { packageName ->
            controller.tightenToHardRestriction(packageName)
            state = controller.state
        },
        onRemoveRule = { packageName ->
            controller.removeRule(packageName)
            state = controller.state
        },
        onDisableProtection = {
            controller.disableProtection()
            state = controller.state
        },
        onCancelSelection = {
            controller.cancelSelection()
            state = controller.state
        },
        onBack = onBack,
    )
}

@Composable
private fun RestrictionSetupScreen(
    state: RestrictionSetupState,
    showingCatalog: Boolean,
    onOpenCatalog: () -> Unit,
    onSelectApp: (String) -> Unit,
    onEditRule: (String) -> Unit,
    onChangeAllowance: (Int) -> Unit,
    onChangeLevel: (RestrictionLevel) -> Unit,
    onSave: () -> Unit,
    onTightenToHard: (String) -> Unit,
    onRemoveRule: (String) -> Unit,
    onDisableProtection: () -> Unit,
    onCancelSelection: () -> Unit,
    onBack: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("受限应用", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

                when {
                    state.editor != null -> RestrictionEditor(
                        editor = state.editor,
                        onChangeAllowance = onChangeAllowance,
                        onChangeLevel = onChangeLevel,
                        onSave = onSave,
                        onCancelSelection = onCancelSelection,
                    )
                    showingCatalog -> AppCatalogList(state.apps, onSelectApp)
                    else -> RestrictionSummary(
                        savedRules = state.savedRules,
                        pendingRelaxations = state.pendingRelaxations,
                        protectionEnabled = state.protectionEnabled,
                        onOpenCatalog = onOpenCatalog,
                        onEditRule = onEditRule,
                        onTightenToHard = onTightenToHard,
                        onRemoveRule = onRemoveRule,
                        onDisableProtection = onDisableProtection,
                    )
                }

                OutlinedButton(onClick = onBack) {
                    Text("返回权限引导")
                }
            }
        }
    }
}

@Composable
private fun RestrictionSummary(
    savedRules: List<RestrictedAppRule>,
    pendingRelaxations: List<PendingRelaxation>,
    protectionEnabled: Boolean,
    onOpenCatalog: () -> Unit,
    onEditRule: (String) -> Unit,
    onTightenToHard: (String) -> Unit,
    onRemoveRule: (String) -> Unit,
    onDisableProtection: () -> Unit,
) {
    Text("当前生效规则", style = MaterialTheme.typography.titleLarge)
    if (!protectionEnabled) {
        Text("保护已关闭", color = MaterialTheme.colorScheme.error)
    }
    if (savedRules.isEmpty()) {
        Text("尚未选择受限应用", style = MaterialTheme.typography.bodyLarge)
    } else {
        Text("已选择 ${savedRules.size} 个受限应用", style = MaterialTheme.typography.titleMedium)
        savedRules.forEach { savedRule ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(savedRule.displayName, style = MaterialTheme.typography.titleLarge)
                    Text("${savedRule.level.displayName} · 每日 ${savedRule.dailyAllowance.minutes} 分钟")
                    Text(savedRule.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onEditRule(savedRule.packageName) }) {
                        Text("编辑规则")
                    }
                    if (savedRule.level == RestrictionLevel.SOFT) {
                        Button(onClick = { onTightenToHard(savedRule.packageName) }) {
                            Text("立即改为强限制")
                        }
                    }
                    OutlinedButton(onClick = { onRemoveRule(savedRule.packageName) }) {
                        Text("次日删除强限制")
                    }
                }
            }
        }
    }
    Button(onClick = onOpenCatalog) {
        Text("选择受限应用")
    }
    if (protectionEnabled) {
        OutlinedButton(onClick = onDisableProtection) {
            Text("次日关闭保护")
        }
    }
    if (pendingRelaxations.isNotEmpty()) {
        Text("次日即将生效", style = MaterialTheme.typography.titleLarge)
        pendingRelaxations.forEach { pending ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = pending.displayText(),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private val RestrictionLevel.displayName: String
    get() = when (this) {
        RestrictionLevel.SOFT -> "弱限制"
        RestrictionLevel.HARD -> "强限制"
    }

private fun PendingRelaxation.displayText(): String = when (this) {
    is PendingRelaxation.ReplaceRule ->
        "${replacement.displayName}：改为${replacement.level.displayName}，每日额度 ${replacement.dailyAllowance.minutes} 分钟（$effectiveDate 生效）"
    is PendingRelaxation.RemoveRule ->
        "${app.displayName}：删除强限制（$effectiveDate 生效）"
    is PendingRelaxation.DisableProtection -> "关闭保护（$effectiveDate 生效）"
}

@Composable
private fun AppCatalogList(
    apps: List<InstalledApp>,
    onSelectApp: (String) -> Unit,
) {
    Text("选择设备上的应用", style = MaterialTheme.typography.titleLarge)
    if (apps.isEmpty()) {
        Text("没有找到可管理的应用")
    }
    apps.forEach { app ->
        OutlinedButton(
            onClick = { onSelectApp(app.packageName) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppIcon(app)
            Text(app.displayName, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun RestrictionEditor(
    editor: RestrictionEditorState,
    onChangeAllowance: (Int) -> Unit,
    onChangeLevel: (RestrictionLevel) -> Unit,
    onSave: () -> Unit,
    onCancelSelection: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(editor.app)
        Column {
            Text(editor.app.displayName, style = MaterialTheme.typography.titleLarge)
            Text(if (editor.level == RestrictionLevel.SOFT) "弱限制" else "强限制")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onChangeLevel(RestrictionLevel.SOFT) }) {
            Text("弱限制")
        }
        OutlinedButton(onClick = { onChangeLevel(RestrictionLevel.HARD) }) {
            Text("强限制")
        }
    }
    Text("每日额度", style = MaterialTheme.typography.titleMedium)
    Text("${editor.dailyAllowance.minutes} 分钟", style = MaterialTheme.typography.headlineMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { onChangeAllowance(editor.dailyAllowance.minutes - 5) },
        ) {
            Text("减少 5 分钟")
        }
        OutlinedButton(
            onClick = { onChangeAllowance(editor.dailyAllowance.minutes + 5) },
        ) {
            Text("增加 5 分钟")
        }
    }
    Button(onClick = onSave) {
        Text(if (editor.level == RestrictionLevel.SOFT) "保存弱限制" else "保存强限制")
    }
    OutlinedButton(onClick = onCancelSelection) {
        Text("取消选择")
    }
}

@Composable
private fun AppIcon(app: InstalledApp) {
    val bitmap = remember(app.iconPng) {
        app.iconPng?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "${app.displayName} 图标",
            modifier = Modifier.size(48.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "${app.displayName} 图标" },
            contentAlignment = Alignment.Center,
        ) {
            Text(app.displayName.take(1), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

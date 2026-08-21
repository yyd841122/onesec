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
        onChangeAllowance = { minutes ->
            controller.changeDailyAllowance(minutes)
            state = controller.state
        },
        onSave = {
            controller.saveRule()
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
    onChangeAllowance: (Int) -> Unit,
    onSave: () -> Unit,
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
                        onSave = onSave,
                    )
                    showingCatalog -> AppCatalogList(state.apps, onSelectApp)
                    else -> RestrictionSummary(state.savedRule, onOpenCatalog)
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
    savedRule: RestrictedAppRule?,
    onOpenCatalog: () -> Unit,
) {
    if (savedRule == null) {
        Text("尚未选择受限应用", style = MaterialTheme.typography.bodyLarge)
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(savedRule.displayName, style = MaterialTheme.typography.titleLarge)
                Text("强限制 · 每日 ${savedRule.dailyAllowance.minutes} 分钟")
                Text(savedRule.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Button(onClick = onOpenCatalog) {
        Text("选择受限应用")
    }
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
    onSave: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(editor.app)
        Column {
            Text(editor.app.displayName, style = MaterialTheme.typography.titleLarge)
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
        Text("保存强限制")
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

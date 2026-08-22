package com.example.onesec

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.Clock
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun InterventionScreen(
    intervention: ProtectionDecision.Intervene,
    zoneId: ZoneId,
    onReturnHome: () -> Unit,
    onWaitCompleted: () -> Unit = {},
    emergencyOverrideAvailable: Boolean = false,
    onEmergencyWaitStarted: () -> Unit = {},
    onEmergencyOverrideConfirmed: (String) -> Unit = {},
    clock: Clock = Clock.systemUTC(),
) {
    BackHandler(onBack = onReturnHome)
    OneSecTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Ⅱ", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (intervention.level == RestrictionLevel.SOFT) {
                    SoftRestrictionWait(clock, onWaitCompleted)
                } else {
                    Text("先停一下", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    StatusPill("强限制已生效", false)
                }
                Text(intervention.app.displayName, style = MaterialTheme.typography.headlineMedium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("今日已用 ${intervention.usedMinutes} 分钟", style = MaterialTheme.typography.titleLarge)
                        Text("每日额度已耗尽", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (intervention.level == RestrictionLevel.HARD) {
                            Text("下次重置：${formatResetTime(intervention, zoneId)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (intervention.level == RestrictionLevel.HARD) {
                    EmergencyOverridePanel(
                        available = emergencyOverrideAvailable,
                        onWaitStarted = onEmergencyWaitStarted,
                        onConfirmed = onEmergencyOverrideConfirmed,
                        clock = clock,
                    )
                }
                Button(onClick = onReturnHome, modifier = Modifier.fillMaxWidth()) {
                    Text("返回桌面")
                }
            }
        }
    }
}

@Composable
private fun EmergencyOverridePanel(
    available: Boolean,
    onWaitStarted: () -> Unit,
    onConfirmed: (String) -> Unit,
    clock: Clock,
) {
    var opened by remember { mutableStateOf(false) }
    if (!opened) {
        if (available) TextButton(onClick = { opened = true; onWaitStarted() }) { Text("需要紧急使用？") }
        return
    }
    val session = remember { EmergencyOverrideSession(clock.instant()) }
    var remaining by remember { mutableIntStateOf(60) }
    var waitComplete by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (!waitComplete) {
            when (val decision = session.waitDecision(clock.instant())) {
                WaitDecision.Complete -> waitComplete = true
                is WaitDecision.Waiting -> remaining = decision.remainingSeconds
            }
            delay(100)
        }
    }
    Text(if (waitComplete) "请填写紧急使用原因" else "紧急解锁需等待 $remaining 秒")
    if (waitComplete) {
        OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("原因") })
        Button(enabled = reason.isNotBlank(), onClick = { onConfirmed(reason.trim()) }) { Text("确认紧急解锁") }
    }
}

@Composable
private fun SoftRestrictionWait(clock: Clock, onWaitCompleted: () -> Unit) {
    val session = remember { SoftRestrictionWaitSession(clock.instant()) }
    var remainingSeconds by remember { mutableIntStateOf(AccessWindowManager.WAIT_DURATION.seconds.toInt()) }
    LaunchedEffect(Unit) {
        while (true) {
            when (val decision = session.decide(clock.instant())) {
                WaitDecision.Complete -> {
                    onWaitCompleted()
                    return@LaunchedEffect
                }
                is WaitDecision.Waiting -> remainingSeconds = decision.remainingSeconds
            }
            delay(100)
        }
    }
    Text("给自己一秒", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text("$remainingSeconds", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
    Text("深呼吸，然后再决定是否继续。", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun formatResetTime(
    intervention: ProtectionDecision.Intervene,
    zoneId: ZoneId,
): String = intervention.resetsAt
    .atZone(zoneId)
    .format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))

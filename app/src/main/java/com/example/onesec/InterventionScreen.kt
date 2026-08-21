package com.example.onesec

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
) {
    BackHandler(onBack = onReturnHome)
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (intervention.level == RestrictionLevel.SOFT) {
                    SoftRestrictionWait(onWaitCompleted)
                } else {
                    Text("强限制已生效", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                Text(intervention.app.displayName, style = MaterialTheme.typography.headlineMedium)
                Text("今日已用 ${intervention.usedMinutes} 分钟")
                Text("每日额度已耗尽")
                if (intervention.level == RestrictionLevel.HARD) {
                    Text("下次重置：${formatResetTime(intervention, zoneId)}")
                }
                Button(onClick = onReturnHome) {
                    Text("返回桌面")
                }
            }
        }
    }
}

@Composable
private fun SoftRestrictionWait(onWaitCompleted: () -> Unit) {
    val clock = remember { Clock.systemUTC() }
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
    Text("弱限制等待", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text("请等待 $remainingSeconds 秒")
}

private fun formatResetTime(
    intervention: ProtectionDecision.Intervene,
    zoneId: ZoneId,
): String = intervention.resetsAt
    .atZone(zoneId)
    .format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))

package com.example.onesec

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class InterventionActivity : ComponentActivity() {
    private var intervention by mutableStateOf<ProtectionDecision.Intervene?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intervention = AndroidInterventionIntent.read(intent)
        setContent {
            intervention?.let { currentIntervention ->
                InterventionScreen(
                    intervention = currentIntervention,
                    zoneId = java.time.ZoneId.systemDefault(),
                    onReturnHome = ::returnHome,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intervention = AndroidInterventionIntent.read(intent)
    }

    private fun returnHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }
}

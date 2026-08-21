package com.example.onesec

import android.content.Context
import android.content.Intent
import java.time.Instant

class AndroidInterventionPresenter(
    private val context: Context,
) : InterventionPresenter {
    override fun present(intervention: ProtectionDecision.Intervene) {
        context.startActivity(AndroidInterventionIntent.create(context, intervention))
    }
}

object AndroidInterventionIntent {
    fun create(
        context: Context,
        intervention: ProtectionDecision.Intervene,
    ): Intent = Intent(context, InterventionActivity::class.java)
        .putExtra(EXTRA_PACKAGE_NAME, intervention.app.packageName)
        .putExtra(EXTRA_DISPLAY_NAME, intervention.app.displayName)
        .putExtra(EXTRA_USED_MINUTES, intervention.usedMinutes)
        .putExtra(EXTRA_RESETS_AT, intervention.resetsAt.toEpochMilli())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    fun read(intent: Intent): ProtectionDecision.Intervene = ProtectionDecision.Intervene(
        app = InstalledApp(
            packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty(),
            displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME).orEmpty(),
        ),
        usedMinutes = intent.getIntExtra(EXTRA_USED_MINUTES, 0),
        resetsAt = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_RESETS_AT, 0)),
    )

    private const val EXTRA_PACKAGE_NAME = "restricted_package_name"
    private const val EXTRA_DISPLAY_NAME = "restricted_display_name"
    private const val EXTRA_USED_MINUTES = "used_minutes"
    private const val EXTRA_RESETS_AT = "resets_at"
}

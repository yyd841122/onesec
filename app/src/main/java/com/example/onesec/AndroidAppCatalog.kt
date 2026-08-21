package com.example.onesec

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

class AndroidAppCatalog(
    private val context: Context,
) : AppCatalog {
    override fun manageableApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val homePackages = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_ALL,
        ).mapTo(mutableSetOf()) { it.activityInfo.packageName }

        val candidates = packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_ALL,
        ).map { resolvedActivity ->
            val applicationInfo = resolvedActivity.activityInfo.applicationInfo
            AppCandidate(
                packageName = resolvedActivity.activityInfo.packageName,
                displayName = resolvedActivity.loadLabel(packageManager).toString(),
                isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                    applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
                isHomeApp = resolvedActivity.activityInfo.packageName in homePackages,
                iconPng = resolvedActivity.loadIcon(packageManager).toPng(),
            )
        }

        return manageableApps(candidates, context.packageName)
    }
}

private fun Drawable.toPng(): ByteArray {
    val bitmap = if (this is BitmapDrawable && bitmap != null) {
        bitmap
    } else {
        Bitmap.createBitmap(
            intrinsicWidth.coerceAtLeast(1),
            intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        ).also { bitmap ->
            setBounds(0, 0, bitmap.width, bitmap.height)
            draw(Canvas(bitmap))
        }
    }
    return ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        output.toByteArray()
    }
}

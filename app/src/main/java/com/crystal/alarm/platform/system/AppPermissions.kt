package com.crystal.alarm.platform.system

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/** Everything the alarm needs to reliably ring and be stopped. */
enum class AppPermission { Notifications, FullScreen, ExactAlarm, Camera, Overlay }

object AppPermissions {

    fun missing(context: Context): List<AppPermission> = AppPermission.entries.filterNot { isGranted(context, it) }

    fun isGranted(context: Context, permission: AppPermission): Boolean = when (permission) {
        AppPermission.Notifications ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        AppPermission.FullScreen ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                context.getSystemService<NotificationManager>()?.canUseFullScreenIntent() == true
        AppPermission.ExactAlarm ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
        AppPermission.Camera -> context.hasPermission(Manifest.permission.CAMERA)
        AppPermission.Overlay -> Settings.canDrawOverlays(context)
    }

    /** Runtime permission to request with a launcher, or null if it's a special settings toggle. */
    fun runtimePermission(permission: AppPermission): String? = when (permission) {
        AppPermission.Notifications ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        AppPermission.Camera -> Manifest.permission.CAMERA
        else -> null
    }

    fun settingsIntent(context: Context, permission: AppPermission): Intent {
        val packageUri = Uri.fromParts("package", context.packageName, null)
        return when {
            permission == AppPermission.FullScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)
            permission == AppPermission.Overlay -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
            permission == AppPermission.ExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        }
    }

    private fun Context.hasPermission(name: String) =
        ContextCompat.checkSelfPermission(this, name) == PackageManager.PERMISSION_GRANTED
}

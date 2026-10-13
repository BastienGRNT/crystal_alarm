package com.crystal.alarm.presentation.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.crystal.alarm.R
import com.crystal.alarm.platform.system.AppPermission
import com.crystal.alarm.platform.system.AppPermissions
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType

/** Lists missing permissions with one-tap fixes. Renders nothing when all is granted. */
@Composable
fun PermissionsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var missing by remember { mutableStateOf(AppPermissions.missing(context)) }
    var denied by remember { mutableStateOf(emptySet<AppPermission>()) }
    var pending by remember { mutableStateOf<AppPermission?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.let { if (!granted) denied = denied + it }
        missing = AppPermissions.missing(context)
    }
    LifecycleResumeEffect(Unit) {
        missing = AppPermissions.missing(context)
        onPauseOrDispose { }
    }

    if (missing.isEmpty()) return

    CrystalCard(modifier) {
        InfoRow(
            icon = rememberVectorPainter(Icons.Rounded.Warning),
            title = stringResource(R.string.permissions_title),
            subtitle = stringResource(R.string.permissions_subtitle),
            tint = CrystalColors.Warning,
        )
        Spacer(Modifier.height(12.dp))
        missing.forEach { permission ->
            PermissionLine(
                label = stringResource(permission.labelRes()),
                onClick = {
                    val runtime = AppPermissions.runtimePermission(permission)
                    if (runtime != null && permission !in denied) {
                        pending = permission
                        launcher.launch(runtime)
                    } else {
                        context.startActivity(AppPermissions.settingsIntent(context, permission))
                    }
                },
            )
        }
    }
}

@Composable
private fun PermissionLine(label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = CrystalType.Body, color = CrystalColors.Text, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            Text(stringResource(R.string.action_allow), style = CrystalType.Label, color = CrystalColors.Accent)
        }
    }
}

fun AppPermission.labelRes(): Int = when (this) {
    AppPermission.Notifications -> R.string.permission_notifications
    AppPermission.FullScreen -> R.string.permission_full_screen
    AppPermission.ExactAlarm -> R.string.permission_exact_alarm
    AppPermission.Camera -> R.string.permission_camera
    AppPermission.Overlay -> R.string.permission_overlay
}

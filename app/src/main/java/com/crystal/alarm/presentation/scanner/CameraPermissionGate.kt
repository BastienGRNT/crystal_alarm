package com.crystal.alarm.presentation.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.crystal.alarm.R
import com.crystal.alarm.presentation.components.CrystalButton
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType

/** Shows [content] once the camera permission is granted, asks for it otherwise. */
@Composable
fun CameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    fun isGranted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(isGranted()) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }

    LifecycleResumeEffect(Unit) {
        granted = isGranted()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (granted) {
        content()
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.camera_permission_title),
            style = CrystalType.Title,
            color = CrystalColors.Text,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.camera_permission_text),
            style = CrystalType.Body,
            color = CrystalColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        CrystalButton(
            text = stringResource(if (asked) R.string.action_open_settings else R.string.action_allow_camera),
            icon = painterResource(R.drawable.ic_qr),
            onClick = {
                if (asked) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            },
        )
    }
}

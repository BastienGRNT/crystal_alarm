package com.crystal.alarm.presentation.ringing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.R
import com.crystal.alarm.presentation.components.FullWidthButton
import com.crystal.alarm.presentation.register.ScannerTopBar
import com.crystal.alarm.presentation.rescue.RescueScreen
import com.crystal.alarm.presentation.scanner.CameraPermissionGate
import com.crystal.alarm.presentation.scanner.CodeScanner
import com.crystal.alarm.presentation.scanner.ScanFeedback
import com.crystal.alarm.presentation.scanner.ScannerOverlay
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun RingingScreen(viewModel: RingingViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(CrystalColors.Background),
    ) {
        AnimatedContent(
            targetState = state.mode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ringingMode",
        ) { mode ->
            when (mode) {
                RingingMode.Ring -> RingView(
                    codeName = state.codeName,
                    onScan = viewModel::onScanRequested,
                    onRescue = viewModel::onRescueRequested,
                )
                RingingMode.Scan -> ScanView(
                    feedback = state.feedback,
                    onScanned = viewModel::onScanned,
                    onBack = viewModel::onBack,
                )
                RingingMode.Rescue -> RescueScreen(onBack = viewModel::onBack)
            }
        }
    }
}

@Composable
private fun RingView(codeName: String?, onScan: () -> Unit, onRescue: () -> Unit) {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000)
        }
    }
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseValue",
    )

    Box(Modifier.fillMaxSize()) {
        // Breathing crystal glow.
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height * 0.38f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CrystalColors.AccentStrong.copy(alpha = 0.55f * pulse), Color.Transparent),
                    center = center,
                    radius = size.width * (0.55f + 0.2f * pulse),
                ),
                radius = size.width * (0.55f + 0.2f * pulse),
                center = center,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CrystalColors.Ice.copy(alpha = 0.25f * pulse), Color.Transparent),
                    center = center,
                    radius = size.width * 0.35f,
                ),
                radius = size.width * 0.35f,
                center = center,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM")).replaceFirstChar { it.uppercase() },
                style = CrystalType.Label,
                color = CrystalColors.TextMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = CrystalType.Hero.copy(fontSize = 112.sp, lineHeight = 116.sp),
                color = CrystalColors.Text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ringing_title),
                style = CrystalType.Title,
                color = CrystalColors.Text,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = codeName?.let { stringResource(R.string.ringing_subtitle_named, it) }
                    ?: stringResource(R.string.ringing_subtitle),
                style = CrystalType.Body,
                color = CrystalColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1.3f))
            FullWidthButton(
                text = stringResource(R.string.action_scan_code),
                icon = painterResource(R.drawable.ic_qr),
                onClick = onScan,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRescue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_no_code), style = CrystalType.Label, color = CrystalColors.TextMuted)
            }
        }
    }
}

@Composable
private fun ScanView(
    feedback: ScanFeedback,
    onScanned: (com.crystal.alarm.presentation.scanner.ScannedCode) -> Unit,
    onBack: () -> Unit,
) {
    var torch by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        CameraPermissionGate {
            CodeScanner(
                onCode = onScanned,
                paused = feedback != ScanFeedback.Idle,
                torchOn = torch,
                modifier = Modifier.fillMaxSize(),
            )
            ScannerOverlay(feedback = feedback, modifier = Modifier.fillMaxSize())
        }
        ScannerTopBar(
            title = stringResource(R.string.ringing_scan_title),
            onBack = onBack,
            torchOn = torch,
            onToggleTorch = { torch = !torch },
        )
        Text(
            text = stringResource(
                when (feedback) {
                    ScanFeedback.Idle -> R.string.ringing_scan_hint_generic
                    ScanFeedback.Success -> R.string.ringing_scan_success
                    ScanFeedback.Error -> R.string.ringing_scan_wrong
                },
            ),
            style = CrystalType.BodyStrong,
            color = when (feedback) {
                ScanFeedback.Idle -> CrystalColors.Text
                ScanFeedback.Success -> CrystalColors.Success
                ScanFeedback.Error -> CrystalColors.Danger
            },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 48.dp),
        )
    }
}

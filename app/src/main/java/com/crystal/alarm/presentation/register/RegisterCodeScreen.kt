package com.crystal.alarm.presentation.register

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.R
import com.crystal.alarm.presentation.components.ButtonStyle
import com.crystal.alarm.presentation.components.CircleIconButton
import com.crystal.alarm.presentation.components.CrystalButton
import com.crystal.alarm.presentation.components.CrystalTextField
import com.crystal.alarm.presentation.scanner.CameraPermissionGate
import com.crystal.alarm.presentation.scanner.CodeScanner
import com.crystal.alarm.presentation.scanner.ScanFeedback
import com.crystal.alarm.presentation.scanner.ScannedCode
import com.crystal.alarm.presentation.scanner.ScannerOverlay
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType

@Composable
fun RegisterCodeScreen(
    onDone: () -> Unit,
    viewModel: RegisterCodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detected = state.detected
    var torch by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.saved.collect { onDone() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(CrystalColors.Background),
    ) {
        CameraPermissionGate {
            CodeScanner(
                onCode = viewModel::onScanned,
                paused = detected != null,
                torchOn = torch,
                modifier = Modifier.fillMaxSize(),
            )
            ScannerOverlay(
                feedback = if (detected != null) ScanFeedback.Success else ScanFeedback.Idle,
                modifier = Modifier.fillMaxSize(),
            )
        }

        ScannerTopBar(
            title = stringResource(R.string.register_title),
            onBack = onDone,
            torchOn = torch,
            onToggleTorch = { torch = !torch },
        )

        AnimatedContent(
            targetState = detected,
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            label = "registerSheet",
        ) { code ->
            if (code == null) {
                HintSheet()
            } else {
                ConfirmSheet(
                    code = code,
                    state = state,
                    onNameChange = viewModel::onNameChange,
                    onSave = viewModel::onSave,
                    onRetry = viewModel::onRetry,
                    onUseExisting = viewModel::onUseExisting,
                )
            }
        }
    }
}

@Composable
fun ScannerTopBar(
    title: String,
    onBack: () -> Unit,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = rememberVectorPainter(Icons.AutoMirrored.Rounded.ArrowBack),
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack,
            container = Color.Black.copy(alpha = 0.45f),
        )
        Spacer(Modifier.width(16.dp))
        Text(title, style = CrystalType.Title, color = CrystalColors.Text, modifier = Modifier.weight(1f))
        CircleIconButton(
            icon = painterResource(R.drawable.ic_flash),
            contentDescription = stringResource(R.string.action_torch),
            onClick = onToggleTorch,
            tint = if (torchOn) CrystalColors.Background else CrystalColors.Text,
            container = if (torchOn) CrystalColors.Text else Color.Black.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun SheetSurface(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CrystalColors.Surface)
            .padding(24.dp),
    ) { content() }
}

@Composable
private fun HintSheet() = SheetSurface {
    Text(stringResource(R.string.register_hint_title), style = CrystalType.BodyStrong, color = CrystalColors.Text)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.register_hint_text), style = CrystalType.Caption, color = CrystalColors.TextMuted)
}

@Composable
private fun ConfirmSheet(
    code: ScannedCode,
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onUseExisting: () -> Unit,
) = SheetSurface {
    Text(
        stringResource(R.string.register_detected, code.format).uppercase(),
        style = CrystalType.Overline,
        color = CrystalColors.Success,
    )
    Spacer(Modifier.height(4.dp))
    Text(code.value, style = CrystalType.Caption, color = CrystalColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)

    val existing = state.alreadySaved
    if (existing != null) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.register_already_saved, existing.name), style = CrystalType.BodyStrong, color = CrystalColors.Text)
        Spacer(Modifier.height(20.dp))
        Row {
            CrystalButton(stringResource(R.string.action_retry), onRetry, Modifier.weight(1f), ButtonStyle.Secondary)
            Spacer(Modifier.width(12.dp))
            CrystalButton(stringResource(R.string.action_use_this_code), onUseExisting, Modifier.weight(1.4f))
        }
        return@SheetSurface
    }

    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.register_name_label), style = CrystalType.Label, color = CrystalColors.Text)
    Spacer(Modifier.height(8.dp))
    CrystalTextField(
        value = state.name,
        onValueChange = onNameChange,
        placeholder = stringResource(R.string.register_name_placeholder),
        onDone = onSave,
    )
    Spacer(Modifier.height(20.dp))
    Row {
        CrystalButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetry,
            style = ButtonStyle.Secondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        CrystalButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.weight(1.4f),
        )
    }
}

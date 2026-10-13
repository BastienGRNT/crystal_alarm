package com.crystal.alarm.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.R
import com.crystal.alarm.presentation.components.FullWidthButton
import com.crystal.alarm.presentation.components.ScreenHeader
import com.crystal.alarm.presentation.components.WheelPicker
import com.crystal.alarm.presentation.format.AlarmFormat
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType

@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val res = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is EditorEvent.Saved) onBack() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrystalColors.Background)
            .navigationBarsPadding(),
    ) {
        val form = state.form
        ScreenHeader(
            title = stringResource(if (form?.isNew != false) R.string.editor_title_new else R.string.editor_title_edit),
            onBack = onBack,
        )
        if (form == null) return@Column

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            TimeWheels(
                hour = form.hour,
                minute = form.minute,
                onHourChange = viewModel::onHourChange,
                onMinuteChange = viewModel::onMinuteChange,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = state.ringsIn?.let { stringResource(R.string.editor_rings_in, AlarmFormat.countdown(res, it)) }.orEmpty(),
                style = CrystalType.BodyStrong,
                color = CrystalColors.Accent,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.editor_one_shot),
                style = CrystalType.Caption,
                color = CrystalColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            state.error?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    style = CrystalType.Caption,
                    color = CrystalColors.Danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            }
            FullWidthButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::onSave,
                enabled = !state.saving,
            )
        }
    }
}

@Composable
private fun TimeWheels(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        // Selection band behind the centered row.
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(CrystalColors.Surface),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            WheelPicker(count = 24, selected = hour, onSelected = onHourChange) { "%02d".format(it) }
            Text(":", style = CrystalType.Display, color = CrystalColors.TextMuted, modifier = Modifier.padding(bottom = 6.dp))
            WheelPicker(count = 60, selected = minute, onSelected = onMinuteChange) { "%02d".format(it) }
        }
        Spacer(Modifier.width(0.dp))
    }
}

private fun EditorError.messageRes(): Int = when (this) {
    EditorError.Locked -> R.string.editor_error_locked
    EditorError.MissingWakeCode -> R.string.editor_error_no_code
    EditorError.Ringing -> R.string.editor_error_ringing
}

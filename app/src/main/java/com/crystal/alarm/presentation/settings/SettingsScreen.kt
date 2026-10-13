package com.crystal.alarm.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.BuildConfig
import com.crystal.alarm.R
import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.rescue.RescueChallenge
import com.crystal.alarm.presentation.components.CrystalCard
import com.crystal.alarm.presentation.components.InfoRow
import com.crystal.alarm.presentation.components.PermissionsCard
import com.crystal.alarm.presentation.components.ScreenHeader
import com.crystal.alarm.presentation.components.SectionLabel
import com.crystal.alarm.presentation.format.AlarmFormat
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRegisterCode: () -> Unit,
    onOpenSound: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    state.message?.let {
        AlertDialog(
            onDismissRequest = viewModel::onMessageShown,
            containerColor = CrystalColors.SurfaceHigh,
            text = { Text(stringResource(R.string.code_delete_last_with_alarm), color = CrystalColors.Text) },
            confirmButton = {
                TextButton(onClick = viewModel::onMessageShown) { Text(stringResource(R.string.action_ok), color = CrystalColors.Accent) }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrystalColors.Background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.settings_title), onBack = onBack)
        if (settings == null) return@Column

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_section_code))
            WakeCodesCard(
                codes = settings.wakeCodes,
                activeId = settings.activeWakeCodeId,
                onSelect = viewModel::onSelectCode,
                onRename = viewModel::onRenameCode,
                onDelete = viewModel::onDeleteCode,
                onAdd = onRegisterCode,
            )
            Text(
                stringResource(R.string.settings_code_hint),
                style = CrystalType.Caption,
                color = CrystalColors.TextMuted,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp),
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.settings_section_sound))
            SoundCard(settings, state.ringtone?.name, onOpenSound)

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.settings_section_lock))
            LockCard(settings.lockMinutes, state, viewModel::onLockMinutesChange)

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.settings_section_rescue))
            CrystalCard {
                Text(
                    stringResource(R.string.settings_rescue_text, RescueChallenge.REQUIRED_ANSWERS),
                    style = CrystalType.Body,
                    color = CrystalColors.TextMuted,
                )
            }

            PermissionsCard(Modifier.padding(top = 28.dp))

            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_footer, BuildConfig.VERSION_NAME),
                style = CrystalType.Caption,
                color = CrystalColors.TextFaint,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SoundCard(settings: Settings, ringtoneName: String?, onOpenSound: () -> Unit) {
    val sound = settings.sound
    val details = listOfNotNull(
        stringResource(R.string.sound_volume_value, sound.volumePercent),
        stringResource(if (sound.progressive) R.string.sound_mode_progressive else R.string.sound_mode_direct),
        stringResource(R.string.sound_vibrate).takeIf { sound.vibrate },
    ).joinToString(" · ")
    CrystalCard(onClick = onOpenSound) {
        InfoRow(
            icon = rememberVectorPainter(Icons.Rounded.Notifications),
            title = ringtoneName ?: stringResource(R.string.sound_title),
            subtitle = details,
            trailing = {
                Icon(
                    rememberVectorPainter(Icons.AutoMirrored.Rounded.KeyboardArrowRight),
                    contentDescription = null,
                    tint = CrystalColors.TextMuted,
                )
            },
        )
    }
}

@Composable
private fun LockCard(minutes: Int, state: SettingsUiState, onChange: (Int) -> Unit) {
    val res = LocalResources.current
    val lock = state.activeLock
    var sliderValue by remember(minutes) { mutableFloatStateOf(minutes.toFloat()) }
    val steps = (Settings.MAX_LOCK_MINUTES - Settings.MIN_LOCK_MINUTES) / Settings.LOCK_STEP_MINUTES - 1

    CrystalCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.duration_minutes, sliderValue.roundToInt()),
                style = CrystalType.Display,
                color = if (lock == null) CrystalColors.Text else CrystalColors.TextMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onChange(sliderValue.roundToInt()) },
            valueRange = Settings.MIN_LOCK_MINUTES.toFloat()..Settings.MAX_LOCK_MINUTES.toFloat(),
            steps = steps,
            enabled = lock == null,
            colors = SliderDefaults.colors(
                thumbColor = CrystalColors.Text,
                activeTrackColor = CrystalColors.Accent,
                inactiveTrackColor = CrystalColors.SurfaceHigh,
                activeTickColor = CrystalColors.Accent,
                inactiveTickColor = CrystalColors.SurfaceHigh,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (lock != null) {
            InfoRow(
                icon = rememberVectorPainter(Icons.Rounded.Lock),
                title = stringResource(R.string.lock_title),
                subtitle = stringResource(
                    R.string.settings_lock_locked,
                    AlarmFormat.shortDuration(res, lock.remaining(state.now)),
                ),
            )
        } else {
            Text(stringResource(R.string.settings_lock_text), style = CrystalType.Caption, color = CrystalColors.TextMuted)
        }
    }
}

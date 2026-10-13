package com.crystal.alarm.presentation.sound

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.R
import com.crystal.alarm.domain.model.Ringtone
import com.crystal.alarm.domain.model.RingtoneCategory
import com.crystal.alarm.domain.model.RingtoneKind
import com.crystal.alarm.domain.model.SoundSettings
import com.crystal.alarm.presentation.components.ButtonStyle
import com.crystal.alarm.presentation.components.CardShape
import com.crystal.alarm.presentation.components.CrystalCard
import com.crystal.alarm.presentation.components.FullWidthButton
import com.crystal.alarm.presentation.components.ScreenHeader
import com.crystal.alarm.presentation.components.SectionLabel
import com.crystal.alarm.presentation.components.SegmentedControl
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType
import kotlin.math.roundToInt

@Composable
fun SoundScreen(
    onBack: () -> Unit,
    viewModel: SoundViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sound = state.sound
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onImport(it.toString()) }
    }
    DisposableEffect(Unit) { onDispose { viewModel.stopPreview() } }

    if (state.importFailed) {
        AlertDialog(
            onDismissRequest = viewModel::onImportErrorShown,
            containerColor = CrystalColors.SurfaceHigh,
            text = { Text(stringResource(R.string.sound_import_failed), color = CrystalColors.Text) },
            confirmButton = {
                TextButton(onClick = viewModel::onImportErrorShown) { Text(stringResource(R.string.action_ok), color = CrystalColors.Accent) }
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CrystalColors.Background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.sound_title), onBack = onBack)
        if (sound == null) return@Column

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            SectionLabel(stringResource(R.string.sound_section_volume))
            VolumeCard(sound.volumePercent, viewModel::onVolumeChange)

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.sound_section_start))
            SegmentedControl(
                options = listOf(stringResource(R.string.sound_mode_direct), stringResource(R.string.sound_mode_progressive)),
                selectedIndex = if (sound.progressive) 1 else 0,
                onSelect = { viewModel.onProgressiveChange(it == 1) },
            )
            Text(
                text = if (sound.progressive) {
                    stringResource(R.string.sound_mode_progressive_hint, SoundSettings.RAMP_SECONDS)
                } else {
                    stringResource(R.string.sound_mode_direct_hint)
                },
                style = CrystalType.Caption,
                color = CrystalColors.TextMuted,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp),
            )

            Spacer(Modifier.height(20.dp))
            CrystalCard(onClick = { viewModel.onVibrateChange(!sound.vibrate) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.sound_vibrate), style = CrystalType.BodyStrong, color = CrystalColors.Text)
                        Text(stringResource(R.string.sound_vibrate_hint), style = CrystalType.Caption, color = CrystalColors.TextMuted)
                    }
                    Switch(
                        checked = sound.vibrate,
                        onCheckedChange = viewModel::onVibrateChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CrystalColors.Background,
                            checkedTrackColor = CrystalColors.Accent,
                            uncheckedThumbColor = CrystalColors.TextMuted,
                            uncheckedTrackColor = CrystalColors.SurfaceHigh,
                            uncheckedBorderColor = CrystalColors.SurfaceHigh,
                        ),
                    )
                }
            }

            // One card per category, in RingtoneCategory order.
            state.ringtones.groupBy { it.category }.toSortedMap().forEach { (category, ringtones) ->
                Spacer(Modifier.height(28.dp))
                SectionLabel(stringResource(category.labelRes()))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(CrystalColors.Surface),
                ) {
                    ringtones.forEachIndexed { index, ringtone ->
                        if (index > 0) HorizontalDivider(color = CrystalColors.Background, thickness = 1.dp)
                        RingtoneRow(
                            ringtone = ringtone,
                            selected = ringtone.id == sound.ringtoneId,
                            previewing = ringtone.id == state.previewingId,
                            onSelect = { viewModel.onSelect(ringtone) },
                            onTogglePreview = { viewModel.onTogglePreview(ringtone) },
                            onDelete = { viewModel.onDelete(ringtone) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            FullWidthButton(
                text = stringResource(if (state.importing) R.string.sound_importing else R.string.action_import_sound),
                icon = rememberVectorPainter(Icons.Rounded.Add),
                onClick = { importer.launch(arrayOf("audio/*")) },
                enabled = !state.importing,
                style = ButtonStyle.Secondary,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VolumeCard(volume: Int, onChange: (Int) -> Unit) {
    var value by remember(volume) { mutableFloatStateOf(volume.toFloat()) }
    val steps = (SoundSettings.MAX_VOLUME - SoundSettings.MIN_VOLUME) / SoundSettings.VOLUME_STEP - 1
    CrystalCard {
        Text(stringResource(R.string.sound_volume_value, value.roundToInt()), style = CrystalType.Display, color = CrystalColors.Text)
        Spacer(Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onChange(value.roundToInt()) },
            valueRange = SoundSettings.MIN_VOLUME.toFloat()..SoundSettings.MAX_VOLUME.toFloat(),
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = CrystalColors.Text,
                activeTrackColor = CrystalColors.Accent,
                inactiveTrackColor = CrystalColors.SurfaceHigh,
                activeTickColor = CrystalColors.Accent,
                inactiveTickColor = CrystalColors.SurfaceHigh,
            ),
        )
        Text(stringResource(R.string.sound_volume_hint), style = CrystalType.Caption, color = CrystalColors.TextMuted)
    }
}

@Composable
private fun RingtoneRow(
    ringtone: Ringtone,
    selected: Boolean,
    previewing: Boolean,
    onSelect: () -> Unit,
    onTogglePreview: () -> Unit,
    onDelete: () -> Unit,
) {
    val bubble by animateColorAsState(if (selected) CrystalColors.Accent else CrystalColors.SurfaceHigh, label = "ringtoneBubble")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(bubble),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(rememberVectorPainter(Icons.Rounded.Check), null, tint = CrystalColors.Background, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(ringtone.name, style = CrystalType.BodyStrong, color = CrystalColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val description = ringtone.description
                ?: stringResource(R.string.ringtone_imported_description).takeIf { ringtone.kind == RingtoneKind.Imported }
            if (description != null) {
                Text(description, style = CrystalType.Caption, color = CrystalColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (ringtone.kind == RingtoneKind.Imported) {
            IconButton(onClick = onDelete) {
                Icon(rememberVectorPainter(Icons.Rounded.Delete), stringResource(R.string.action_delete), tint = CrystalColors.TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        IconButton(onClick = onTogglePreview) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (previewing) CrystalColors.Text else CrystalColors.SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = if (previewing) painterResource(R.drawable.ic_stop) else rememberVectorPainter(Icons.Rounded.PlayArrow),
                    contentDescription = stringResource(if (previewing) R.string.action_stop_preview else R.string.action_preview),
                    tint = if (previewing) CrystalColors.Background else CrystalColors.Text,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun RingtoneCategory.labelRes(): Int = when (this) {
    RingtoneCategory.Classic -> R.string.ringtone_category_classic
    RingtoneCategory.Soft -> R.string.ringtone_category_soft
    RingtoneCategory.Electro -> R.string.ringtone_category_electro
    RingtoneCategory.Techno -> R.string.ringtone_category_techno
    RingtoneCategory.FunkPop -> R.string.ringtone_category_funk_pop
    RingtoneCategory.Phone -> R.string.ringtone_category_phone
    RingtoneCategory.Mine -> R.string.ringtone_category_mine
}

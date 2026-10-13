package com.crystal.alarm.presentation.home

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.R
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.presentation.components.ButtonStyle
import com.crystal.alarm.presentation.components.CircleIconButton
import com.crystal.alarm.presentation.components.CrystalCard
import com.crystal.alarm.presentation.components.CrystalProgress
import com.crystal.alarm.presentation.components.FullWidthButton
import com.crystal.alarm.presentation.components.InfoRow
import com.crystal.alarm.presentation.components.PermissionsCard
import com.crystal.alarm.presentation.components.pressScale
import com.crystal.alarm.presentation.format.AlarmFormat
import com.crystal.alarm.presentation.ringing.RingingActivity
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType
import java.time.Duration

@Composable
fun HomeScreen(
    onEditAlarm: () -> Unit,
    onOpenSettings: () -> Unit,
    onRegisterCode: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? HomeUiState.Ready ?: return
    val context = LocalContext.current

    val isRinging = ready.dashboard.alarm is AlarmState.Ringing
    // Coming back to the app while ringing always lands on the ringing screen.
    LifecycleResumeEffect(isRinging) {
        if (isRinging) context.startActivity(Intent(context, RingingActivity::class.java))
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrystalColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        TopBar(onOpenSettings)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(40.dp))
            Hero(ready, onEdit = onEditAlarm, onToggle = viewModel::onToggleAlarm)
            Spacer(Modifier.height(40.dp))
            StatusCards(ready, onRegisterCode, onOpenHistory)
            Spacer(Modifier.height(24.dp))
        }
        BottomActions(
            state = ready,
            onEdit = onEditAlarm,
            onRegisterCode = onRegisterCode,
            onStopRinging = { context.startActivity(Intent(context, RingingActivity::class.java)) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_wordmark),
            style = CrystalType.Title.merge(TextStyle(brush = CrystalColors.Crystal)),
        )
        Spacer(Modifier.weight(1f))
        CircleIconButton(
            icon = rememberVectorPainter(Icons.Rounded.Settings),
            contentDescription = stringResource(R.string.settings_title),
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun Hero(state: HomeUiState.Ready, onEdit: () -> Unit, onToggle: (Boolean) -> Unit) {
    val res = LocalResources.current
    val alarmState = state.dashboard.alarm
    // While disabled, the last time is still shown (greyed) so it can be turned back on.
    val alarmTime = when (alarmState) {
        is AlarmState.Scheduled -> alarmState.alarm.time
        is AlarmState.Ringing -> alarmState.alarm.time
        AlarmState.None -> state.dashboard.lastTime
    }
    val enabled = alarmState !is AlarmState.None

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(
                when {
                    alarmState is AlarmState.Ringing -> R.string.home_ringing
                    alarmState is AlarmState.Scheduled -> R.string.home_next_alarm
                    alarmTime != null -> R.string.home_alarm_disabled
                    else -> R.string.home_no_alarm
                },
            ),
            style = CrystalType.Label,
            color = if (alarmState is AlarmState.Ringing) CrystalColors.Danger else CrystalColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
        if (alarmTime != null && !state.isRinging) {
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                // Turning off is always possible; turning on follows the creation rules.
                enabled = enabled || (state.hasWakeCode && state.activeLock == null),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CrystalColors.Background,
                    checkedTrackColor = CrystalColors.Accent,
                    uncheckedThumbColor = CrystalColors.TextMuted,
                    uncheckedTrackColor = CrystalColors.SurfaceHigh,
                    uncheckedBorderColor = CrystalColors.SurfaceHigh,
                    disabledUncheckedThumbColor = CrystalColors.TextFaint,
                    disabledUncheckedTrackColor = CrystalColors.Surface,
                    disabledUncheckedBorderColor = CrystalColors.Surface,
                ),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    val interaction = remember { MutableInteractionSource() }
    AnimatedContent(
        targetState = alarmTime?.let { AlarmFormat.time(it) } ?: "--:--",
        transitionSpec = {
            (slideInVertically { it / 3 } + fadeIn()) togetherWith (slideOutVertically { -it / 3 } + fadeOut())
        },
        label = "heroTime",
        // Tapping the time opens the editor, like the "Modifier" button.
        modifier = Modifier
            .pressScale(interaction)
            .clickable(
                enabled = state.canEdit,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = stringResource(R.string.action_edit),
                onClick = onEdit,
            ),
    ) { time ->
        Text(
            text = time,
            style = CrystalType.Hero,
            color = when {
                enabled -> CrystalColors.Text
                alarmTime != null -> CrystalColors.TextMuted
                else -> CrystalColors.TextFaint
            },
        )
    }

    if (alarmState is AlarmState.Scheduled) {
        val trigger = alarmState.alarm.triggerAt
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(AlarmFormat.relativeDay(res, trigger, state.now, state.zone))
            Spacer(Modifier.width(10.dp))
            Text(
                text = AlarmFormat.countdown(res, Duration.between(state.now, trigger)),
                style = CrystalType.Body,
                color = CrystalColors.TextMuted,
            )
        }
    } else if (alarmState is AlarmState.None && alarmTime == null) {
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.home_tagline), style = CrystalType.Body, color = CrystalColors.TextMuted)
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(CrystalColors.Accent.copy(alpha = 0.22f), CrystalColors.Ice.copy(alpha = 0.16f))))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = CrystalType.Label, color = CrystalColors.Text)
    }
}

@Composable
private fun StatusCards(state: HomeUiState.Ready, onRegisterCode: () -> Unit, onOpenHistory: () -> Unit) {
    val res = LocalResources.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(visible = state.activeLock != null) {
            val lock = state.activeLock ?: return@AnimatedVisibility
            CrystalCard {
                InfoRow(
                    icon = rememberVectorPainter(Icons.Rounded.Lock),
                    title = stringResource(R.string.lock_title),
                    subtitle = stringResource(R.string.lock_subtitle, AlarmFormat.shortDuration(res, lock.remaining(state.now))),
                )
                Spacer(Modifier.height(16.dp))
                CrystalProgress(lock.progress(state.now))
            }
        }
        if (!state.hasWakeCode) {
            CrystalCard(onClick = onRegisterCode) {
                InfoRow(
                    icon = painterResource(R.drawable.ic_qr),
                    title = stringResource(R.string.code_missing_title),
                    subtitle = stringResource(R.string.code_missing_subtitle),
                    tint = CrystalColors.Ice,
                )
            }
        }
        if (state.dashboard.alarm is AlarmState.Ringing) {
            CrystalCard {
                InfoRow(
                    icon = rememberVectorPainter(Icons.Rounded.Notifications),
                    title = stringResource(R.string.home_ringing),
                    subtitle = stringResource(R.string.notification_ringing_text),
                    tint = CrystalColors.Danger,
                )
            }
        }
        LastWakeUpCard(state, onOpenHistory)
        PermissionsCard()
    }
}

@Composable
private fun LastWakeUpCard(state: HomeUiState.Ready, onOpenHistory: () -> Unit) {
    val res = LocalResources.current
    val last = state.wakeUps.last ?: return
    val average = state.wakeUps.average ?: return
    // While ringing, only the ringing card matters.
    if (state.dashboard.alarm is AlarmState.Ringing) return
    CrystalCard(onClick = onOpenHistory) {
        InfoRow(
            icon = painterResource(R.drawable.ic_stat_alarm),
            title = stringResource(R.string.home_last_wake_title, AlarmFormat.preciseDuration(res, last.duration)),
            subtitle = stringResource(R.string.home_last_wake_subtitle, AlarmFormat.preciseDuration(res, average)),
            tint = CrystalColors.Ice,
        )
    }
}

@Composable
private fun BottomActions(
    state: HomeUiState.Ready,
    onEdit: () -> Unit,
    onRegisterCode: () -> Unit,
    onStopRinging: () -> Unit,
) {
    val res = LocalResources.current
    val lock = state.activeLock
    val hasSetup = state.dashboard.alarm is AlarmState.Scheduled || state.dashboard.isDisabled
    when {
        state.isRinging -> FullWidthButton(
            text = stringResource(R.string.action_stop_alarm),
            icon = painterResource(R.drawable.ic_qr),
            onClick = onStopRinging,
        )
        !state.hasWakeCode -> FullWidthButton(
            text = stringResource(R.string.action_register_code_first),
            icon = painterResource(R.drawable.ic_qr),
            onClick = onRegisterCode,
        )
        else -> FullWidthButton(
            text = when {
                lock != null -> stringResource(R.string.action_locked, AlarmFormat.shortDuration(res, lock.remaining(state.now)))
                hasSetup -> stringResource(R.string.action_edit)
                else -> stringResource(R.string.action_new_alarm)
            },
            onClick = onEdit,
            enabled = lock == null,
            style = if (hasSetup) ButtonStyle.Secondary else ButtonStyle.Primary,
        )
    }
}

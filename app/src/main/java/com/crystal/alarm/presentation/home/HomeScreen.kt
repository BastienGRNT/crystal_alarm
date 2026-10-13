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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.R
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.presentation.components.ButtonStyle
import com.crystal.alarm.presentation.components.CircleIconButton
import com.crystal.alarm.presentation.components.CrystalButton
import com.crystal.alarm.presentation.components.CrystalCard
import com.crystal.alarm.presentation.components.CrystalProgress
import com.crystal.alarm.presentation.components.FullWidthButton
import com.crystal.alarm.presentation.components.InfoRow
import com.crystal.alarm.presentation.components.PermissionsCard
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
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? HomeUiState.Ready ?: return
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

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
            Hero(ready)
            Spacer(Modifier.height(40.dp))
            StatusCards(ready, onRegisterCode)
            Spacer(Modifier.height(24.dp))
        }
        BottomActions(
            state = ready,
            onEdit = onEditAlarm,
            onDelete = { confirmDelete = true },
            onRegisterCode = onRegisterCode,
            onStopRinging = { context.startActivity(Intent(context, RingingActivity::class.java)) },
        )
        Spacer(Modifier.height(16.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = CrystalColors.SurfaceHigh,
            title = { Text(stringResource(R.string.delete_title), style = CrystalType.Title) },
            text = { Text(stringResource(R.string.delete_text), color = CrystalColors.TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.onDelete()
                }) { Text(stringResource(R.string.action_delete), color = CrystalColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel), color = CrystalColors.Text)
                }
            },
        )
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
private fun Hero(state: HomeUiState.Ready) {
    val res = LocalResources.current
    val alarmState = state.dashboard.alarm
    val alarm = when (alarmState) {
        is AlarmState.Scheduled -> alarmState.alarm
        is AlarmState.Ringing -> alarmState.alarm
        AlarmState.None -> null
    }

    Text(
        text = stringResource(
            when (alarmState) {
                is AlarmState.Ringing -> R.string.home_ringing
                is AlarmState.Scheduled -> R.string.home_next_alarm
                AlarmState.None -> R.string.home_no_alarm
            },
        ),
        style = CrystalType.Label,
        color = if (alarmState is AlarmState.Ringing) CrystalColors.Danger else CrystalColors.TextMuted,
    )
    Spacer(Modifier.height(4.dp))
    AnimatedContent(
        targetState = alarm?.let { AlarmFormat.time(it.time) } ?: "--:--",
        transitionSpec = {
            (slideInVertically { it / 3 } + fadeIn()) togetherWith (slideOutVertically { -it / 3 } + fadeOut())
        },
        label = "heroTime",
    ) { time ->
        Text(
            text = time,
            style = CrystalType.Hero,
            color = if (alarm == null) CrystalColors.TextFaint else CrystalColors.Text,
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
        Spacer(Modifier.height(14.dp))
        Text(
            text = AlarmFormat.repeatSummary(res, alarmState.alarm.repeatDays),
            style = CrystalType.Label,
            color = CrystalColors.Accent,
        )
    } else if (alarmState is AlarmState.None) {
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
private fun StatusCards(state: HomeUiState.Ready, onRegisterCode: () -> Unit) {
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
        PermissionsCard()
    }
}

@Composable
private fun BottomActions(
    state: HomeUiState.Ready,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegisterCode: () -> Unit,
    onStopRinging: () -> Unit,
) {
    val res = LocalResources.current
    val lock = state.activeLock
    when {
        state.dashboard.alarm is AlarmState.Ringing -> FullWidthButton(
            text = stringResource(R.string.action_stop_alarm),
            icon = painterResource(R.drawable.ic_qr),
            onClick = onStopRinging,
        )
        !state.hasWakeCode -> FullWidthButton(
            text = stringResource(R.string.action_register_code_first),
            icon = painterResource(R.drawable.ic_qr),
            onClick = onRegisterCode,
        )
        state.dashboard.alarm is AlarmState.Scheduled -> Row(verticalAlignment = Alignment.CenterVertically) {
            CrystalButton(
                text = if (lock != null) {
                    stringResource(R.string.action_locked, AlarmFormat.shortDuration(res, lock.remaining(state.now)))
                } else {
                    stringResource(R.string.action_edit)
                },
                onClick = onEdit,
                enabled = lock == null,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            CircleIconButton(
                icon = rememberVectorPainter(Icons.Rounded.Delete),
                contentDescription = stringResource(R.string.action_delete),
                onClick = onDelete,
                size = 58.dp,
                tint = CrystalColors.Danger,
                container = CrystalColors.Danger.copy(alpha = 0.14f),
            )
        }
        else -> FullWidthButton(
            text = if (lock != null) {
                stringResource(R.string.action_locked, AlarmFormat.shortDuration(res, lock.remaining(state.now)))
            } else {
                stringResource(R.string.action_new_alarm)
            },
            onClick = onEdit,
            enabled = lock == null,
            style = ButtonStyle.Primary,
        )
    }
}

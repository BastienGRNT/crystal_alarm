package com.crystal.alarm.presentation.rescue

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.R
import com.crystal.alarm.presentation.components.ScreenHeader
import com.crystal.alarm.presentation.components.pressScale
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType

@Composable
fun RescueScreen(
    onBack: () -> Unit,
    viewModel: RescueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }

    LaunchedEffect(state.mistakes) {
        if (state.mistakes == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
        for (offset in listOf(-24f, 20f, -14f, 8f, 0f)) {
            shake.animateTo(offset, spring(stiffness = Spring.StiffnessHigh))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CrystalColors.Background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.rescue_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProgressSegments(solved = state.progress.solved, required = state.progress.required)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.rescue_rules, state.progress.required),
                style = CrystalType.Caption,
                color = CrystalColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = state.progress.problem,
                transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut() },
                label = "problem",
            ) { problem ->
                Text(
                    "${problem.left} × ${problem.right}",
                    style = CrystalType.Display,
                    color = CrystalColors.Text,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.input.ifEmpty { "?" },
                style = CrystalType.Display,
                color = if (state.input.isEmpty()) CrystalColors.TextFaint else CrystalColors.Accent,
                modifier = Modifier.graphicsLayer { translationX = shake.value },
            )
            Spacer(Modifier.height(12.dp))
            FeedbackText(state.feedback, state.completed)
            Spacer(Modifier.weight(1f))
        }
        Keypad(
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            onSubmit = viewModel::onSubmit,
            submitEnabled = state.input.isNotEmpty() && !state.completed,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun FeedbackText(feedback: RescueFeedback, completed: Boolean) {
    val (text, color) = when {
        completed -> stringResource(R.string.rescue_done) to CrystalColors.Success
        feedback is RescueFeedback.Wrong -> stringResource(R.string.rescue_wrong, feedback.expected) to CrystalColors.Danger
        feedback is RescueFeedback.Correct -> stringResource(R.string.rescue_correct) to CrystalColors.Success
        else -> " " to Color.Transparent
    }
    Text(text, style = CrystalType.Label, color = color, textAlign = TextAlign.Center)
}

@Composable
private fun ProgressSegments(solved: Int, required: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(required) { index ->
            val color by animateColorAsState(
                if (index < solved) CrystalColors.Accent else CrystalColors.SurfaceHigh,
                label = "segment",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit -> KeyText(digit.toString(), Modifier.weight(1f)) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KeyIcon(painterResource(R.drawable.ic_backspace), stringResource(R.string.action_erase), Modifier.weight(1f), onClick = onBackspace)
            KeyText("0", Modifier.weight(1f)) { onDigit(0) }
            KeyIcon(
                rememberVectorPainter(Icons.Rounded.Check),
                stringResource(R.string.action_validate),
                Modifier.weight(1f),
                container = if (submitEnabled) CrystalColors.Text else CrystalColors.Surface,
                tint = if (submitEnabled) CrystalColors.Background else CrystalColors.TextFaint,
                onClick = { if (submitEnabled) onSubmit() },
            )
        }
    }
}

@Composable
private fun Key(
    modifier: Modifier,
    container: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun KeyText(label: String, modifier: Modifier, onClick: () -> Unit) =
    Key(modifier, CrystalColors.Surface, onClick) {
        Text(label, style = CrystalType.Title, color = CrystalColors.Text)
    }

@Composable
private fun KeyIcon(
    icon: Painter,
    contentDescription: String,
    modifier: Modifier,
    container: Color = CrystalColors.Surface,
    tint: Color = CrystalColors.Text,
    onClick: () -> Unit,
) = Key(modifier, container, onClick) {
    Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(24.dp))
}

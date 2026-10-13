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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.crystal.alarm.domain.rescue.RescueStep
import com.crystal.alarm.domain.rescue.TextCheck
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

    Column(
        Modifier
            .fillMaxSize()
            .background(CrystalColors.Background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.rescue_title), onBack = onBack)
        StepSegments(state.progress.step, Modifier.padding(horizontal = 24.dp))
        when (state.progress.step) {
            RescueStep.CopyText -> CopyTextStep(state, viewModel::onTextChange)
            RescueStep.Calculation, RescueStep.Done -> CalculationStep(state, viewModel)
        }
    }
}

@Composable
private fun ColumnScope.CopyTextStep(state: RescueUiState, onTextChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val check = state.textCheck

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.rescue_step_text), style = CrystalType.Caption, color = CrystalColors.TextMuted)
        Spacer(Modifier.height(16.dp))
        // Plain Text: not selectable, so the target can't be copied and pasted.
        Text(
            text = highlightedTarget(state.progress.text, check),
            style = TargetStyle,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CrystalColors.Surface)
                .padding(20.dp),
        )
        Spacer(Modifier.height(16.dp))
        val shape = RoundedCornerShape(18.dp)
        BasicTextField(
            value = state.textInput,
            onValueChange = onTextChange,
            textStyle = CrystalType.Body.copy(color = CrystalColors.Text),
            cursorBrush = SolidColor(CrystalColors.Accent),
            minLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .clip(shape)
                        .background(CrystalColors.SurfaceHigh)
                        .border(1.dp, if (check.hasError) CrystalColors.Danger else CrystalColors.Accent, shape)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    if (state.textInput.isEmpty()) {
                        Text(stringResource(R.string.rescue_text_placeholder), style = CrystalType.Body, color = CrystalColors.TextFaint)
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (check.hasError) stringResource(R.string.rescue_text_error) else " ",
            style = CrystalType.Label,
            color = CrystalColors.Danger,
        )
        Spacer(Modifier.height(16.dp))
    }
}

private val TargetStyle = TextStyle(fontSize = 22.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)

/**
 * Already typed part in accent, the first wrong letter boxed and underlined (not only red:
 * the user is colorblind), the rest in plain text.
 */
private fun highlightedTarget(text: String, check: TextCheck): AnnotatedString {
    // The check counts characters of the normalized text; the generated text is already
    // lowercase, accent-free and single-spaced, so indices match.
    val matched = check.matched.coerceIn(0, text.length)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = CrystalColors.Accent)) { append(text.take(matched)) }
        if (check.hasError && matched < text.length) {
            withStyle(
                SpanStyle(
                    color = CrystalColors.Background,
                    background = CrystalColors.Danger,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold,
                ),
            ) { append(text[matched]) }
            withStyle(SpanStyle(color = CrystalColors.Text)) { append(text.substring(matched + 1)) }
        } else {
            withStyle(SpanStyle(color = CrystalColors.Text)) { append(text.substring(matched)) }
        }
    }
}

@Composable
private fun ColumnScope.CalculationStep(state: RescueUiState, viewModel: RescueViewModel) {
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
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.rescue_step_calc),
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
                "${problem.left} ${problem.operator.symbol} ${problem.right}",
                style = CrystalType.Display,
                color = CrystalColors.Text,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = state.calcInput.ifEmpty { "?" },
            style = CrystalType.Display,
            color = if (state.calcInput.isEmpty()) CrystalColors.TextFaint else CrystalColors.Accent,
            modifier = Modifier.graphicsLayer { translationX = shake.value },
        )
        Spacer(Modifier.height(12.dp))
        FeedbackText(state)
        Spacer(Modifier.weight(1f))
    }
    Keypad(
        onDigit = viewModel::onDigit,
        onBackspace = viewModel::onBackspace,
        onSubmit = viewModel::onSubmit,
        submitEnabled = state.calcInput.isNotEmpty() && !state.completed,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

@Composable
private fun FeedbackText(state: RescueUiState) {
    val expected = state.wrongExpected
    val (text, color) = when {
        state.completed -> stringResource(R.string.rescue_done) to CrystalColors.Success
        expected != null -> stringResource(R.string.rescue_wrong, expected) to CrystalColors.Danger
        else -> " " to Color.Transparent
    }
    Text(text, style = CrystalType.Label, color = color, textAlign = TextAlign.Center)
}

/** Two segments: text, then calculation. */
@Composable
private fun StepSegments(step: RescueStep, modifier: Modifier = Modifier) {
    val done = when (step) {
        RescueStep.CopyText -> 0
        RescueStep.Calculation -> 1
        RescueStep.Done -> 2
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) { index ->
            val color by animateColorAsState(
                if (index < done) CrystalColors.Accent else CrystalColors.SurfaceHigh,
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

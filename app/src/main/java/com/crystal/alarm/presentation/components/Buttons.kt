package com.crystal.alarm.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType

enum class ButtonStyle { Primary, Secondary, Danger }

/** Pill button with a springy press. Primary is white-on-black, the signature of the app. */
@Composable
fun CrystalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    icon: Painter? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val (container, content) = when (style) {
        ButtonStyle.Primary -> CrystalColors.Text to CrystalColors.Background
        ButtonStyle.Secondary -> CrystalColors.SurfaceHigh to CrystalColors.Text
        ButtonStyle.Danger -> CrystalColors.Danger.copy(alpha = 0.14f) to CrystalColors.Danger
    }
    val containerColor by animateColorAsState(if (enabled) container else CrystalColors.Surface, label = "container")
    val contentColor by animateColorAsState(if (enabled) content else CrystalColors.TextFaint, label = "content")

    Row(
        modifier = modifier
            .pressScale(interaction)
            .height(58.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = CrystalType.BodyStrong, color = contentColor, maxLines = 1)
    }
}

@Composable
fun CircleIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = CrystalColors.Text,
    container: Color = CrystalColors.SurfaceHigh,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (enabled) tint else CrystalColors.TextFaint, modifier = Modifier.size(size * 0.45f))
    }
}

@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = 800f), label = "press")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun FullWidthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    icon: Painter? = null,
) = CrystalButton(text, onClick, modifier.fillMaxWidth(), style, enabled, icon)

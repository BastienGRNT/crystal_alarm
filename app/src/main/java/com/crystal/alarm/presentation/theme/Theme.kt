package com.crystal.alarm.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CrystalColorScheme = darkColorScheme(
    primary = CrystalColors.Accent,
    onPrimary = Color.Black,
    secondary = CrystalColors.Ice,
    onSecondary = Color.Black,
    background = CrystalColors.Background,
    onBackground = CrystalColors.Text,
    surface = CrystalColors.Surface,
    onSurface = CrystalColors.Text,
    surfaceVariant = CrystalColors.SurfaceHigh,
    onSurfaceVariant = CrystalColors.TextMuted,
    surfaceContainerHigh = CrystalColors.SurfaceHigh,
    outline = CrystalColors.Stroke,
    error = CrystalColors.Danger,
)

/** Dark only: an alarm app lives at night. */
@Composable
fun CrystalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CrystalColorScheme,
        typography = CrystalTypography,
        content = content,
    )
}

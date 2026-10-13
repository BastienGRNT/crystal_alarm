package com.crystal.alarm.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object CrystalColors {
    val Background = Color(0xFF09090B)
    val Surface = Color(0xFF141417)
    val SurfaceHigh = Color(0xFF1D1D22)
    val Stroke = Color(0xFF2A2A31)

    val Text = Color(0xFFF4F4F6)
    val TextMuted = Color(0xFF8A8A94)
    val TextFaint = Color(0xFF45454D)

    val Accent = Color(0xFFA594FF)
    val AccentStrong = Color(0xFF7B61FF)
    val Ice = Color(0xFF7DE2FC)

    val Danger = Color(0xFFFF5D5D)
    val Success = Color(0xFF3DDC97)
    val Warning = Color(0xFFFFC857)

    val Crystal: Brush = Brush.linearGradient(listOf(Ice, Accent, AccentStrong))
}

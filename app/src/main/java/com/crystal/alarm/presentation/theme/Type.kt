package com.crystal.alarm.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Tabular figures so digits don't jiggle while counting down. */
private const val TABULAR = "tnum"

object CrystalType {
    val Hero = TextStyle(
        fontSize = 96.sp,
        lineHeight = 100.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-4).sp,
        fontFeatureSettings = TABULAR,
    )
    val Display = TextStyle(
        fontSize = 56.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-2).sp,
        fontFeatureSettings = TABULAR,
    )
    val Title = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
    val Body = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val BodyStrong = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val Label = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val Caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val Overline = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}

internal val CrystalTypography = Typography(
    displayLarge = CrystalType.Hero,
    displayMedium = CrystalType.Display,
    titleLarge = CrystalType.Title,
    bodyLarge = CrystalType.Body,
    labelLarge = CrystalType.Label,
    bodySmall = CrystalType.Caption,
)

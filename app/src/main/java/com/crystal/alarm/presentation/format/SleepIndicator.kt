package com.crystal.alarm.presentation.format

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.time.Duration

/**
 * Colors the time left before the alarm like a sleep budget, around the user's sleep goal
 * (7 h 30 by default):
 * - more than goal + 2 h: nothing to flag (null, classic colors);
 * - goal to goal + 2 h: a good night, pastel mint;
 * - under the goal: always red, from light pastel red to deep red as the night gets shorter
 *   (deep red at 4 h). Only the intensity changes, never the hue: the user is colorblind
 *   and can't tell orange from red.
 */
object SleepIndicator {
    private val GOOD_MARGIN: Duration = Duration.ofHours(2)
    private val CRITICAL: Duration = Duration.ofHours(4)

    val Mint = Color(0xFF8FE3C0)
    private val LightRed = Color(0xFFFFB3B3)
    private val DeepRed = Color(0xFFFF2D2D)

    fun color(timeLeft: Duration, goal: Duration): Color? = when {
        timeLeft > goal + GOOD_MARGIN -> null
        timeLeft >= goal -> Mint
        else -> {
            val range = (goal - CRITICAL).seconds.toFloat().coerceAtLeast(1f)
            val shortfall = (goal - timeLeft).seconds.toFloat()
            lerp(LightRed, DeepRed, (shortfall / range).coerceIn(0f, 1f))
        }
    }
}

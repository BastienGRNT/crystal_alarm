package com.crystal.alarm.presentation.format

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.time.Duration

/**
 * Colors the time left before the alarm like a sleep budget:
 * - more than 9 h 30: nothing to flag (null, classic colors);
 * - 7 h 30 to 9 h 30: a good night, pastel mint;
 * - under 7 h 30: always red, from light pastel red to deep red as the night gets shorter
 *   (deep red at 4 h). Only the intensity changes, never the hue: the user is colorblind
 *   and can't tell orange from red.
 */
object SleepIndicator {
    private val GOOD_MIN: Duration = Duration.ofMinutes(7 * 60 + 30)
    private val GOOD_MAX: Duration = Duration.ofMinutes(9 * 60 + 30)
    private val CRITICAL: Duration = Duration.ofHours(4)

    val Mint = Color(0xFF8FE3C0)
    private val LightRed = Color(0xFFFFB3B3)
    private val DeepRed = Color(0xFFFF2D2D)

    fun color(timeLeft: Duration): Color? = when {
        timeLeft > GOOD_MAX -> null
        timeLeft >= GOOD_MIN -> Mint
        else -> {
            val range = (GOOD_MIN - CRITICAL).seconds.toFloat()
            val shortfall = (GOOD_MIN - timeLeft).seconds.toFloat()
            lerp(LightRed, DeepRed, (shortfall / range).coerceIn(0f, 1f))
        }
    }
}

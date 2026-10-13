package com.crystal.alarm.presentation.scanner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.crystal.alarm.presentation.theme.CrystalColors

enum class ScanFeedback { Idle, Success, Error }

/** Dimmed scrim with a rounded viewfinder, corner brackets and a sweeping scan line. */
@Composable
fun ScannerOverlay(feedback: ScanFeedback, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val sweep by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep",
    )
    val frameColor by animateColorAsState(
        when (feedback) {
            ScanFeedback.Idle -> CrystalColors.Text
            ScanFeedback.Success -> CrystalColors.Success
            ScanFeedback.Error -> CrystalColors.Danger
        },
        label = "frame",
    )

    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val frameWidth = size.width * 0.74f
        val frameHeight = frameWidth * 0.78f
        val topLeft = Offset((size.width - frameWidth) / 2, (size.height - frameHeight) / 2.4f)
        val frame = Rect(topLeft, Size(frameWidth, frameHeight))
        val radius = 28.dp.toPx()

        drawRect(Color.Black.copy(alpha = 0.62f))
        drawRoundRect(Color.Transparent, frame.topLeft, frame.size, CornerRadius(radius), blendMode = BlendMode.Clear)

        // Corner brackets
        val arm = 34.dp.toPx()
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            val path = Path().apply {
                moveTo(x, y + dy * arm)
                lineTo(x, y + dy * radius)
                quadraticTo(x, y, x + dx * radius, y)
                lineTo(x + dx * arm, y)
            }
            drawPath(path, frameColor, style = stroke)
        }
        corner(frame.left, frame.top, 1f, 1f)
        corner(frame.right, frame.top, -1f, 1f)
        corner(frame.left, frame.bottom, 1f, -1f)
        corner(frame.right, frame.bottom, -1f, -1f)

        if (feedback == ScanFeedback.Idle) {
            val y = frame.top + frame.height * sweep
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, CrystalColors.Accent, CrystalColors.Ice, Color.Transparent),
                    startX = frame.left,
                    endX = frame.right,
                ),
                start = Offset(frame.left + 16.dp.toPx(), y),
                end = Offset(frame.right - 16.dp.toPx(), y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

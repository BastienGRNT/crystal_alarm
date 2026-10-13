package com.crystal.alarm.platform.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.crystal.alarm.R
import com.crystal.alarm.presentation.main.MainActivity

/**
 * Three layouts picked from the actual size:
 * - strip (1 row, e.g. 4x1): countdown on the left, alarm time and one detail on the right;
 * - compact (2x2): stacked, one detail line (two if tall enough);
 * - wide (4x2): alarm time in the header, every detail under the countdown.
 */
class NextAlarmWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(WidgetSnapshot.read(currentState<Preferences>()))
        }
    }
}

private val TextPrimary = Color(0xFFF4F4F6)
private val TextSecondary = Color(0xFFC4C4CC)
private val TextMuted = Color(0xFF8A8A94)
private val Accent = Color(0xFFA594FF)

/** Below this height the widget is a single row of cells. */
private val STRIP_MAX_HEIGHT = 100.dp

/** From this width there is room for the long texts (3 columns and more). */
private val WIDE_MIN_WIDTH = 220.dp

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    val size = LocalSize.current
    val base = GlanceModifier
        .fillMaxSize()
        .background(ImageProvider(R.drawable.widget_background))
        .cornerRadius(28.dp)
        .clickable(actionStartActivity<MainActivity>())
    when {
        size.height < STRIP_MAX_HEIGHT -> StripLayout(snapshot, size.width, base)
        size.width < WIDE_MIN_WIDTH -> CompactLayout(snapshot, size.width, size.height, base)
        else -> WideLayout(snapshot, size.width, base)
    }
}

@Composable
private fun StripLayout(snapshot: WidgetSnapshot, width: Dp, modifier: GlanceModifier) {
    Row(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(snapshot)
        Spacer(GlanceModifier.width(10.dp))
        Hero(snapshot, if (width < WIDE_MIN_WIDTH || !snapshot.highlighted) 22.sp else 28.sp)
        if (width >= WIDE_MIN_WIDTH) {
            Spacer(GlanceModifier.width(12.dp))
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End,
            ) {
                Subtitle(snapshot, 13.sp, TextAlign.End)
                // Only one detail fits: the lock is short-lived, so it wins.
                when {
                    snapshot.lockShort != null -> Detail(R.drawable.ic_widget_lock, snapshot.lockShort)
                    snapshot.bedtimeShort != null -> Detail(R.drawable.ic_widget_bed, snapshot.bedtimeShort)
                }
            }
        }
    }
}

@Composable
private fun CompactLayout(snapshot: WidgetSnapshot, width: Dp, height: Dp, modifier: GlanceModifier) {
    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(snapshot)
        Spacer(GlanceModifier.height(2.dp))
        Hero(snapshot, if (width < 140.dp) 26.sp else 32.sp)
        Subtitle(snapshot, 12.sp)
        val showBoth = height >= 150.dp
        if (snapshot.lockShort != null) Detail(R.drawable.ic_widget_lock, snapshot.lockShort)
        if (snapshot.bedtimeShort != null && (showBoth || snapshot.lockShort == null)) {
            Detail(R.drawable.ic_widget_bed, snapshot.bedtimeShort)
        }
    }
}

@Composable
private fun WideLayout(snapshot: WidgetSnapshot, width: Dp, modifier: GlanceModifier) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // With a countdown, the alarm time moves to the header to leave the bottom row to the details.
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Label(snapshot, GlanceModifier.defaultWeight())
            if (snapshot.highlighted) Subtitle(snapshot, 14.sp, TextAlign.End)
        }
        Spacer(GlanceModifier.height(2.dp))
        Hero(snapshot, if (width < 280.dp) 38.sp else 44.sp)
        if (!snapshot.highlighted) Subtitle(snapshot, 13.sp)
        if (snapshot.bedtime != null || snapshot.lock != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (snapshot.bedtime != null) Detail(R.drawable.ic_widget_bed, snapshot.bedtime)
                if (snapshot.bedtime != null && snapshot.lock != null) Spacer(GlanceModifier.width(14.dp))
                if (snapshot.lock != null) Detail(R.drawable.ic_widget_lock, snapshot.lock)
            }
        }
    }
}

private val WidgetSnapshot.indicatorColor: Color?
    get() = indicator?.let { Color(it) }

@Composable
private fun Dot(snapshot: WidgetSnapshot) {
    Spacer(
        GlanceModifier
            .size(8.dp)
            .cornerRadius(4.dp)
            .background(snapshot.indicatorColor ?: if (snapshot.highlighted) Accent else TextMuted),
    )
}

@Composable
private fun Label(snapshot: WidgetSnapshot, modifier: GlanceModifier = GlanceModifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Dot(snapshot)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = snapshot.label,
            style = TextStyle(color = ColorProvider(TextMuted), fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
private fun Hero(snapshot: WidgetSnapshot, fontSize: TextUnit) {
    Text(
        text = snapshot.time,
        style = TextStyle(
            color = ColorProvider(snapshot.indicatorColor ?: if (snapshot.highlighted) TextPrimary else TextMuted),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
    )
}

/** Alarm time and day: secondary, but readable at a glance. */
@Composable
private fun Subtitle(snapshot: WidgetSnapshot, fontSize: TextUnit, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = snapshot.subtitle,
        style = TextStyle(
            // With a sleep color on the countdown, keep the subtitle neutral so colors don't clash.
            color = ColorProvider(
                when {
                    !snapshot.highlighted -> TextMuted
                    snapshot.indicator == null -> Accent
                    else -> TextSecondary
                },
            ),
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            textAlign = textAlign,
        ),
        maxLines = 1,
    )
}

@Composable
private fun Detail(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(12.dp),
            colorFilter = ColorFilter.tint(ColorProvider(TextMuted)),
        )
        Spacer(GlanceModifier.width(5.dp))
        Text(
            text = text,
            style = TextStyle(color = ColorProvider(TextMuted), fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

package com.crystal.alarm.platform.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.crystal.alarm.R
import com.crystal.alarm.presentation.main.MainActivity

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
private val TextMuted = Color(0xFF8A8A94)
private val Accent = Color(0xFFA594FF)

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    val indicator = snapshot.indicator?.let { Color(it) }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .cornerRadius(28.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(indicator ?: if (snapshot.highlighted) Accent else TextMuted),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = snapshot.label,
                style = TextStyle(color = ColorProvider(TextMuted), fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = snapshot.time,
            style = TextStyle(
                color = ColorProvider(indicator ?: if (snapshot.highlighted) TextPrimary else TextMuted),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(
            text = snapshot.subtitle,
            style = TextStyle(
                // With a sleep color on the countdown, keep the subtitle quiet so colors don't clash.
                color = ColorProvider(if (snapshot.highlighted && indicator == null) Accent else TextMuted),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

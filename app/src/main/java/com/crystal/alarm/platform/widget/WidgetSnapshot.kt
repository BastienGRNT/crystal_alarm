package com.crystal.alarm.platform.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.crystal.alarm.R
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.usecase.Dashboard
import com.crystal.alarm.presentation.format.AlarmFormat
import com.crystal.alarm.presentation.format.SleepIndicator
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Pre-formatted texts stored in the Glance widget state. */
data class WidgetSnapshot(
    val label: String,
    val time: String,
    val subtitle: String,
    val highlighted: Boolean,
    val needsTicking: Boolean,
    /** Sleep budget color (ARGB) for the dot and countdown; null keeps the classic look. */
    val indicator: Int? = null,
) {
    fun writeTo(prefs: MutablePreferences) {
        prefs[KEY_LABEL] = label
        prefs[KEY_TIME] = time
        prefs[KEY_SUBTITLE] = subtitle
        prefs[KEY_HIGHLIGHTED] = highlighted
        if (indicator != null) prefs[KEY_INDICATOR] = indicator else prefs.remove(KEY_INDICATOR)
    }

    companion object {
        private val KEY_LABEL = stringPreferencesKey("label")
        private val KEY_TIME = stringPreferencesKey("time")
        private val KEY_SUBTITLE = stringPreferencesKey("subtitle")
        private val KEY_HIGHLIGHTED = booleanPreferencesKey("highlighted")
        private val KEY_INDICATOR = intPreferencesKey("indicator")
        private const val NO_TIME = "--"

        fun read(prefs: Preferences) = WidgetSnapshot(
            label = prefs[KEY_LABEL].orEmpty(),
            time = prefs[KEY_TIME] ?: NO_TIME,
            subtitle = prefs[KEY_SUBTITLE].orEmpty(),
            highlighted = prefs[KEY_HIGHLIGHTED] ?: false,
            needsTicking = false,
            indicator = prefs[KEY_INDICATOR],
        )

        fun from(context: Context, dashboard: Dashboard, now: Instant, zone: ZoneId): WidgetSnapshot {
            val res = context.resources
            val lock = dashboard.lock?.takeIf { it.isActive(now) }
            return when (val state = dashboard.alarm) {
                is AlarmState.Ringing -> WidgetSnapshot(
                    label = res.getString(R.string.widget_label_ringing),
                    time = res.getString(R.string.widget_ringing),
                    subtitle = AlarmFormat.time(state.alarm.time),
                    highlighted = true,
                    needsTicking = false,
                )
                // The countdown is the hero: "in how long do I get to sleep".
                is AlarmState.Scheduled -> {
                    val trigger = state.alarm.triggerAt
                    val timeLeft = Duration.between(now, trigger)
                    WidgetSnapshot(
                        label = res.getString(R.string.widget_label_next),
                        time = AlarmFormat.compactCountdown(res, timeLeft),
                        subtitle = res.getString(
                            R.string.widget_scheduled,
                            AlarmFormat.time(state.alarm.time),
                            AlarmFormat.relativeDay(res, trigger, now, zone),
                        ),
                        highlighted = true,
                        needsTicking = true,
                        indicator = SleepIndicator.color(timeLeft)?.toArgb(),
                    )
                }
                is AlarmState.None -> if (lock != null) {
                    WidgetSnapshot(
                        label = res.getString(R.string.widget_label_locked),
                        time = AlarmFormat.shortDuration(res, lock.remaining(now)),
                        subtitle = res.getString(R.string.widget_locked),
                        highlighted = false,
                        needsTicking = true,
                    )
                } else {
                    WidgetSnapshot(
                        label = res.getString(R.string.widget_label_none),
                        time = res.getString(R.string.widget_none),
                        subtitle = res.getString(R.string.widget_none_hint),
                        highlighted = false,
                        needsTicking = false,
                    )
                }
            }
        }
    }
}

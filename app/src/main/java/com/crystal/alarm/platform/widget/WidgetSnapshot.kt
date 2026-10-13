package com.crystal.alarm.platform.widget

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
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
    /** Alarm time and day ("07:00 · Demain"), or a hint when there is no countdown. */
    val subtitle: String,
    val highlighted: Boolean,
    val needsTicking: Boolean,
    /** Sleep budget color (ARGB) for the dot and countdown; null keeps the classic look. */
    val indicator: Int? = null,
    /** "Au lit dans 1 h 30" / "Moins de 7 h 30 de sommeil", for wide layouts. */
    val bedtime: String? = null,
    /** "1 h 30" / "< 7 h 30", next to a moon icon in narrow layouts. */
    val bedtimeShort: String? = null,
    /** "Verrou 23 min": post-dismissal lock still running while the next alarm is scheduled. */
    val lock: String? = null,
    /** "23 min", next to a lock icon in narrow layouts. */
    val lockShort: String? = null,
) {
    fun writeTo(prefs: MutablePreferences) {
        prefs[KEY_LABEL] = label
        prefs[KEY_TIME] = time
        prefs[KEY_SUBTITLE] = subtitle
        prefs[KEY_HIGHLIGHTED] = highlighted
        prefs.putOrRemove(KEY_INDICATOR, indicator)
        prefs.putOrRemove(KEY_BEDTIME, bedtime)
        prefs.putOrRemove(KEY_BEDTIME_SHORT, bedtimeShort)
        prefs.putOrRemove(KEY_LOCK, lock)
        prefs.putOrRemove(KEY_LOCK_SHORT, lockShort)
    }

    companion object {
        private val KEY_LABEL = stringPreferencesKey("label")
        private val KEY_TIME = stringPreferencesKey("time")
        private val KEY_SUBTITLE = stringPreferencesKey("subtitle")
        private val KEY_HIGHLIGHTED = booleanPreferencesKey("highlighted")
        private val KEY_INDICATOR = intPreferencesKey("indicator")
        private val KEY_BEDTIME = stringPreferencesKey("bedtime")
        private val KEY_BEDTIME_SHORT = stringPreferencesKey("bedtime_short")
        private val KEY_LOCK = stringPreferencesKey("lock")
        private val KEY_LOCK_SHORT = stringPreferencesKey("lock_short")
        private const val NO_TIME = "--:--"

        /** Further than this, "go to bed in 40 h" is noise: the bedtime line is hidden. */
        private val BEDTIME_HORIZON: Duration = Duration.ofHours(24)

        fun read(prefs: Preferences) = WidgetSnapshot(
            label = prefs[KEY_LABEL].orEmpty(),
            time = prefs[KEY_TIME] ?: NO_TIME,
            subtitle = prefs[KEY_SUBTITLE].orEmpty(),
            highlighted = prefs[KEY_HIGHLIGHTED] ?: false,
            needsTicking = false,
            indicator = prefs[KEY_INDICATOR],
            bedtime = prefs[KEY_BEDTIME],
            bedtimeShort = prefs[KEY_BEDTIME_SHORT],
            lock = prefs[KEY_LOCK],
            lockShort = prefs[KEY_LOCK_SHORT],
        )

        fun from(context: Context, dashboard: Dashboard, now: Instant, zone: ZoneId): WidgetSnapshot {
            val res = context.resources
            val lock = dashboard.lock?.takeIf { it.isActive(now) }
            val lockLeft = lock?.let { AlarmFormat.shortDuration(res, it.remaining(now)) }
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
                    val goal = dashboard.settings.sleepGoal
                    val untilBedtime = Duration.between(now, state.alarm.bedtime(goal))
                    val goalText = AlarmFormat.shortDuration(res, goal)
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
                        indicator = SleepIndicator.color(timeLeft, goal)?.toArgb(),
                        bedtime = when {
                            untilBedtime > BEDTIME_HORIZON -> null
                            untilBedtime.isNegative -> res.getString(R.string.widget_bedtime_late, goalText)
                            else -> res.getString(R.string.widget_bedtime, AlarmFormat.shortDuration(res, untilBedtime))
                        },
                        bedtimeShort = when {
                            untilBedtime > BEDTIME_HORIZON -> null
                            untilBedtime.isNegative -> res.getString(R.string.widget_bedtime_late_short, goalText)
                            else -> AlarmFormat.shortDuration(res, untilBedtime)
                        },
                        lock = lockLeft?.let { res.getString(R.string.widget_lock, it) },
                        lockShort = lockLeft,
                    )
                }
                // Nothing active: "--:--", whether disabled or never set up. The time only shows when enabled.
                is AlarmState.None -> WidgetSnapshot(
                    label = res.getString(if (dashboard.isDisabled) R.string.widget_label_disabled else R.string.widget_label_none),
                    time = NO_TIME,
                    subtitle = res.getString(if (dashboard.isDisabled) R.string.widget_disabled_hint else R.string.widget_none_hint),
                    highlighted = false,
                    needsTicking = lock != null,
                    lock = lockLeft?.let { res.getString(R.string.widget_lock, it) },
                    lockShort = lockLeft,
                )
            }
        }

        private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
            if (value != null) this[key] = value else remove(key)
        }
    }
}

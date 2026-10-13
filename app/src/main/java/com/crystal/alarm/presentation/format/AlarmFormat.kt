package com.crystal.alarm.presentation.format

import android.content.res.Resources
import com.crystal.alarm.R
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Display formatting shared by the app screens and the home screen widget. */
object AlarmFormat {
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val WEEK: List<DayOfWeek> = DayOfWeek.entries
    private val WEEKDAYS = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    )
    private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun time(time: LocalTime): String = time.format(TIME)

    fun time(instant: Instant, zone: ZoneId): String = instant.atZone(zone).format(TIME)

    /** "dans 7 h 32 min", "dans 12 min", "dans moins d'une minute". Rounded up to the minute. */
    fun countdown(res: Resources, duration: Duration): String {
        val totalMinutes = ceilMinutes(duration)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            totalMinutes < 1 -> res.getString(R.string.countdown_soon)
            hours == 0L -> res.getString(R.string.countdown_minutes, minutes)
            minutes == 0L -> res.getString(R.string.countdown_hours, hours)
            else -> res.getString(R.string.countdown_hours_minutes, hours, minutes)
        }
    }

    /** Widget hero: "7 h 32", "45 min", "< 1 min". Rounded up to the minute. */
    fun compactCountdown(res: Resources, duration: Duration): String {
        val totalMinutes = ceilMinutes(duration)
        return when {
            totalMinutes < 1 -> res.getString(R.string.countdown_compact_soon)
            totalMinutes < 60 -> res.getString(R.string.duration_minutes, totalMinutes)
            else -> res.getString(R.string.duration_hours_minutes, totalMinutes / 60, totalMinutes % 60)
        }
    }

    /** "23 min" or "1 h 05". Rounded up to the minute. */
    fun shortDuration(res: Resources, duration: Duration): String {
        val totalMinutes = ceilMinutes(duration).coerceAtLeast(1)
        return if (totalMinutes < 60) {
            res.getString(R.string.duration_minutes, totalMinutes)
        } else {
            res.getString(R.string.duration_hours_minutes, totalMinutes / 60, totalMinutes % 60)
        }
    }

    /** "Aujourd'hui", "Demain" or the weekday name. */
    fun relativeDay(res: Resources, trigger: Instant, now: Instant, zone: ZoneId): String {
        val days = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), trigger.atZone(zone).toLocalDate())
        return when (days) {
            0L -> res.getString(R.string.day_today)
            1L -> res.getString(R.string.day_tomorrow)
            else -> dayName(trigger.atZone(zone).dayOfWeek, TextStyle.FULL)
        }
    }

    fun repeatSummary(res: Resources, days: Set<DayOfWeek>): String = when {
        days.isEmpty() -> res.getString(R.string.repeat_once)
        days.size == 7 -> res.getString(R.string.repeat_everyday)
        days == WEEKDAYS -> res.getString(R.string.repeat_weekdays)
        days == WEEKEND -> res.getString(R.string.repeat_weekend)
        else -> WEEK.filter { it in days }.joinToString(" · ") { dayName(it, TextStyle.SHORT).removeSuffix(".") }
    }

    /** Single-letter label for the day picker (L M M J V S D in French). */
    fun dayLetter(day: DayOfWeek): String = dayName(day, TextStyle.NARROW)

    fun dayName(day: DayOfWeek, style: TextStyle): String =
        day.getDisplayName(style, Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

    private fun ceilMinutes(duration: Duration): Long {
        val seconds = duration.seconds.coerceAtLeast(0)
        return (seconds + 59) / 60
    }
}

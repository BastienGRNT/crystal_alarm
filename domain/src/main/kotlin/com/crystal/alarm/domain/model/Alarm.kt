package com.crystal.alarm.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/**
 * The one and only alarm of the app.
 *
 * @property time wall-clock time at which the alarm rings.
 * @property repeatDays days on which the alarm repeats. Empty means a one-shot alarm.
 * @property triggerAt next absolute instant at which the alarm will ring.
 */
data class Alarm(
    val time: LocalTime,
    val repeatDays: Set<DayOfWeek>,
    val triggerAt: Instant,
) {
    val isRepeating: Boolean get() = repeatDays.isNotEmpty()
}

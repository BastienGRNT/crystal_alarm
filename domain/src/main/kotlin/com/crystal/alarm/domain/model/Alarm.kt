package com.crystal.alarm.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/**
 * The one and only alarm of the app. It rings once, then turns itself off: the user
 * re-enables it by hand for the next morning.
 *
 * @property time wall-clock time at which the alarm rings.
 * @property triggerAt absolute instant at which the alarm will ring.
 */
data class Alarm(
    val time: LocalTime,
    val triggerAt: Instant,
) {
    /** Last moment to fall asleep and still get [sleepGoal] before the alarm rings. */
    fun bedtime(sleepGoal: Duration): Instant = triggerAt.minus(sleepGoal)
}

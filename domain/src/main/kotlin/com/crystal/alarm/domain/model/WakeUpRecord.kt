package com.crystal.alarm.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalTime

enum class DismissMethod { Code, Rescue }

/**
 * One morning: how long the alarm rang before the user proved they were up.
 * A reboot during ringing doesn't reset [rangAt]: the time lost counts.
 */
data class WakeUpRecord(
    val alarmTime: LocalTime,
    val rangAt: Instant,
    val stoppedAt: Instant,
    val method: DismissMethod,
) {
    /** Never negative, even if the clock was moved back while ringing. */
    val duration: Duration get() = Duration.between(rangAt, stoppedAt).let { if (it.isNegative) Duration.ZERO else it }

    companion object {
        /** Older records are dropped: about three months of mornings. */
        const val MAX_RECORDS = 90
    }
}

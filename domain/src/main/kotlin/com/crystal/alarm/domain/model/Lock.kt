package com.crystal.alarm.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Post-dismissal lock: once the alarm is stopped, no alarm can be created or moved
 * until [until], so the user can't sneak in a "snooze".
 */
data class Lock(
    val since: Instant,
    val until: Instant,
) {
    fun isActive(now: Instant): Boolean = now.isBefore(until)

    fun remaining(now: Instant): Duration =
        if (isActive(now)) Duration.between(now, until) else Duration.ZERO

    /** Elapsed fraction in [0, 1]. */
    fun progress(now: Instant): Float {
        val total = Duration.between(since, until).toMillis().coerceAtLeast(1)
        val elapsed = Duration.between(since, now).toMillis()
        return (elapsed.toFloat() / total).coerceIn(0f, 1f)
    }
}

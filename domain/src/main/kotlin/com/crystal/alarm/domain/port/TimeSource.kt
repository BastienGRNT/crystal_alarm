package com.crystal.alarm.domain.port

import java.time.Instant
import java.time.ZoneId

/** Abstraction over the system clock so time-based rules are testable. */
interface TimeSource {
    fun now(): Instant

    fun zone(): ZoneId
}

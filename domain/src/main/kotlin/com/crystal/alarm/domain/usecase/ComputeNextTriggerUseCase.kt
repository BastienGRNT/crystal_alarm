package com.crystal.alarm.domain.usecase

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/** Next instant strictly after [now] at [time]: later today, or tomorrow if already passed. */
class ComputeNextTriggerUseCase @Inject constructor() {

    operator fun invoke(time: LocalTime, now: Instant, zone: ZoneId): Instant {
        val alarmTime = time.withSecond(0).withNano(0)
        val today = now.atZone(zone).toLocalDate()
        val candidate = ZonedDateTime.of(today, alarmTime, zone).toInstant()
        return if (candidate.isAfter(now)) candidate else ZonedDateTime.of(today.plusDays(1), alarmTime, zone).toInstant()
    }
}

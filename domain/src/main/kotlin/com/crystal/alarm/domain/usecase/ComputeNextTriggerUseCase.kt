package com.crystal.alarm.domain.usecase

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/** Next instant strictly after [now] matching [time] and, if any, one of [repeatDays]. */
class ComputeNextTriggerUseCase @Inject constructor() {

    operator fun invoke(
        time: LocalTime,
        repeatDays: Set<DayOfWeek>,
        now: Instant,
        zone: ZoneId,
    ): Instant {
        val alarmTime = time.withSecond(0).withNano(0)
        var date = now.atZone(zone).toLocalDate()
        // 8 days covers "same weekday next week" when today's slot already passed.
        repeat(8) {
            val candidate = ZonedDateTime.of(date, alarmTime, zone).toInstant()
            val dayMatches = repeatDays.isEmpty() || date.dayOfWeek in repeatDays
            if (dayMatches && candidate.isAfter(now)) return candidate
            date = date.plusDays(1)
        }
        error("No trigger found for $time on $repeatDays")
    }
}

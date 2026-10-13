package com.crystal.alarm.domain

import com.crystal.alarm.domain.usecase.ComputeNextTriggerUseCase
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ComputeNextTriggerUseCaseTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val compute = ComputeNextTriggerUseCase()

    // Tuesday 13 October 2026
    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 10, day, hour, minute, second, 0, zone).toInstant()

    @Test
    fun `one shot later today`() {
        assertEquals(at(13, 7, 30), compute(LocalTime.of(7, 30), at(13, 6, 0), zone))
    }

    @Test
    fun `one shot already passed goes to tomorrow`() {
        assertEquals(at(14, 7, 30), compute(LocalTime.of(7, 30), at(13, 22, 0), zone))
    }

    @Test
    fun `same minute counts as passed`() {
        assertEquals(at(14, 7, 30), compute(LocalTime.of(7, 30), at(13, 7, 30, 5), zone))
    }

    @Test
    fun `daylight saving change keeps the wall clock time`() {
        // Night of 24 to 25 October 2026: clocks go back one hour in Paris.
        assertEquals(at(25, 7, 0), compute(LocalTime.of(7, 0), at(24, 23, 0), zone))
    }

    @Test
    fun `night of the switch to winter time lasts one hour more`() {
        val bedtime = at(24, 23, 0)
        val trigger = compute(LocalTime.of(7, 0), bedtime, zone)
        assertEquals("07:00", trigger.atZone(zone).toLocalTime().toString())
        assertEquals(Duration.ofHours(9), Duration.between(bedtime, trigger))
    }

    @Test
    fun `night of the switch to summer time lasts one hour less`() {
        val bedtime = ZonedDateTime.of(2027, 3, 27, 23, 0, 0, 0, zone).toInstant()
        val trigger = compute(LocalTime.of(7, 0), bedtime, zone)
        assertEquals("07:00", trigger.atZone(zone).toLocalTime().toString())
        assertEquals(Duration.ofHours(7), Duration.between(bedtime, trigger))
    }

    @Test
    fun `alarm inside the skipped hour rings right after the jump`() {
        // 28 March 2027, 02:00 jumps to 03:00 in Paris: 02:30 doesn't exist.
        val trigger = compute(LocalTime.of(2, 30), ZonedDateTime.of(2027, 3, 27, 23, 0, 0, 0, zone).toInstant(), zone)
        assertEquals(ZonedDateTime.of(2027, 3, 28, 3, 30, 0, 0, zone).toInstant(), trigger)
    }
}

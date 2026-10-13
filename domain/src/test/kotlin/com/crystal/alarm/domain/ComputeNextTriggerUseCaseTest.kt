package com.crystal.alarm.domain

import com.crystal.alarm.domain.usecase.ComputeNextTriggerUseCase
import java.time.DayOfWeek
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
        assertEquals(at(13, 7, 30), compute(LocalTime.of(7, 30), emptySet(), at(13, 6, 0), zone))
    }

    @Test
    fun `one shot already passed goes to tomorrow`() {
        assertEquals(at(14, 7, 30), compute(LocalTime.of(7, 30), emptySet(), at(13, 22, 0), zone))
    }

    @Test
    fun `same minute counts as passed`() {
        assertEquals(at(14, 7, 30), compute(LocalTime.of(7, 30), emptySet(), at(13, 7, 30, 5), zone))
    }

    @Test
    fun `repeating skips non selected days`() {
        val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        assertEquals(at(17, 9, 0), compute(LocalTime.of(9, 0), weekend, at(13, 6, 0), zone))
    }

    @Test
    fun `repeating on today only and passed goes to next week`() {
        assertEquals(
            at(20, 7, 0),
            compute(LocalTime.of(7, 0), setOf(DayOfWeek.TUESDAY), at(13, 8, 0), zone),
        )
    }
}

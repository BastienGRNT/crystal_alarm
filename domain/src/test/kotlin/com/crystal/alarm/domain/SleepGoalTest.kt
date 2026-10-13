package com.crystal.alarm.domain

import com.crystal.alarm.domain.model.Alarm
import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.usecase.UpdateSleepGoalUseCase
import kotlinx.coroutines.test.runTest
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SleepGoalTest {
    private val settings = FakeSettingsRepository()
    private val updateSleepGoal = UpdateSleepGoalUseCase(settings)

    @Test
    fun `default goal is 7 h 30`() = runTest {
        assertEquals(Duration.ofMinutes(450), settings.current().sleepGoal)
    }

    @Test
    fun `goal snaps to the step and stays in range`() = runTest {
        updateSleepGoal(8 * 60 + 10)
        assertEquals(8 * 60 + 15, settings.current().sleepGoalMinutes)
        updateSleepGoal(60)
        assertEquals(Settings.MIN_SLEEP_GOAL_MINUTES, settings.current().sleepGoalMinutes)
        updateSleepGoal(14 * 60)
        assertEquals(Settings.MAX_SLEEP_GOAL_MINUTES, settings.current().sleepGoalMinutes)
    }

    @Test
    fun `bedtime is the alarm minus the goal`() {
        val zone = ZoneId.of("Europe/Paris")
        val trigger = ZonedDateTime.of(2026, 10, 15, 7, 0, 0, 0, zone).toInstant()
        val alarm = Alarm(LocalTime.of(7, 0), trigger)
        assertEquals(
            ZonedDateTime.of(2026, 10, 14, 23, 30, 0, 0, zone).toInstant(),
            alarm.bedtime(Duration.ofMinutes(450)),
        )
    }
}

package com.crystal.alarm.domain

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.rescue.RescueChallenge
import com.crystal.alarm.domain.usecase.AddWakeCodeResult
import com.crystal.alarm.domain.usecase.AddWakeCodeUseCase
import com.crystal.alarm.domain.usecase.CancelAlarmUseCase
import com.crystal.alarm.domain.usecase.ComputeNextTriggerUseCase
import com.crystal.alarm.domain.usecase.DismissResult
import com.crystal.alarm.domain.usecase.DismissWithCodeUseCase
import com.crystal.alarm.domain.usecase.DismissWithRescueUseCase
import com.crystal.alarm.domain.usecase.FinishRingingUseCase
import com.crystal.alarm.domain.usecase.RestoreAlarmUseCase
import com.crystal.alarm.domain.usecase.SetAlarmResult
import com.crystal.alarm.domain.usecase.SetAlarmUseCase
import com.crystal.alarm.domain.usecase.StartRingingUseCase
import com.crystal.alarm.domain.usecase.UpdateLockMinutesUseCase
import kotlinx.coroutines.test.runTest
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlarmLifecycleTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val time = FakeTimeSource(ZonedDateTime.of(2026, 10, 13, 22, 0, 0, 0, zone).toInstant(), zone)
    private val alarms = FakeAlarmRepository()
    private val locks = FakeLockRepository()
    private val settings = FakeSettingsRepository()
    private val scheduler = FakeScheduler()
    private val compute = ComputeNextTriggerUseCase()

    private val setAlarm = SetAlarmUseCase(alarms, locks, settings, scheduler, time, compute)
    private val cancel = CancelAlarmUseCase(alarms, scheduler)
    private val startRinging = StartRingingUseCase(alarms, scheduler, time)
    private val finish = FinishRingingUseCase(alarms, locks, settings, scheduler, time, compute)
    private val dismissWithCode = DismissWithCodeUseCase(alarms, settings, finish)
    private val dismissWithRescue = DismissWithRescueUseCase(finish)
    private val restore = RestoreAlarmUseCase(alarms, scheduler, time, compute)
    private val addCode = AddWakeCodeUseCase(alarms, settings)
    private val updateLock = UpdateLockMinutesUseCase(settings, locks, time)

    private suspend fun ringAt7() {
        setAlarm(LocalTime.of(7, 0), emptySet())
        time.instant = ZonedDateTime.of(2026, 10, 14, 7, 0, 0, 0, zone).toInstant()
        startRinging()
    }

    @Test
    fun `cannot set an alarm without a wake code`() = runTest {
        val noCode = FakeSettingsRepository(code = null)
        val useCase = SetAlarmUseCase(alarms, locks, noCode, scheduler, time, compute)
        assertEquals(SetAlarmResult.MissingWakeCode, useCase(LocalTime.of(7, 0), emptySet()))
        assertNull(scheduler.scheduled)
    }

    @Test
    fun `setting an alarm replaces the previous one`() = runTest {
        setAlarm(LocalTime.of(7, 0), emptySet())
        setAlarm(LocalTime.of(8, 15), emptySet())
        val state = assertIs<AlarmState.Scheduled>(alarms.current())
        assertEquals(LocalTime.of(8, 15), state.alarm.time)
        assertEquals(state.alarm, scheduler.scheduled)
    }

    @Test
    fun `wrong code keeps ringing, right code dismisses and locks`() = runTest {
        ringAt7()
        assertEquals(DismissResult.WrongCode, dismissWithCode("nope"))
        assertIs<AlarmState.Ringing>(alarms.current())

        assertEquals(DismissResult.Dismissed, dismissWithCode(" 3017620422003 "))
        assertEquals(AlarmState.None, alarms.current())
        val lock = locks.current()!!
        assertEquals(Duration.ofMinutes(30), Duration.between(lock.since, lock.until))
    }

    @Test
    fun `no new alarm during the lock, allowed after`() = runTest {
        ringAt7()
        dismissWithCode("3017620422003")

        time.advance(Duration.ofMinutes(10))
        assertIs<SetAlarmResult.Locked>(setAlarm(LocalTime.of(7, 20), emptySet()))

        time.advance(Duration.ofMinutes(21))
        assertIs<SetAlarmResult.Scheduled>(setAlarm(LocalTime.of(7, 45), emptySet()))
    }

    @Test
    fun `lock duration cannot change while locked`() = runTest {
        ringAt7()
        dismissWithCode("3017620422003")
        assertFalse(updateLock(5))
        time.advance(Duration.ofMinutes(31))
        assertTrue(updateLock(47))
        assertEquals(45, settings.current().lockMinutes)
    }

    @Test
    fun `ringing alarm cannot be cancelled nor its code swapped`() = runTest {
        ringAt7()
        assertFalse(cancel())
        assertEquals(AddWakeCodeResult.AlarmRinging, addCode("Café", "other", "QR"))
        assertIs<AlarmState.Ringing>(alarms.current())
    }

    @Test
    fun `repeating alarm moves to next occurrence after dismissal`() = runTest {
        setAlarm(LocalTime.of(7, 0), setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        time.instant = ZonedDateTime.of(2026, 10, 14, 7, 0, 0, 0, zone).toInstant()
        startRinging()
        dismissWithCode("3017620422003")

        val next = assertIs<AlarmState.Scheduled>(alarms.current()).alarm
        assertEquals(ZonedDateTime.of(2026, 10, 16, 7, 0, 0, 0, zone).toInstant(), next.triggerAt)
    }

    @Test
    fun `rescue dismisses only when complete`() = runTest {
        ringAt7()
        val challenge = RescueChallenge(Random(42))
        val incomplete = challenge.start()
        assertEquals(DismissResult.WrongCode, dismissWithRescue(incomplete))

        val complete = incomplete.copy(solved = incomplete.required)
        assertEquals(DismissResult.Dismissed, dismissWithRescue(complete))
        assertEquals(AlarmState.None, alarms.current())
    }

    @Test
    fun `reboot while ringing rings again`() = runTest {
        ringAt7()
        restore()
        assertEquals(1, scheduler.firedNow)
    }

    @Test
    fun `alarm missed while phone was off rings on boot`() = runTest {
        setAlarm(LocalTime.of(7, 0), emptySet())
        time.instant = ZonedDateTime.of(2026, 10, 14, 7, 40, 0, 0, zone).toInstant()
        restore()
        assertEquals(1, scheduler.firedNow)
    }

    @Test
    fun `early stale fire is rescheduled instead of ringing`() = runTest {
        setAlarm(LocalTime.of(7, 0), emptySet())
        assertNull(startRinging())
        assertIs<AlarmState.Scheduled>(alarms.current())
    }
}

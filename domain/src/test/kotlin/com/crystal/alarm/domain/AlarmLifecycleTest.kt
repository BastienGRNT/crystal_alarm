package com.crystal.alarm.domain

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.DismissMethod
import com.crystal.alarm.domain.rescue.RescueAnswer
import com.crystal.alarm.domain.rescue.RescueChallenge
import com.crystal.alarm.domain.rescue.RescueStep
import com.crystal.alarm.domain.usecase.AddWakeCodeResult
import com.crystal.alarm.domain.usecase.AddWakeCodeUseCase
import com.crystal.alarm.domain.usecase.CancelAlarmUseCase
import com.crystal.alarm.domain.usecase.ComputeNextTriggerUseCase
import com.crystal.alarm.domain.usecase.DismissResult
import com.crystal.alarm.domain.usecase.DismissWithCodeUseCase
import com.crystal.alarm.domain.usecase.DismissWithRescueUseCase
import com.crystal.alarm.domain.usecase.EnableAlarmUseCase
import com.crystal.alarm.domain.usecase.FinishRingingUseCase
import com.crystal.alarm.domain.usecase.ObserveWakeUpStatsUseCase
import com.crystal.alarm.domain.usecase.RestoreAlarmUseCase
import com.crystal.alarm.domain.usecase.SetAlarmResult
import com.crystal.alarm.domain.usecase.SetAlarmUseCase
import com.crystal.alarm.domain.usecase.StartRingingUseCase
import com.crystal.alarm.domain.usecase.UpdateLockMinutesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlarmLifecycleTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val time = FakeTimeSource(ZonedDateTime.of(2026, 10, 13, 22, 0, 0, 0, zone).toInstant(), zone)
    private val alarms = FakeAlarmRepository()
    private val locks = FakeLockRepository()
    private val settings = FakeSettingsRepository()
    private val history = FakeWakeUpHistoryRepository()
    private val scheduler = FakeScheduler()
    private val compute = ComputeNextTriggerUseCase()

    private val setAlarm = SetAlarmUseCase(alarms, locks, settings, scheduler, time, compute)
    private val cancel = CancelAlarmUseCase(alarms, scheduler)
    private val startRinging = StartRingingUseCase(alarms, scheduler, time)
    private val finish = FinishRingingUseCase(alarms, locks, settings, history, scheduler, time)
    private val dismissWithCode = DismissWithCodeUseCase(alarms, settings, finish)
    private val dismissWithRescue = DismissWithRescueUseCase(finish)
    private val restore = RestoreAlarmUseCase(alarms, scheduler, time, compute)
    private val addCode = AddWakeCodeUseCase(alarms, settings)
    private val updateLock = UpdateLockMinutesUseCase(settings, locks, time)
    private val enable = EnableAlarmUseCase(alarms, setAlarm)

    private suspend fun ringAt7() {
        setAlarm(LocalTime.of(7, 0))
        time.instant = ZonedDateTime.of(2026, 10, 14, 7, 0, 0, 0, zone).toInstant()
        startRinging()
    }

    @Test
    fun `cannot set an alarm without a wake code`() = runTest {
        val noCode = FakeSettingsRepository(code = null)
        val useCase = SetAlarmUseCase(alarms, locks, noCode, scheduler, time, compute)
        assertEquals(SetAlarmResult.MissingWakeCode, useCase(LocalTime.of(7, 0)))
        assertNull(scheduler.scheduled)
    }

    @Test
    fun `setting an alarm replaces the previous one`() = runTest {
        setAlarm(LocalTime.of(7, 0))
        setAlarm(LocalTime.of(8, 15))
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
        assertIs<SetAlarmResult.Locked>(setAlarm(LocalTime.of(7, 20)))

        time.advance(Duration.ofMinutes(21))
        assertIs<SetAlarmResult.Scheduled>(setAlarm(LocalTime.of(7, 45)))
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
    fun `alarm turns off after dismissal and keeps its time`() = runTest {
        ringAt7()
        dismissWithCode("3017620422003")
        assertEquals(AlarmState.None, alarms.current())
        assertNull(scheduler.scheduled)
        assertEquals(LocalTime.of(7, 0), alarms.lastTime.value)
    }

    @Test
    fun `rescue dismisses only when complete`() = runTest {
        ringAt7()
        val challenge = RescueChallenge(Random(42))
        val incomplete = challenge.start()
        assertEquals(DismissResult.WrongCode, dismissWithRescue(incomplete))

        // The copied text alone isn't enough: the calculation is still pending.
        val calc = challenge.submitText(incomplete, incomplete.text)
        assertEquals(DismissResult.WrongCode, dismissWithRescue(calc))

        val complete = assertIs<RescueAnswer.Completed>(challenge.answer(calc, calc.problem.answer)).progress
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
        setAlarm(LocalTime.of(7, 0))
        time.instant = ZonedDateTime.of(2026, 10, 14, 7, 40, 0, 0, zone).toInstant()
        restore()
        assertEquals(1, scheduler.firedNow)
    }

    @Test
    fun `early stale fire is rescheduled instead of ringing`() = runTest {
        setAlarm(LocalTime.of(7, 0))
        assertNull(startRinging())
        assertIs<AlarmState.Scheduled>(alarms.current())
    }

    @Test
    fun `dismissal records how long the alarm rang`() = runTest {
        ringAt7()
        time.advance(Duration.ofSeconds(134))
        assertEquals(DismissResult.WrongCode, dismissWithCode("nope"))
        assertTrue(history.records.value.isEmpty())

        dismissWithCode("3017620422003")
        val record = history.records.value.single()
        assertEquals(Duration.ofSeconds(134), record.duration)
        assertEquals(LocalTime.of(7, 0), record.alarmTime)
        assertEquals(DismissMethod.Code, record.method)
    }

    @Test
    fun `rescue dismissal is recorded as such`() = runTest {
        ringAt7()
        time.advance(Duration.ofMinutes(4))
        val progress = RescueChallenge(Random(1)).start()
        dismissWithRescue(progress.copy(step = RescueStep.Done))
        assertEquals(DismissMethod.Rescue, history.records.value.single().method)
    }

    @Test
    fun `stats average the last mornings and keep the best`() = runTest {
        val stats = ObserveWakeUpStatsUseCase(history)
        assertNull(stats().first().average)

        // 8 mornings, re-enabled each evening, stopped in 1 to 8 minutes; newest is 8 min.
        for (minutes in 1L..8L) {
            time.advance(Duration.ofHours(12))
            setAlarm(LocalTime.of(7, 0))
            time.instant = (alarms.current() as AlarmState.Scheduled).alarm.triggerAt
            startRinging()
            time.advance(Duration.ofMinutes(minutes))
            dismissWithCode("3017620422003")
        }

        val result = stats().first()
        assertEquals(8, result.records.size)
        assertEquals(Duration.ofMinutes(8), result.last?.duration)
        // Last 7: 2..8 min, mean 5 min.
        assertEquals(Duration.ofMinutes(5), result.average)
        assertEquals(Duration.ofMinutes(1), result.best)
        assertEquals(0, result.rescueCount)
    }

    @Test
    fun `disabling keeps the time and enabling brings it back`() = runTest {
        assertNull(enable())
        setAlarm(LocalTime.of(6, 45))
        assertTrue(cancel())
        assertEquals(AlarmState.None, alarms.current())
        assertNull(scheduler.scheduled)
        assertEquals(LocalTime.of(6, 45), alarms.lastTime.value)

        val result = assertIs<SetAlarmResult.Scheduled>(enable())
        assertEquals(LocalTime.of(6, 45), result.alarm.time)
        assertEquals(result.alarm, scheduler.scheduled)
    }

    @Test
    fun `the lock blocks re-enabling right after waking up`() = runTest {
        ringAt7()
        dismissWithCode("3017620422003")

        assertIs<SetAlarmResult.Locked>(enable())
        time.advance(Duration.ofMinutes(31))
        assertIs<SetAlarmResult.Scheduled>(enable())
    }

    @Test
    fun `during the lock no alarm at all can be set, even one ringing long after it ends`() = runTest {
        ringAt7()
        dismissWithCode("3017620422003")
        val lock = assertNotNull(locks.current())

        // 07:00 + 30 min lock: an alarm at 09:30 rings 2 h after the lock ends, still refused.
        time.advance(Duration.ofMinutes(29))
        assertIs<SetAlarmResult.Locked>(setAlarm(LocalTime.of(9, 30)))
        assertIs<SetAlarmResult.Locked>(setAlarm(LocalTime.of(22, 0)))
        assertIs<SetAlarmResult.Locked>(enable())
        assertEquals(AlarmState.None, alarms.current())
        assertNull(scheduler.scheduled)

        time.instant = lock.until
        assertIs<SetAlarmResult.Scheduled>(setAlarm(LocalTime.of(9, 30)))
    }

    @Test
    fun `the lock is already set when the alarm turns off`() = runTest {
        ringAt7()
        val locksSeenWhenOff = mutableListOf<Boolean>()
        // Unconfined: the watcher reacts to each state change immediately, like the home screen would.
        val watcher = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            alarms.state.collect { if (it == AlarmState.None) locksSeenWhenOff += locks.current()?.isActive(time.instant) == true }
        }
        dismissWithCode("3017620422003")
        watcher.cancel()
        assertEquals(listOf(true), locksSeenWhenOff)
    }

    @Test
    fun `dismissal completes even when the ringing screen closes mid-way`() = runTest {
        ringAt7()
        val rescue = RescueChallenge(Random(3))
        val progress = rescue.start().let { rescue.submitText(it, it.text) }
        val done = assertIs<RescueAnswer.Completed>(rescue.answer(progress, progress.problem.answer)).progress

        // Like RingingActivity: as soon as the alarm is off, the screen closes and its scope is cancelled.
        lateinit var dismissal: Job
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            alarms.state.first { it == AlarmState.None }
            dismissal.cancel()
        }
        dismissal = launch { dismissWithRescue(done) }
        dismissal.join()

        assertEquals(true, locks.current()?.isActive(time.instant))
        assertEquals(1, history.records.value.size)
        assertIs<SetAlarmResult.Locked>(enable())
    }
}

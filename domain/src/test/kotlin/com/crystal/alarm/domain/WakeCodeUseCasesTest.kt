package com.crystal.alarm.domain

import com.crystal.alarm.domain.model.Alarm
import com.crystal.alarm.domain.model.WakeCode
import com.crystal.alarm.domain.usecase.AddWakeCodeResult
import com.crystal.alarm.domain.usecase.AddWakeCodeUseCase
import com.crystal.alarm.domain.usecase.DeleteWakeCodeResult
import com.crystal.alarm.domain.usecase.DeleteWakeCodeUseCase
import com.crystal.alarm.domain.usecase.DismissResult
import com.crystal.alarm.domain.usecase.DismissWithCodeUseCase
import com.crystal.alarm.domain.usecase.FinishRingingUseCase
import com.crystal.alarm.domain.usecase.ComputeNextTriggerUseCase
import com.crystal.alarm.domain.usecase.RenameWakeCodeUseCase
import com.crystal.alarm.domain.usecase.SelectWakeCodeUseCase
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WakeCodeUseCasesTest {
    private val alarms = FakeAlarmRepository()
    private val settings = FakeSettingsRepository(code = null)
    private val add = AddWakeCodeUseCase(alarms, settings)
    private val select = SelectWakeCodeUseCase(alarms, settings)
    private val rename = RenameWakeCodeUseCase(settings)
    private val delete = DeleteWakeCodeUseCase(alarms, settings)

    private val alarm = Alarm(LocalTime.of(7, 0), emptySet(), Instant.parse("2026-10-14T05:00:00Z"))

    private suspend fun addCode(name: String, value: String): WakeCode =
        assertIs<AddWakeCodeResult.Added>(add(name, value, "QR")).code

    @Test
    fun `a new code is named and becomes active`() = runTest {
        val first = addCode("  Dentifrice ", "111")
        val second = addCode("Café", "222")
        val current = settings.current()
        assertEquals("Dentifrice", first.name)
        assertEquals(listOf(first, second), current.wakeCodes)
        assertEquals(second, current.activeWakeCode)
    }

    @Test
    fun `blank names and duplicate codes are refused`() = runTest {
        assertEquals(AddWakeCodeResult.InvalidName, add("   ", "111", "QR"))
        val first = addCode("Dentifrice", "111")
        assertEquals(AddWakeCodeResult.AlreadySaved(first), add("Encore", " 111 ", "QR"))
    }

    @Test
    fun `only the active code stops the alarm`() = runTest {
        val toothpaste = addCode("Dentifrice", "111")
        addCode("Café", "222")
        select(toothpaste.id)
        alarms.markRinging(alarm, alarm.triggerAt)

        val time = FakeTimeSource(alarm.triggerAt)
        val finish = FinishRingingUseCase(alarms, FakeLockRepository(), settings, FakeScheduler(), time, ComputeNextTriggerUseCase())
        val dismiss = DismissWithCodeUseCase(alarms, settings, finish)
        assertEquals(DismissResult.WrongCode, dismiss("222"))
        assertEquals(DismissResult.Dismissed, dismiss("111"))
    }

    @Test
    fun `codes are frozen while ringing`() = runTest {
        val toothpaste = addCode("Dentifrice", "111")
        val coffee = addCode("Café", "222")
        alarms.markRinging(alarm, alarm.triggerAt)
        assertFalse(select(toothpaste.id))
        assertEquals(DeleteWakeCodeResult.AlarmRinging, delete(coffee.id))
        assertEquals(coffee, settings.current().activeWakeCode)
    }

    @Test
    fun `deleting the active code hands over to the next one`() = runTest {
        val toothpaste = addCode("Dentifrice", "111")
        val coffee = addCode("Café", "222")
        assertEquals(DeleteWakeCodeResult.Deleted, delete(coffee.id))
        assertEquals(toothpaste, settings.current().activeWakeCode)
    }

    @Test
    fun `the last code cannot be deleted while an alarm is scheduled`() = runTest {
        val toothpaste = addCode("Dentifrice", "111")
        alarms.schedule(alarm)
        assertEquals(DeleteWakeCodeResult.LastCodeWithAlarm, delete(toothpaste.id))

        alarms.clear()
        assertEquals(DeleteWakeCodeResult.Deleted, delete(toothpaste.id))
        assertNull(settings.current().activeWakeCode)
    }

    @Test
    fun `rename keeps the code and its active state`() = runTest {
        val toothpaste = addCode("Dentifrice", "111")
        assertTrue(rename(toothpaste.id, "Salle de bain"))
        assertFalse(rename(toothpaste.id, " "))
        assertEquals("Salle de bain", settings.current().activeWakeCode?.name)
    }
}

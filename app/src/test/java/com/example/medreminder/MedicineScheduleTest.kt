package com.example.medreminder

import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.domain.model.Medicine
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class MedicineScheduleTest {
    private val start = LocalDate.of(2026, 11, 10)
    private val medicine = Medicine(1, "Тест", "1", TimeOfDay.MORNING, 8, 0,
        start.toEpochDay(), 7, 127, listOf(480, 720, 1080))
    private val zone = ZoneId.of("Europe/Astrakhan")
    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).atZone(zone)

    @Test fun courseIncludesStartAndLastDayOnly() {
        assertFalse(medicine.isScheduledOn(start.minusDays(1)))
        assertTrue(medicine.isScheduledOn(start))
        assertTrue(medicine.isScheduledOn(start.plusDays(6)))
        assertFalse(medicine.isScheduledOn(start.plusDays(7)))
    }

    @Test fun weekdaysDoNotExtendCalendarCourse() {
        val mondays = medicine.copy(weekdaysMask = 1)
        assertFalse(mondays.isScheduledOn(start))
        assertTrue(mondays.isScheduledOn(LocalDate.of(2026, 11, 16)))
        assertFalse(mondays.isScheduledOn(LocalDate.of(2026, 11, 23)))
    }

    @Test fun nextAlarmUsesNextTimeAndThenNextDay() {
        assertEquals(at(start, 12), medicine.nextDoseAfter(at(start, 8)))
        assertEquals(at(start.plusDays(1), 8), medicine.nextDoseAfter(at(start, 18)))
    }

    @Test fun noAlarmsAfterFinalDose() {
        assertNull(medicine.nextDoseAfter(at(start.plusDays(6), 18)))
    }

    @Test fun futureStartDoesNotScheduleBeforeCourse() {
        assertEquals(at(start, 8), medicine.nextDoseAfter(at(start.minusDays(30), 12)))
    }

    @Test fun skipsUnselectedDays() {
        val mondays = medicine.copy(weekdaysMask = 1)
        assertEquals(at(LocalDate.of(2026, 11, 16), 8), mondays.nextDoseAfter(at(start, 7)))
    }

    @Test fun legacyMedicinesRemainDailyWithoutAnEndDate() {
        val old = Medicine(1, "Старое", "1", TimeOfDay.MORNING, 8, 30)
        assertEquals(listOf(510), old.times)
        assertTrue(old.isScheduledOn(start.plusYears(1)))
    }

    @Test fun yearBoundaryAndLeapDay() {
        val leap = medicine.copy(startEpochDay = LocalDate.of(2028, 2, 28).toEpochDay(), courseDays = 2)
        assertTrue(leap.isScheduledOn(LocalDate.of(2028, 2, 29)))
        assertFalse(leap.isScheduledOn(LocalDate.of(2028, 3, 1)))
        val december = medicine.copy(startEpochDay = LocalDate.of(2026, 12, 31).toEpochDay(), courseDays = 2)
        assertTrue(december.isScheduledOn(LocalDate.of(2027, 1, 1)))
        assertFalse(december.isScheduledOn(LocalDate.of(2027, 1, 2)))
    }
}

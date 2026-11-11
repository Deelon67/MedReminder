package com.example.medreminder

import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DiaryStatisticsTest {
    private val today = LocalDate.of(2026, 11, 11)
    private val now = today.atTime(10, 0).atZone(ZoneId.of("Europe/Moscow"))
    private fun medicine(id: Int = 1) = Medicine(id, "Препарат", "1 табл.", TimeOfDay.MORNING, 8, 0,
        startEpochDay = today.toEpochDay(), times = listOf(480, 1200))

    @Test fun futureDosesDoNotReduceAdherenceAndEarlyMarksAreCounted() {
        val result = diaryStatistics(listOf(medicine()), listOf(
            DiaryMark(1, today.toEpochDay(), 480, taken = true, skipped = false)
        ), now)
        assertEquals(30, result.days.size)
        assertEquals(1, result.due)
        assertEquals(100, result.percent)
        assertEquals(DiaryStatus.UPCOMING, result.days.single { it.date == today }.entries.last().status)
    }

    @Test fun hiddenDatesCabinetAndCourseBoundariesAreRespected() {
        val schedule = medicine().copy(startEpochDay = today.minusDays(3).toEpochDay(),
            courseDays = 2, times = listOf(480, 480),
            hiddenDates = setOf(today.minusDays(2).toEpochDay()))
        val result = diaryStatistics(listOf(schedule, medicine(2).copy(cabinetOnly = true)), emptyList(), now)
        assertEquals(1, result.due)
        assertEquals(0, result.percent)
        assertEquals("Утро", result.frequentUnmarkedPeriod)
        assertTrue(result.days.single { it.date == today }.entries.isEmpty())
    }

    @Test fun unfinishedTodayPreservesCompletedDayStreakAndSkippedDosesBreakIt() {
        val schedule = medicine().copy(startEpochDay = today.minusDays(3).toEpochDay(), times = listOf(480))
        val marks = listOf(
            DiaryMark(1, today.minusDays(3).toEpochDay(), 480, taken = false, skipped = true),
            DiaryMark(1, today.minusDays(2).toEpochDay(), 480, taken = true, skipped = false),
            DiaryMark(1, today.minusDays(1).toEpochDay(), 480, taken = true, skipped = false)
        )
        val result = diaryStatistics(listOf(schedule), marks, now)
        assertEquals(2, result.streak)
        assertEquals(50, result.percent)
        assertEquals(DiaryStatus.SKIPPED, result.days.single { it.date == today.minusDays(3) }.entries.single().status)
    }

    @Test fun medicinesWithoutAnyTakenMarksRemainInDiary() {
        val schedules = listOf(medicine(1), medicine(2), medicine(3))
        val result = diaryStatistics(schedules, listOf(
            DiaryMark(1, today.toEpochDay(), 480, taken = true, skipped = false),
            DiaryMark(2, today.toEpochDay(), 480, taken = false, skipped = true)
        ), now)
        val morning = result.days.single { it.date == today }.entries.filter { it.minutes == 480 }
        assertEquals(setOf(1, 2, 3), morning.map { it.medicine.id }.toSet())
        assertEquals(DiaryStatus.SKIPPED, morning.single { it.medicine.id == 2 }.status)
        assertEquals(DiaryStatus.UNMARKED, morning.single { it.medicine.id == 3 }.status)
        val unmarkedOnly = diaryStatistics(listOf(medicine(3)), emptyList(), now)
        assertEquals(0, unmarkedOnly.percent)
        assertEquals(1, unmarkedOnly.due)
    }

    @Test fun daysStayInTheirCalendarColumnsAfterMidnight() {
        val marks = listOf(DiaryMark(1, today.toEpochDay(), 480, taken = true, skipped = false))
        val before = diaryStatistics(listOf(medicine()), marks, today.atTime(23, 59).atZone(now.zone))
        val after = diaryStatistics(listOf(medicine()), marks, today.plusDays(1).atStartOfDay(now.zone))
        assertEquals(today, after.days[10].date)
        assertEquals(before.days[10], after.days[10])
        assertEquals(today.plusDays(1), after.days[11].date)
        assertTrue(after.days[11].entries.all { it.status == DiaryStatus.UPCOMING })
    }

    @Test fun calendarMonthHandlesAllLengthsAndLeapYears() {
        listOf(
            LocalDate.of(2026, 2, 15) to 28,
            LocalDate.of(2028, 2, 15) to 29,
            LocalDate.of(2026, 11, 15) to 30,
            LocalDate.of(2026, 12, 15) to 31
        ).forEach { (date, length) ->
            val result = diaryStatistics(emptyList(), emptyList(), date.atStartOfDay(now.zone))
            assertEquals(length, result.days.size)
            assertEquals(date.withDayOfMonth(1), result.days.first().date)
            assertEquals(date.withDayOfMonth(length), result.days.last().date)
        }
    }

    @Test fun newMonthDoesNotIncludePreviousMonthMarks() {
        val date = LocalDate.of(2027, 1, 1)
        val schedule = medicine().copy(startEpochDay = date.minusDays(2).toEpochDay())
        val result = diaryStatistics(listOf(schedule), listOf(
            DiaryMark(1, date.minusDays(1).toEpochDay(), 480, taken = true, skipped = false)
        ), date.atStartOfDay(now.zone))
        assertEquals(date, result.days.first().date)
        assertEquals(0, result.due)
        assertEquals(0, result.taken)
        assertNull(result.percent)
    }

    @Test fun emptyDiaryHasNoInventedPercentage() {
        val result = diaryStatistics(emptyList(), emptyList(), now)
        assertNull(result.percent)
        assertNull(result.frequentUnmarkedPeriod)
        assertEquals(0, result.streak)
    }
}

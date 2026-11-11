package com.example.medreminder.domain.model

import java.time.LocalDate
import java.time.ZonedDateTime

data class DiaryMark(val medicineId: Int, val day: Long, val minutes: Int, val taken: Boolean, val skipped: Boolean)
enum class DiaryStatus { TAKEN, SKIPPED, UNMARKED, UPCOMING }
data class DiaryEntry(val medicine: Medicine, val date: LocalDate, val minutes: Int, val status: DiaryStatus)
data class DiaryDay(val date: LocalDate, val entries: List<DiaryEntry>) {
    val due get() = entries.count { it.status != DiaryStatus.UPCOMING }
    val taken get() = entries.count { it.status == DiaryStatus.TAKEN }
    val percent: Int? get() = if (due == 0) null else taken * 100 / due
}
data class DiaryStatistics(val days: List<DiaryDay>, val streak: Int, val frequentUnmarkedPeriod: String?) {
    val entries get() = days.flatMap { it.entries }
    val due get() = days.sumOf { it.due }
    val taken get() = days.sumOf { it.taken }
    val percent: Int? get() = if (due == 0) null else taken * 100 / due
}

fun diaryStatistics(medicines: List<Medicine>, marks: List<DiaryMark>, now: ZonedDateTime): DiaryStatistics {
    val records = marks.associateBy { Triple(it.medicineId, it.day, it.minutes) }
    val today = now.toLocalDate()
    val monthStart = today.withDayOfMonth(1)
    val days = (1..today.lengthOfMonth()).map { day ->
        val date = monthStart.withDayOfMonth(day)
        val entries = medicines.filter { it.isScheduledOn(date) }.flatMap { medicine ->
            medicine.times.distinct().filter { it in 0..1439 }.map { minutes ->
                val record = records[Triple(medicine.id, date.toEpochDay(), minutes)]
                val due = date.atTime(minutes / 60, minutes % 60).atZone(now.zone)
                val status = when {
                    record?.taken == true -> DiaryStatus.TAKEN
                    record?.skipped == true -> DiaryStatus.SKIPPED
                    due.isAfter(now) -> DiaryStatus.UPCOMING
                    else -> DiaryStatus.UNMARKED
                }
                DiaryEntry(medicine, date, minutes, status)
            }
        }.sortedWith(compareBy({ it.minutes }, { medicineNameKey(it.medicine.name) }))
        DiaryDay(date, entries)
    }
    var streak = 0
    for (day in days.filter { it.date <= today }.asReversed()) {
        if (day.entries.isEmpty()) continue
        // An unfinished current day must not break the streak of completed days.
        if (day.date == today && day.entries.any { it.status != DiaryStatus.TAKEN }) continue
        if (day.entries.all { it.status == DiaryStatus.TAKEN }) streak++ else break
    }
    val missing = days.flatMap { it.entries }.filter {
        it.status == DiaryStatus.UNMARKED || it.status == DiaryStatus.SKIPPED
    }.groupingBy {
        when (it.minutes / 60) {
            in 0..5 -> "Ночь"
            in 6..11 -> "Утро"
            in 12..17 -> "День"
            else -> "Вечер"
        }
    }.eachCount()
    val highest = missing.values.maxOrNull()
    val periods = missing.filterValues { it == highest }.keys
    return DiaryStatistics(days, streak, if (periods.size == 1) periods.first() else if (periods.isEmpty()) null else "Разное время")
}

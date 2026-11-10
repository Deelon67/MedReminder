package com.example.medreminder.domain.model

import com.example.medreminder.data.db.TimeOfDay
import java.time.LocalDate
import java.time.ZonedDateTime

data class Medicine(
    val id: Int,
    val name: String,
    val dosage: String,
    val timeOfDay: TimeOfDay,
    val hour: Int,
    val minute: Int,
    // Null preserves the unlimited schedule of pre-migration medicines.
    val startEpochDay: Long? = null,
    val courseDays: Int? = null,
    val weekdaysMask: Int = 127,
    val times: List<Int> = listOf(hour * 60 + minute),
    val comment: String = "",
    val hiddenDates: Set<Long> = emptySet(),
    val cabinetOnly: Boolean = false,
    val form: String = "",
    val quantity: String = "",
    val expiryEpochDay: Long? = null,
    val preparationId: Int? = null
) {
    fun isScheduledOn(date: LocalDate): Boolean {
        if (cabinetOnly || date.toEpochDay() in hiddenDates) return false
        val start = startEpochDay
        if (start != null) {
            if (date.toEpochDay() < start) return false
            if (courseDays != null && date.toEpochDay() - start >= courseDays) return false
        }
        return weekdaysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
    }

    fun nextDoseAfter(now: ZonedDateTime): ZonedDateTime? {
        val first = maxOf(now.toLocalDate().toEpochDay(), startEpochDay ?: Long.MIN_VALUE)
        // Allow an extra week for each explicitly hidden date.
        for (offset in 0L..(7L * (hiddenDates.size.toLong() + 1))) {
            val date = LocalDate.ofEpochDay(first + offset)
            if (!isScheduledOn(date)) continue
            for (minutes in times.distinct().sorted()) {
                if (minutes !in 0..1439) continue
                val candidate = date.atTime(minutes / 60, minutes % 60).atZone(now.zone)
                if (candidate.isAfter(now)) return candidate
            }
        }
        return null
    }
}

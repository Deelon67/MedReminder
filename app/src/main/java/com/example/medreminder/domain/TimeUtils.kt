package com.example.medreminder.domain

import com.example.medreminder.data.db.TimeOfDay

// Утилита для определения периода дня по часу
object TimeUtils {

    // Определяет период дня на основе часа (0-23)
    fun getTimeOfDayFromHour(hour: Int): TimeOfDay {
        return when (hour) {
            in 0..11 -> TimeOfDay.MORNING      // 0:00 - 11:59 → Утро
            in 12..17 -> TimeOfDay.AFTERNOON   // 12:00 - 17:59 → День
            in 18..23 -> TimeOfDay.EVENING     // 18:00 - 23:59 → Вечер
            else -> TimeOfDay.MORNING
        }
    }

    // Форматирует период для отображения
    fun formatTimeOfDay(timeOfDay: TimeOfDay): String {
        return when (timeOfDay) {
            TimeOfDay.MORNING -> "Утро"
            TimeOfDay.AFTERNOON -> "День"
            TimeOfDay.EVENING -> "Вечер"
        }
    }
}
package com.example.medreminder.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Перечисление для времени приема лекарства
enum class TimeOfDay {
    MORNING,    // Утро
    AFTERNOON,  // День
    EVENING     // Вечер
}

// Аннотация @Entity говорит Room, что это таблица в базе данных
@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) // ID генерируется автоматически
    val id: Int = 0,
    val name: String,           // Название лекарства
    val dosage: String,         // Дозировка (например, "1 таблетка")
    val timeOfDay: TimeOfDay,   // Время приема (утро/день/вечер)
    val hour: Int,              // Час для уведомления (0-23)
    val minute: Int,            // Минута для уведомления (0-59)
    val startEpochDay: Long? = null,
    val courseDays: Int? = null,
    @androidx.room.ColumnInfo(defaultValue = "127")
    val weekdaysMask: Int = 127,
    @androidx.room.ColumnInfo(defaultValue = "''")
    val times: String = "",
    @androidx.room.ColumnInfo(defaultValue = "''")
    val comment: String = ""
)
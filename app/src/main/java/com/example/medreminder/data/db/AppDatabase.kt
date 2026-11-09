package com.example.medreminder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// Конвертер для сохранения enum в базу данных
class Converters {
    @TypeConverter
    fun fromTimeOfDay(value: TimeOfDay): String {
        return value.name
    }

    @TypeConverter
    fun toTimeOfDay(value: String): TimeOfDay {
        return TimeOfDay.valueOf(value)
    }
}

// @Database говорит Room, что это база данных
@Database(entities = [MedicineEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Абстрактный метод для получения DAO
    abstract fun medicineDao(): MedicineDao
}
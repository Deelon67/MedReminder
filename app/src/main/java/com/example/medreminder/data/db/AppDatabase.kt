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
@Database(entities = [MedicineEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN startEpochDay INTEGER")
                db.execSQL("ALTER TABLE medicines ADD COLUMN courseDays INTEGER")
                db.execSQL("ALTER TABLE medicines ADD COLUMN weekdaysMask INTEGER NOT NULL DEFAULT 127")
                db.execSQL("ALTER TABLE medicines ADD COLUMN times TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
            }
        }
    }


    // Абстрактный метод для получения DAO
    abstract fun medicineDao(): MedicineDao
}
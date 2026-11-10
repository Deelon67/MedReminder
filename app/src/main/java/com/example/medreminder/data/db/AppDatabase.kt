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
@Database(entities = [MedicineEntity::class, DoseRecord::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dose_records ADD COLUMN skippedAt INTEGER")
                db.execSQL("ALTER TABLE dose_records ADD COLUMN autoReminderAt INTEGER")
                db.execSQL("ALTER TABLE dose_records ADD COLUMN repeatCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS dose_records (medicineId INTEGER NOT NULL, epochDay INTEGER NOT NULL, minutes INTEGER NOT NULL, takenAt INTEGER, snoozedUntil INTEGER, notifiedAt INTEGER, PRIMARY KEY(medicineId, epochDay, minutes), FOREIGN KEY(medicineId) REFERENCES medicines(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dose_records_medicineId ON dose_records(medicineId)")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN preparationId INTEGER")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN hiddenDates TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN cabinetOnly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN form TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN quantity TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN expiryEpochDay INTEGER")
            }
        }

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
    abstract fun doseRecordDao(): DoseRecordDao
}
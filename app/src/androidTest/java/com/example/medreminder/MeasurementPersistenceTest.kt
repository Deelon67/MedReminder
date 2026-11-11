package com.example.medreminder

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medreminder.data.db.AppDatabase
import com.example.medreminder.data.db.MedicineEntity
import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.data.repository.MeasurementRepositoryImpl
import com.example.medreminder.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeasurementPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun rollbackFromVersionNinePreservesRecordedData() = runBlocking {
        val name = "measurement-rollback-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val id = db.medicineDao().insertMedicine(MedicineEntity(
                name = "Препарат", dosage = "1 табл.", timeOfDay = TimeOfDay.MORNING, hour = 8, minute = 0)).toInt()
            val dose = com.example.medreminder.data.db.DoseRecord(id, 0, 480, takenAt = 1000)
            db.doseRecordDao().save(dose)
            val pressure = parseMeasurement(MeasurementType.PRESSURE, "120", "80", "72", 1234)!!
            MeasurementRepositoryImpl(db.measurementDao()).add(pressure)
            val before = MeasurementRepositoryImpl(db.measurementDao()).observeAll().first().single()
            db.close()
            context.openOrCreateDatabase(name, 0, null).use {
                it.execSQL("ALTER TABLE health_measurements ADD COLUMN assessmentContext TEXT NOT NULL DEFAULT ''")
                it.version = 9
            }
            db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_9_8).build()
            assertEquals(before, MeasurementRepositoryImpl(db.measurementDao()).observeAll().first().single())
            assertEquals("Препарат", db.medicineDao().getMedicinesSnapshot().single().name)
            assertEquals(dose, db.doseRecordDao().find(id, 0, 480))
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals(before, MeasurementRepositoryImpl(db.measurementDao()).observeAll().first().single())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migrationKeepsMedicinesAndMeasurementsSurviveReopening() = runBlocking {
        val name = "measurement-migration-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            db.medicineDao().insertMedicine(MedicineEntity(
                name = "Препарат", dosage = "1 табл.", timeOfDay = TimeOfDay.MORNING, hour = 8, minute = 0))
            db.close()
            // Reproduce a version 7 database which had no measurement table.
            context.openOrCreateDatabase(name, 0, null).use {
                it.execSQL("DROP TABLE health_measurements")
                it.version = 7
            }
            db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_7_8).build()
            assertEquals("Препарат", db.medicineDao().getMedicinesSnapshot().single().name)
            val repository = MeasurementRepositoryImpl(db.measurementDao())
            val pressure = parseMeasurement(MeasurementType.PRESSURE, "120", "80", "72", 1000)!!
            val weight = parseMeasurement(MeasurementType.WEIGHT, "65,5", measuredAt = 2000)!!
            repository.add(pressure)
            repository.add(weight)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val rows = MeasurementRepositoryImpl(db.measurementDao()).observeAll().first()
            assertEquals(listOf(MeasurementType.WEIGHT, MeasurementType.PRESSURE), rows.map { it.type })
            assertEquals(weight.copy(id = rows[0].id), rows[0])
            assertEquals(pressure.copy(id = rows[1].id), rows[1])
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}

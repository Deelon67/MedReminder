package com.example.medreminder

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medreminder.data.db.AppDatabase
import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.data.repository.MedicineRepositoryImpl
import com.example.medreminder.domain.model.Medicine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MedicinePersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migratesOldMedicineWithoutLosingData() = runBlocking {
        val name = "migration-schedule-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, 0, null).use { db ->
            db.execSQL("CREATE TABLE medicines (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, dosage TEXT NOT NULL, timeOfDay TEXT NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL)")
            db.execSQL("INSERT INTO medicines VALUES (7, 'Старое лекарство', '1 таблетка', 'MORNING', 8, 30)")
            db.version = 1
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8).build()
        try {
            val old = MedicineRepositoryImpl(db.medicineDao()).getAllMedicines().first().single()
            assertEquals(7, old.id)
            assertEquals("Старое лекарство", old.name)
            assertEquals("1 таблетка", old.dosage)
            assertEquals(listOf(510), old.times)
            assertNull(old.courseDays)
            assertTrue(old.isScheduledOn(LocalDate.of(2027, 1, 1)))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun cabinetDetailsSurviveReopening() = runBlocking {
        val name = "cabinet-details-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val preparation = Medicine(
                0, "Препарат", "500 мг", TimeOfDay.MORNING, 0, 0,
                times = emptyList(), weekdaysMask = 0, comment = "В шкафу",
                cabinetOnly = true, form = "Таблетки", quantity = "20 таблеток",
                expiryEpochDay = LocalDate.of(2028, 1, 1).toEpochDay()
            )
            val id = MedicineRepositoryImpl(db.medicineDao()).addMedicine(preparation)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val restored = MedicineRepositoryImpl(db.medicineDao()).getAllMedicines().first().single()
            assertEquals(preparation.copy(id = id.toInt()), restored)
            assertFalse(restored.isScheduledOn(LocalDate.now()))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun sameNameSharesCabinetDetailsAndPreservesSchedules() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = MedicineRepositoryImpl(db.medicineDao())
            val inventory = Medicine(0, "Парацетамол", "500 мг", TimeOfDay.MORNING, 0, 0,
                cabinetOnly = true, times = emptyList(), form = "Таблетки", quantity = "20 шт")
            repository.addMedicine(inventory)
            repository.addMedicine(Medicine(0, "  ПАРАЦЕТАМОЛ ", "1 таблетка", TimeOfDay.MORNING, 8, 0))
            var rows = repository.getAllMedicines().first()
            val dose = rows.single { !it.cabinetOnly }
            assertEquals(rows.single { it.cabinetOnly }.id, dose.preparationId)
            assertEquals(listOf(480), dose.times)
            assertEquals("20 шт", dose.quantity)
            assertEquals(1, com.example.medreminder.domain.model.cabinetMedicines(rows).size)
            repository.addMedicine(inventory.copy(name = "парацетамол", quantity = "10 шт"))
            rows = repository.getAllMedicines().first()
            assertEquals(2, rows.size)
            assertEquals("10 шт", rows.single { it.cabinetOnly }.quantity)
            assertEquals("1 таблетка", rows.single { !it.cabinetOnly }.dosage)
            repository.deleteMedicine(rows.first())
            assertTrue(repository.getAllMedicines().first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test fun twoSchedulesLinkToOnePreparationAndKeepPackageDosage() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = MedicineRepositoryImpl(db.medicineDao())
            val inventory = Medicine(0, "Препарат", "500 мг", TimeOfDay.MORNING, 0, 0,
                cabinetOnly = true, times = emptyList(), form = "Таблетки", quantity = "20 табл.",
                comment = "В шкафу")
            val preparationId = repository.addMedicine(inventory).toInt()
            repository.addMedicine(Medicine(0, "Препарат", "1 табл.", TimeOfDay.MORNING, 8, 0,
                preparationId = preparationId, comment = "После еды"))
            repository.addMedicine(Medicine(0, "Препарат", "2 табл.", TimeOfDay.EVENING, 18, 0,
                preparationId = preparationId))
            val rows = repository.getAllMedicines().first()
            assertEquals(3, rows.size)
            assertEquals(setOf(preparationId), rows.filter { !it.cabinetOnly }.map { it.preparationId }.toSet())
            val card = com.example.medreminder.domain.model.cabinetMedicines(rows).single()
            assertEquals("500 мг", card.dosage)
            assertEquals("В шкафу", card.comment)
            assertEquals(setOf("1 табл.", "2 табл."), rows.filter { !it.cabinetOnly }.map { it.dosage }.toSet())
            val expiry = LocalDate.of(2028, 12, 1).toEpochDay()
            val updatedId = repository.addMedicine(inventory.copy(
                id = preparationId, dosage = "250 мг", quantity = "15 табл.",
                expiryEpochDay = expiry, comment = "В верхнем шкафу"
            ))
            val updatedRows = repository.getAllMedicines().first()
            assertEquals(preparationId.toLong(), updatedId)
            assertEquals(3, updatedRows.size)
            val updatedCard = updatedRows.single { it.cabinetOnly }
            assertEquals("250 мг", updatedCard.dosage)
            assertEquals(expiry, updatedCard.expiryEpochDay)
            assertEquals("В верхнем шкафу", updatedCard.comment)
            assertEquals(rows.filter { !it.cabinetOnly }.map { it.times },
                updatedRows.filter { !it.cabinetOnly }.map { it.times })
            assertEquals(setOf("1 табл.", "2 табл."),
                updatedRows.filter { !it.cabinetOnly }.map { it.dosage }.toSet())
            assertEquals("После еды", updatedRows.first { !it.cabinetOnly }.comment)
        } finally {
            db.close()
        }
    }

    @Test fun numericDoseUpdatesAfterCompletingPreparation() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = MedicineRepositoryImpl(db.medicineDao())
            repository.addMedicine(Medicine(0, "Парацетамол", "3", TimeOfDay.MORNING, 8, 0))
            val before = repository.getAllMedicines().first()
            assertEquals("3", before.single { !it.cabinetOnly }.dosage)
            val inventory = before.single { it.cabinetOnly }
            repository.addMedicine(inventory.copy(form = "Таблетки", dosage = "35 мг", quantity = "38 табл."))
            val after = repository.getAllMedicines().first()
            assertEquals("3 табл.", after.single { !it.cabinetOnly }.dosage)
            assertEquals("35 мг", after.single { it.cabinetOnly }.dosage)
            assertEquals(listOf(480), after.single { !it.cabinetOnly }.times)
            repository.addMedicine(inventory.copy(form = "Капсулы", dosage = "35 мг", quantity = "38 капс."))
            assertEquals("3 капс.", repository.getAllMedicines().first().single { !it.cabinetOnly }.dosage)
        } finally {
            db.close()
        }
    }

    @Test fun changingPreparationFormUpdatesSavedScheduleUnit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = MedicineRepositoryImpl(db.medicineDao())
            val preparation = Medicine(0, "Препарат", "", TimeOfDay.MORNING, 0, 0,
                cabinetOnly = true, times = emptyList(), form = "Капли")
            val id = repository.addMedicine(preparation).toInt()
            repository.addMedicine(Medicine(0, preparation.name, "3 кап.", TimeOfDay.MORNING, 8, 0,
                preparationId = id, comment = "После еды"))
            repository.addMedicine(Medicine(0, preparation.name, "20 мг", TimeOfDay.EVENING, 18, 0,
                preparationId = id))
            repository.addMedicine(preparation.copy(id = id, form = "Таблетки"))
            val schedules = repository.getAllMedicines().first().filter { !it.cabinetOnly }
            assertEquals(setOf("3 табл.", "20 мг"), schedules.map { it.dosage }.toSet())
            assertEquals(listOf(480), schedules.single { it.dosage == "3 табл." }.times)
            assertEquals("После еды", schedules.single { it.dosage == "3 табл." }.comment)
            assertEquals(setOf("3 табл.", "20 мг"),
                db.medicineDao().getMedicinesSnapshot().filter { !it.cabinetOnly }.map { it.dosage }.toSet())
            repository.addMedicine(preparation.copy(id = id, form = "Капсулы"))
            assertEquals(setOf("3 капс.", "20 мг"),
                repository.getAllMedicines().first().filter { !it.cabinetOnly }.map { it.dosage }.toSet())
        } finally {
            db.close()
        }
    }

    @Test fun chosenUnitFillsOnlyMissingInventoryForm() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = MedicineRepositoryImpl(db.medicineDao())
            listOf("табл." to "Таблетки", "капс." to "Капсулы", "кап." to "Капли", "впрыск." to "Спрей")
                .forEachIndexed { index, (unit, form) ->
                    val name = "Тест формы $index"
                    repository.addMedicine(Medicine(0, name, "1 $unit", TimeOfDay.MORNING, 8, 0))
                    val inventory = repository.getAllMedicines().first().single { it.cabinetOnly && it.name == name }
                    assertEquals(form, inventory.form)
                    assertEquals(form, db.medicineDao().getMedicinesSnapshot().single { it.id == inventory.id }.form)
                    repository.addMedicine(Medicine(0, name, "1 капс.", TimeOfDay.MORNING, 12, 0,
                        preparationId = inventory.id))
                    assertEquals(form, repository.getAllMedicines().first().single { it.id == inventory.id }.form)
                }
            val blank = Medicine(0, "Пустая карточка", "", TimeOfDay.MORNING, 0, 0,
                cabinetOnly = true, times = emptyList(), comment = "В шкафу")
            val id = repository.addMedicine(blank).toInt()
            repository.addMedicine(Medicine(0, blank.name, "2 капс.", TimeOfDay.MORNING, 8, 0, preparationId = id))
            val filled = repository.getAllMedicines().first().single { it.id == id }
            assertEquals("Капсулы", filled.form)
            assertEquals("В шкафу", filled.comment)
            assertEquals("", filled.dosage)
        } finally {
            db.close()
        }
    }

    @Test fun courseSurvivesDatabaseReopening() = runBlocking {
        val name = "persist-schedule-test.db"
        context.deleteDatabase(name)
        val medicine = Medicine(0, "Тест курса", "2 таблетки", TimeOfDay.MORNING, 8, 0,
            LocalDate.of(2026, 11, 10).toEpochDay(), 14, 21, listOf(480, 720, 1080), "После еды")
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val id = MedicineRepositoryImpl(db.medicineDao()).addMedicine(medicine)
            val hiddenDate = LocalDate.of(2026, 11, 10).toEpochDay()
            MedicineRepositoryImpl(db.medicineDao()).hideMedicineOnDate(medicine.copy(id = id.toInt()), hiddenDate)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val rows = MedicineRepositoryImpl(db.medicineDao()).getAllMedicines().first()
            val restored = rows.single { !it.cabinetOnly }
            assertEquals(rows.single { it.cabinetOnly }.id, restored.preparationId)
            assertEquals(medicine.copy(id = id.toInt(), hiddenDates = setOf(hiddenDate),
                preparationId = restored.preparationId, form = "Таблетки"), restored)
            assertFalse(restored.isScheduledOn(LocalDate.ofEpochDay(hiddenDate)))
            MedicineRepositoryImpl(db.medicineDao()).deleteMedicine(restored)
            assertTrue(MedicineRepositoryImpl(db.medicineDao()).getAllMedicines().first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}

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
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
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

    @Test fun courseSurvivesDatabaseReopening() = runBlocking {
        val name = "persist-schedule-test.db"
        context.deleteDatabase(name)
        val medicine = Medicine(0, "Тест курса", "2 таблетки", TimeOfDay.MORNING, 8, 0,
            LocalDate.of(2026, 11, 10).toEpochDay(), 14, 21, listOf(480, 720, 1080), "После еды")
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val id = MedicineRepositoryImpl(db.medicineDao()).addMedicine(medicine)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val restored = MedicineRepositoryImpl(db.medicineDao()).getAllMedicines().first().single()
            assertEquals(medicine.copy(id = id.toInt()), restored)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}

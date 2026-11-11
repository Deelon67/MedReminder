package com.example.medreminder

import android.app.Notification
import android.app.NotificationManager
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medreminder.data.db.AppDatabase
import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.data.repository.MedicineRepositoryImpl
import com.example.medreminder.domain.model.Medicine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class StockConsumptionPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val day = LocalDate.now().minusDays(1).toEpochDay()
    private fun inventory(quantity: String) = Medicine(910000, "Препарат", "50 мг", TimeOfDay.MORNING, 0, 0,
        cabinetOnly = true, form = "Таблетки", quantity = quantity, times = emptyList())
    private fun schedule(preparationId: Int, dose: String, minutes: Int = 0) =
        Medicine(0, "Препарат", dose, TimeOfDay.MORNING, minutes / 60, minutes % 60,
            times = listOf(minutes), preparationId = preparationId)

    @Test fun nonTabletFormsConsumeStockAndSendLowStockWarnings() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        val manager = DoseReminderManager(context, repo, db.doseRecordDao())
        val cleanupIds = mutableListOf<Int>()
        try {
            listOf(
                Triple("Мазь", "7 г", "3 г"),
                Triple("Крем", "5 г", "500 мг"),
                Triple("Гель", "7 г", "3 г"),
                Triple("Порошок", "7 г", "3 г"),
                Triple("Раствор", "7 мл", "3 мл"),
                Triple("Сироп", "7 мл", "3 мл"),
                Triple("Капли", "7 кап.", "3 кап."),
                Triple("Спрей", "7 впрыск", "3 впрыск."),
                Triple("Капсулы", "7 капс.", "3 капс."),
                Triple("Свечи", "7 шт.", "3 шт.")
            ).forEachIndexed { index, (form, quantity, dosage) ->
                val preparationId = repo.addMedicine(inventory(quantity).copy(
                    id = 920000 + index * 2, name = "Препарат $form", form = form
                )).toInt()
                val id = repo.addMedicine(schedule(preparationId, dosage)).toInt()
                cleanupIds += preparationId
                cleanupIds += id
                assertTrue(form, manager.take(id, day, 0))
                val saved = repo.getAllMedicines().first().single { it.id == preparationId }
                val expected = if (form == "Крем") "4,5 г" else quantity.replaceFirst("7", "4")
                assertEquals(form, expected, saved.quantity)
                assertTrue(form, context.getSystemService(NotificationManager::class.java).activeNotifications
                    .any { it.tag == "stock/$preparationId" })
                assertTrue(manager.take(id, day, 0))
                assertEquals(expected, repo.getAllMedicines().first().single { it.id == preparationId }.quantity)
            }
        } finally {
            cleanupIds.forEach { manager.cancelMedicine(it) }
            db.close()
        }
    }

    @Test fun savingNotesDoesNotRestoreStaleStockButExplicitRefillDoes() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        try {
            val original = inventory("10 табл.")
            val preparationId = repo.addMedicine(original).toInt()
            val id = repo.addMedicine(schedule(preparationId, "3 табл.")).toInt()
            repo.recordTakenAndConsume(id, day, 0, System.currentTimeMillis())
            repo.addMedicine(original.copy(comment = "Новая заметка"), updateQuantity = false)
            val updated = repo.getAllMedicines().first().single { it.cabinetOnly }
            assertEquals("7 табл.", updated.quantity)
            assertEquals("Новая заметка", updated.comment)
            repo.addMedicine(updated.copy(quantity = "20 табл."))
            assertEquals("20 табл.", repo.getAllMedicines().first().single { it.cabinetOnly }.quantity)
        } finally {
            db.close()
        }
    }

    @Test fun takesConsumeExactlyOnceAndWarnBelowFive() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        val manager = DoseReminderManager(context, repo, db.doseRecordDao())
        val notifications = context.getSystemService(NotificationManager::class.java)
        val preparationId = repo.addMedicine(inventory("7 табл.")).toInt()
        val id = repo.addMedicine(schedule(preparationId, "3 табл.")).toInt()
        try {
            assertTrue(manager.take(id, day, 0))
            assertEquals("4 табл.", repo.getAllMedicines().first().single { it.cabinetOnly }.quantity)
            assertEquals("4 табл.", db.medicineDao().getMedicinesSnapshot().single { it.id == id }.quantity)
            val record = db.doseRecordDao().find(id, day, 0)
            assertNotNull(record?.takenAt)
            val warning = notifications.activeNotifications.single { it.tag == "stock/$preparationId" }
            assertEquals("Пора пополнить аптечку",
                warning.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertTrue(warning.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                .contains("4 таблетки"))
            assertTrue(manager.take(id, day, 0))
            manager.skip(id, day, 0)
            assertEquals(record, db.doseRecordDao().find(id, day, 0))
            assertEquals("4 табл.", repo.getAllMedicines().first().single { it.cabinetOnly }.quantity)
        } finally {
            manager.cancelMedicine(preparationId)
            manager.cancelMedicine(id)
            db.close()
        }
    }

    @Test fun independentManagersShareStockAndCannotDoubleConsume() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        val first = DoseReminderManager(context, repo, db.doseRecordDao())
        val second = DoseReminderManager(context, repo, db.doseRecordDao())
        val preparationId = repo.addMedicine(inventory("10 табл.")).toInt()
        val id = repo.addMedicine(schedule(preparationId, "3 табл.")).toInt()
        val otherId = repo.addMedicine(schedule(preparationId, "2 табл.", 60)).toInt()
        try {
            coroutineScope {
                awaitAll(
                    async(Dispatchers.Default) { first.take(id, day, 0) },
                    async(Dispatchers.Default) { second.take(id, day, 0) },
                    async(Dispatchers.Default) { second.take(otherId, day, 60) }
                )
            }
            assertEquals("5 табл.", repo.getAllMedicines().first().single { it.cabinetOnly }.quantity)
            assertEquals(2, db.doseRecordDao().observeAll().first().count { it.takenAt != null })
            assertFalse(context.getSystemService(NotificationManager::class.java).activeNotifications
                .any { it.tag == "stock/$preparationId" })
        } finally {
            first.cancelMedicine(preparationId)
            first.cancelMedicine(id)
            first.cancelMedicine(otherId)
            db.close()
        }
    }

    @Test fun skipAndSnoozeDoNotConsumeAndInsufficientStockStopsAtZero() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        val manager = DoseReminderManager(context, repo, db.doseRecordDao())
        val preparationId = repo.addMedicine(inventory("1 табл.")).toInt()
        val id = repo.addMedicine(schedule(preparationId, "3 табл.")).toInt()
        val otherId = repo.addMedicine(schedule(preparationId, "2 табл.", 60)).toInt()
        try {
            manager.skip(id, day, 0)
            assertFalse(manager.take(id, day, 0))
            manager.snooze(otherId, day, 60, 15)
            assertEquals("1 табл.", repo.getAllMedicines().first().single { it.cabinetOnly }.quantity)
            assertTrue(manager.take(otherId, day, 60))
            assertEquals("0 табл.", repo.getAllMedicines().first().single { it.cabinetOnly }.quantity)
            assertNull(db.doseRecordDao().find(otherId, day, 60)?.snoozedUntil)
        } finally {
            manager.cancelMedicine(preparationId)
            manager.cancelMedicine(id)
            manager.cancelMedicine(otherId)
            db.close()
        }
    }

    @Test fun unknownOrDifferentUnitsStillAllowRecordingWithoutStockConversion() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        try {
            val preparationId = repo.addMedicine(inventory("20 табл.")).toInt()
            val id = repo.addMedicine(schedule(preparationId, "50 мг")).toInt()
            assertTrue(repo.recordTakenAndConsume(id, day, 0, System.currentTimeMillis()).accepted)
            assertEquals("20 табл.", repo.getAllMedicines().first().single { it.cabinetOnly }.quantity)
            assertNotNull(db.doseRecordDao().find(id, day, 0)?.takenAt)
        } finally {
            db.close()
        }
    }
}

package com.example.medreminder

import android.app.NotificationManager
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medreminder.data.db.AppDatabase
import com.example.medreminder.data.db.DoseRecord
import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.data.repository.MedicineRepositoryImpl
import com.example.medreminder.domain.model.Medicine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class DoseReminderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun takeAndSnoozeAreScopedToOneDoseAndTakingIsIdempotent() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        val dao = db.doseRecordDao()
        val manager = DoseReminderManager(context, repo, dao)
        val day = LocalDate.now().toEpochDay()
        val minutes = (LocalTime.now().hour * 60 + LocalTime.now().minute - 1).coerceAtLeast(0)
        val otherTime = if (minutes == 1439) 0 else minutes + 1
        val id = 900001
        try {
            repo.addMedicine(Medicine(id, "Тест напоминания", "1 табл.", TimeOfDay.MORNING,
                minutes / 60, minutes % 60, times = listOf(minutes, otherTime)))
            manager.snooze(id, day, minutes, 15)
            val snoozed = dao.find(id, day, minutes)!!
            assertTrue(snoozed.snoozedUntil!! > System.currentTimeMillis())
            assertNull(dao.find(id, day, otherTime))
            assertNull(dao.find(id, day + 1, minutes))
            manager.take(id, day, minutes)
            val taken = dao.find(id, day, minutes)!!
            assertNotNull(taken.takenAt)
            assertNull(taken.snoozedUntil)
            manager.take(id, day, minutes)
            assertEquals(taken, dao.find(id, day, minutes))
            manager.snooze(id, day, minutes, 30)
            assertEquals(taken, dao.find(id, day, minutes))
            manager.skip(id, day, minutes)
            assertEquals(taken, dao.find(id, day, minutes))
        } finally {
            manager.cancelMedicine(id)
            repo.getAllMedicines().first().firstOrNull { it.id == id }?.let {
                MedicineAlarmScheduler(context).cancelMedicineAlarm(it)
            }
            db.close()
        }
    }

    @Test fun notificationHasThreeIndependentActionsAndDisappearsAfterTaking() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        val manager = DoseReminderManager(context, repo, db.doseRecordDao())
        val id = 900002
        val day = LocalDate.now().toEpochDay()
        try {
            repo.addMedicine(Medicine(id, "Тест уведомления", "2 капс.", TimeOfDay.MORNING, 0, 0))
            manager.show(id, day, 0)
            val notifications = context.getSystemService(NotificationManager::class.java)
            val notification = notifications.activeNotifications.single { it.tag == "dose/$id/$day/0" }.notification
            assertEquals("Тест уведомления — 2 капсулы",
                notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString())
            assertTrue(notification.extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)
                .toString().startsWith("Тест уведомления — 2 капсулы."))
            assertEquals(listOf("Принято", "15 мин.", "30 мин."),
                notification.actions.map { it.title.toString() })
            assertEquals(3, notification.actions.map { it.actionIntent }.distinct().size)
            manager.take(id, day, 0)
            kotlinx.coroutines.withTimeout(3_000) {
                while (notifications.activeNotifications.any { it.tag == "dose/$id/$day/0" }) {
                    kotlinx.coroutines.delay(25)
                }
            }
            manager.show(id, day, 0)
            kotlinx.coroutines.withTimeout(3_000) {
                while (notifications.activeNotifications.any { it.tag == "dose/$id/$day/0" }) {
                    kotlinx.coroutines.delay(25)
                }
            }
        } finally {
            manager.cancelMedicine(id)
            db.close()
        }
    }

    @Test fun automaticRepeatsStopAfterTwoAndExplicitSnoozeReplacesThem() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = MedicineRepositoryImpl(db.medicineDao())
        val preferences = ReminderPreferences(context, "repeat-test-preferences")
        preferences.update(true, 15)
        val manager = DoseReminderManager(context, repo, db.doseRecordDao(), preferences)
        val dao = db.doseRecordDao()
        val id = 900003
        val day = LocalDate.now().toEpochDay()
        try {
            repo.addMedicine(Medicine(id, "Тест повторов", "1 табл.", TimeOfDay.MORNING, 0, 0))
            manager.show(id, day, 0)
            var record = dao.find(id, day, 0)!!
            assertEquals(0, record.repeatCount)
            assertNotNull(record.autoReminderAt)
            dao.save(record.copy(autoReminderAt = System.currentTimeMillis() - 1000))
            manager.show(id, day, 0)
            record = dao.find(id, day, 0)!!
            assertEquals(1, record.repeatCount)
            assertNotNull(record.autoReminderAt)
            dao.save(record.copy(autoReminderAt = System.currentTimeMillis() - 1000))
            manager.show(id, day, 0)
            record = dao.find(id, day, 0)!!
            assertEquals(2, record.repeatCount)
            assertNull(record.autoReminderAt)
            manager.show(id, day, 0)
            assertEquals(record, dao.find(id, day, 0))

            // Start a fresh pending fixture to test explicit snoozing independently.
            dao.save(DoseRecord(id, day, 0))
            manager.show(id, day, 0)
            manager.snooze(id, day, 0, 15)
            record = dao.find(id, day, 0)!!
            assertNotNull(record.snoozedUntil)
            assertNull(record.autoReminderAt)
            dao.save(record.copy(snoozedUntil = System.currentTimeMillis() - 1000))
            manager.show(id, day, 0)
            record = dao.find(id, day, 0)!!
            assertNull(record.snoozedUntil)
            assertNull(record.autoReminderAt)
            manager.skip(id, day, 0)
            val skipped = dao.find(id, day, 0)!!
            assertNotNull(skipped.skippedAt)
            manager.show(id, day, 0)
            manager.snooze(id, day, 0, 15)
            manager.take(id, day, 0)
            manager.skip(id, day, 0)
            assertEquals(skipped, dao.find(id, day, 0))
            kotlinx.coroutines.withTimeout(3_000) {
                while (context.getSystemService(NotificationManager::class.java).activeNotifications
                        .any { it.tag == "dose/$id/$day/0" }) {
                    kotlinx.coroutines.delay(25)
                }
            }
        } finally {
            manager.cancelMedicine(id)
            repo.getAllMedicines().first().firstOrNull { it.id == id }?.let {
                MedicineAlarmScheduler(context).cancelMedicineAlarm(it)
            }
            db.close()
        }
    }

    @Test fun skippedStatusSurvivesReopening() = runBlocking {
        val name = "skipped-history-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val day = LocalDate.now().toEpochDay()
        try {
            val repo = MedicineRepositoryImpl(db.medicineDao())
            val id = repo.addMedicine(Medicine(0, "Пропуск", "1 табл.", TimeOfDay.MORNING, 0, 0)).toInt()
            val manager = DoseReminderManager(context, repo, db.doseRecordDao())
            manager.skip(id, day, 0)
            val skipped = db.doseRecordDao().find(id, day, 0)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals(skipped, db.doseRecordDao().find(id, day, 0))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun doseHistorySurvivesReopeningAndIsRemovedWithItsMedicine() = runBlocking {
        val name = "dose-history-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val day = LocalDate.now().toEpochDay()
        try {
            var repo = MedicineRepositoryImpl(db.medicineDao())
            val id = repo.addMedicine(Medicine(0, "История", "1 табл.", TimeOfDay.MORNING, 8, 0)).toInt()
            db.doseRecordDao().save(DoseRecord(id, day, 480, takenAt = 123L))
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals(123L, db.doseRecordDao().find(id, day, 480)?.takenAt)
            repo = MedicineRepositoryImpl(db.medicineDao())
            repo.deleteMedicine(repo.getAllMedicines().first().first { it.id == id })
            assertTrue(db.doseRecordDao().observeAll().first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}

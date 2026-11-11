package com.example.medreminder

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.medreminder.data.db.DoseRecord
import com.example.medreminder.data.db.DoseRecordDao
import com.example.medreminder.domain.repository.MedicineRepository
import com.example.medreminder.presentation.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

class DoseReminderManager(
    private val context: Context,
    private val repository: MedicineRepository,
    private val records: DoseRecordDao,
    private val preferences: ReminderPreferences = ReminderPreferences(context)
) {
    private val mutex = Mutex()
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL = "medicine_reminder_channel"
        const val DAY = "dose_day"
        const val MINUTES = "dose_minutes"
        const val DELAY = "delay_minutes"
        const val TAKEN = "com.example.medreminder.TAKEN"
        const val SNOOZE = "com.example.medreminder.SNOOZE"
        const val REPEAT = "com.example.medreminder.REPEAT"
        const val SKIP = "com.example.medreminder.SKIP"
        fun key(id: Int, day: Long, minutes: Int) = "$id/$day/$minutes"
    }

    private fun tag(id: Int, day: Long, minutes: Int) = "dose/" + key(id, day, minutes)
    private fun intent(id: Int, day: Long, minutes: Int, action: String) =
        Intent(context, MedicineAlarmReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("medreminder://dose/${key(id, day, minutes)}/$action")
            putExtra(MedicineAlarmReceiver.EXTRA_NOTIFICATION_ID, id)
            putExtra(DAY, day)
            putExtra(MINUTES, minutes)
        }

    private fun repeatPending(id: Int, day: Long, minutes: Int) =
        PendingIntent.getBroadcast(context, 0, intent(id, day, minutes, REPEAT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private suspend fun medicine(id: Int, day: Long, minutes: Int) =
        repository.getAllMedicines().first().firstOrNull {
            it.id == id && minutes in it.times && it.isScheduledOn(LocalDate.ofEpochDay(day))
        }

    suspend fun take(id: Int, day: Long, minutes: Int): Boolean = mutex.withLock {
        if (medicine(id, day, minutes) == null) return@withLock false
        val now = System.currentTimeMillis()
        require(com.example.medreminder.domain.model.ReminderPolicy.canMarkTaken(scheduledAt(day, minutes), now))
        val result = repository.recordTakenAndConsume(id, day, minutes, now)
        if (!result.accepted) return@withLock false
        alarms.cancel(repeatPending(id, day, minutes))
        notifications.cancel(tag(id, day, minutes), 0)
        result.lowStock?.let(::showLowStock)
        true
    }

    private fun showLowStock(stock: com.example.medreminder.domain.model.LowMedicineStock) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (!notifications.areNotificationsEnabled()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Напоминания о лекарствах", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val name = com.example.medreminder.domain.model.capitalizeFirstLetter(stock.name)
        val remaining = com.example.medreminder.domain.model.formatDoseForDisplay(stock.remaining)
        val text = "$name: осталось $remaining. Пополните запас препарата — он почти закончился."
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_capsule_notification)
            .setContentTitle("Пора пополнить аптечку")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            // One notification per preparation, independent of the number of schedules.
            notifications.notify("stock/${stock.preparationId}", 0, notification)
        } catch (_: SecurityException) { /* Consumption stays recorded if permission was revoked. */ }
    }

    suspend fun snooze(id: Int, day: Long, minutes: Int, delay: Int): Boolean = mutex.withLock {
        require(delay in setOf(15, 30, 60))
        if (medicine(id, day, minutes) == null) return@withLock false
        val record = records.find(id, day, minutes)
        if (record?.takenAt != null || record?.skippedAt != null) return@withLock false
        val scheduled = LocalDate.ofEpochDay(day).atTime(minutes / 60, minutes % 60).atZone(ZoneId.systemDefault())
        require(scheduled.toInstant().toEpochMilli() <= System.currentTimeMillis())
        val until = System.currentTimeMillis() + delay * 60_000L
        // Schedule first: a scheduling failure must not leave a false "snoozed" status.
        scheduleRepeat(id, day, minutes, until)
        records.save(DoseRecord(id, day, minutes, snoozedUntil = until,
            repeatCount = com.example.medreminder.domain.model.ReminderPolicy.MAX_REPEATS))
        notifications.cancel(tag(id, day, minutes), 0)
        true
    }

    private fun scheduledAt(day: Long, minutes: Int): Long =
        LocalDate.ofEpochDay(day).atTime(minutes / 60, minutes % 60)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    suspend fun skip(id: Int, day: Long, minutes: Int) = mutex.withLock {
        if (medicine(id, day, minutes) == null) return@withLock
        require(scheduledAt(day, minutes) <= System.currentTimeMillis())
        val record = records.find(id, day, minutes)
        if (record?.takenAt != null || record?.skippedAt != null) return@withLock
        records.save(DoseRecord(id, day, minutes, skippedAt = System.currentTimeMillis()))
        alarms.cancel(repeatPending(id, day, minutes))
        notifications.cancel(tag(id, day, minutes), 0)
    }

    suspend fun updateRepeatSettings(enabled: Boolean, interval: Int) {
        preferences.update(enabled, interval)
        restore()
    }

    private fun scheduleRepeat(id: Int, day: Long, minutes: Int, until: Long) {
        val pending = repeatPending(id, day, minutes)
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, pending)
                return
            } catch (_: SecurityException) { /* Permission may have changed. */ }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, pending)
    }

    suspend fun show(id: Int, day: Long, minutes: Int) = mutex.withLock {
        val medicine = medicine(id, day, minutes) ?: return@withLock
        val record = records.find(id, day, minutes)
        val now = System.currentTimeMillis()
        if (record?.takenAt != null || record?.skippedAt != null || scheduledAt(day, minutes) > now) return@withLock
        val pendingAt = record?.snoozedUntil ?: record?.autoReminderAt
        if (record?.notifiedAt != null && pendingAt == null) return@withLock
        if ((pendingAt ?: 0) > now + 1_000) return@withLock
        val automaticRepeat = record?.autoReminderAt != null && record.snoozedUntil == null
        val count = ((record?.repeatCount ?: 0) + if (automaticRepeat) 1 else 0)
            .coerceAtMost(com.example.medreminder.domain.model.ReminderPolicy.MAX_REPEATS)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return@withLock
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Напоминания о лекарствах", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(action: String, delay: Int = 0): PendingIntent {
            val target = intent(id, day, minutes, action).apply {
                data = Uri.parse(data.toString() + "/$delay")
                putExtra(DELAY, delay)
            }
            return PendingIntent.getBroadcast(context, 0, target,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val description = when {
            record?.snoozedUntil != null -> "Вы просили напомнить о приёме."
            count >= 2 -> "Время приёма прошло. Проверьте указания для пропущенной дозы в инструкции или назначении."
            count == 1 -> "Приём пока не отмечен. Вы уже приняли препарат?"
            else -> "Небольшая забота о себе — время вашего приёма."
        }
        val dosage = com.example.medreminder.domain.model.formatDoseForDisplay(medicine.dosage, medicine.form)
        val text = "${medicine.name} — $dosage. $description"
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_capsule_notification)
            .setContentTitle(if (record?.snoozedUntil != null || count > 0) "Напоминаем с заботой" else "Время позаботиться о себе")
            .setOnlyAlertOnce(false)
            .setWhen(now)
            .setContentText("${medicine.name} — $dosage")
            .setSubText("Приём в " + String.format(java.util.Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Принято", action(TAKEN))
            .addAction(0, "15 мин.", action(SNOOZE, 15))
            .addAction(0, "30 мин.", action(SNOOZE, 30))
            .build()
        try {
            notifications.notify(tag(id, day, minutes), 0, notification)
            val next = com.example.medreminder.domain.model.ReminderPolicy.nextRepeat(
                now, count, preferences.enabled, preferences.intervalMinutes)
            if (next != null) scheduleRepeat(id, day, minutes, next)
            records.save(DoseRecord(id, day, minutes, notifiedAt = now, autoReminderAt = next, repeatCount = count))
        } catch (_: SecurityException) { /* The user can revoke notifications at any time. */ }
    }

    suspend fun cancelDay(id: Int, day: Long) = mutex.withLock {
        records.observeAll().first().filter { it.medicineId == id && it.epochDay == day }.forEach {
            alarms.cancel(repeatPending(id, day, it.minutes))
        }
        repository.getAllMedicines().first().firstOrNull { it.id == id }?.times?.forEach {
            notifications.cancel(tag(id, day, it), 0)
        }
    }

    suspend fun cancelMedicine(id: Int) = mutex.withLock {
        records.observeAll().first().filter { it.medicineId == id }.forEach {
            alarms.cancel(repeatPending(id, it.epochDay, it.minutes))
        }
        notifications.cancel("stock/$id", 0)
        // Tags keep cancellation scoped to this medicine, without affecting other reminders.
        context.getSystemService(NotificationManager::class.java).activeNotifications
            .filter { it.tag?.startsWith("dose/$id/") == true }
            .forEach { notifications.cancel(it.tag, it.id) }
    }

    suspend fun schedulePastDoses(medicine: com.example.medreminder.domain.model.Medicine) = mutex.withLock {
        schedulePastDosesLocked(medicine, System.currentTimeMillis())
    }

    private suspend fun schedulePastDosesLocked(
        medicine: com.example.medreminder.domain.model.Medicine, now: Long
    ) {
        val date = java.time.Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        if (!medicine.isScheduledOn(date)) return
        val day = date.toEpochDay()
        medicine.times.distinct().forEach { minutes ->
            if (records.find(medicine.id, day, minutes) == null) {
                val next = com.example.medreminder.domain.model.ReminderPolicy.firstRepeatForPastDose(
                    scheduledAt(day, minutes), now, preferences.enabled, preferences.intervalMinutes
                )
                if (next != null) {
                    scheduleRepeat(medicine.id, day, minutes, next)
                    records.save(DoseRecord(medicine.id, day, minutes, autoReminderAt = next))
                }
            }
        }
    }

    suspend fun restore() = mutex.withLock {
        val medicines = repository.getAllMedicines().first()
        medicines.filter { !it.cabinetOnly }.forEach { MedicineAlarmScheduler(context).scheduleMedicineAlarm(it) }
        val current = System.currentTimeMillis()
        medicines.forEach { schedulePastDosesLocked(it, current) }
        records.observeAll().first().filter { it.takenAt == null && it.skippedAt == null }.forEach { record ->
            alarms.cancel(repeatPending(record.medicineId, record.epochDay, record.minutes))
            val auto = if (record.autoReminderAt != null && preferences.enabled &&
                current - scheduledAt(record.epochDay, record.minutes) < 24 * 60 * 60_000L)
                (record.notifiedAt ?: scheduledAt(record.epochDay, record.minutes)) + preferences.intervalMinutes * 60_000L else null
            val updated = record.copy(autoReminderAt = auto)
            if (updated != record) records.save(updated)
            val pendingAt = updated.snoozedUntil ?: updated.autoReminderAt
            if (pendingAt != null && medicines.any {
                    it.id == record.medicineId && record.minutes in it.times &&
                        it.isScheduledOn(LocalDate.ofEpochDay(record.epochDay))
                }) {
                scheduleRepeat(record.medicineId, record.epochDay, record.minutes, maxOf(current + 1_000, pendingAt))
            }
        }
    }
}

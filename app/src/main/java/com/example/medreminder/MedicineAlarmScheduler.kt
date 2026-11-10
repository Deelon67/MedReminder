package com.example.medreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medreminder.domain.model.Medicine
import java.time.ZonedDateTime

class MedicineAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // One pending alarm per medicine; the receiver schedules the following dose.
    fun scheduleMedicineAlarm(medicine: Medicine) {
        val next = medicine.nextDoseAfter(ZonedDateTime.now())
        if (next == null) {
            cancelMedicineAlarm(medicine)
            return
        }
        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_NOTIFICATION_ID, medicine.id)
            putExtra(DoseReminderManager.DAY, next.toLocalDate().toEpochDay())
            putExtra(DoseReminderManager.MINUTES, next.hour * 60 + next.minute)
        }
        val pending = PendingIntent.getBroadcast(context, medicine.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val trigger = next.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun cancelMedicineAlarm(medicine: Medicine) {
        val pending = PendingIntent.getBroadcast(context, medicine.id,
            Intent(context, MedicineAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }
}

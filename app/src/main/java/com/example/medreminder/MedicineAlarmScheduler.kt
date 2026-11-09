package com.example.medreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.medreminder.domain.model.Medicine
import java.util.Calendar

class MedicineAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleMedicineAlarm(medicine: Medicine) {
        Log.d("AlarmScheduler", "Планируем будильник для: ${medicine.name} на ${medicine.hour}:${medicine.minute}")

        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(MedicineAlarmReceiver.EXTRA_DOSAGE, medicine.dosage)
            putExtra(MedicineAlarmReceiver.EXTRA_NOTIFICATION_ID, medicine.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, medicine.hour)
            set(Calendar.MINUTE, medicine.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
                Log.d("AlarmScheduler", "Время прошло, планируем на завтра")
            }
        }

        Log.d("AlarmScheduler", "Время срабатывания: ${calendar.time}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Точный будильник установлен")
            } else {
                Log.w("AlarmScheduler", "Нет разрешения на точные будильники!")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelMedicineAlarm(medicine: Medicine) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Будильник отменён для ID: ${medicine.id}")
    }
}
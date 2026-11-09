package com.example.medreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// Receiver получает сигнал от AlarmManager и показывает уведомление
class MedicineAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "medicine_reminder_channel"
        const val CHANNEL_NAME = "Напоминания о лекарствах"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MedicineReceiver", "Получен сигнал будильника!")

        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: return
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        Log.d("MedicineReceiver", "Показываем уведомление: $medicineName - $dosage (ID: $notificationId)")

        // Создаем канал уведомлений (нужен для Android 8.0+)
        createNotificationChannel(context)

        // Создаем Intent для открытия приложения при клике на уведомление
        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Создаем уведомление
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Иконка уведомления
            .setContentTitle("Время принять лекарство!")
            .setContentText("$medicineName - $dosage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Уведомление исчезнет после клика
            .build()

        // Показываем уведомление
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notification)
            }
        } catch (e: SecurityException) {
            // Если нет разрешения на уведомления
            e.printStackTrace()
        }
    }

    // Создание канала уведомлений
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Высокий приоритет
            ).apply {
                description = "Уведомления о приеме лекарств"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
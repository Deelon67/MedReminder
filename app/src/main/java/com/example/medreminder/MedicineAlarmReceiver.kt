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
import com.example.medreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.core.component.get
import java.time.LocalDate

// Receiver получает сигнал от AlarmManager и показывает уведомление
class MedicineAlarmReceiver : BroadcastReceiver(), org.koin.core.component.KoinComponent {

    companion object {
        const val CHANNEL_ID = "medicine_reminder_channel"
        const val CHANNEL_NAME = "Напоминания о лекарствах"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (id <= 0) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    // Re-read Room: a deleted medicine must not produce a notification.
                    val medicine = get<MedicineRepository>().getAllMedicines().first()
                        .firstOrNull { it.id == id } ?: return@withTimeout
                    if (medicine.isScheduledOn(LocalDate.now())) {
                        showNotification(context, medicine.name, medicine.dosage, medicine.id)
                    }
                    MedicineAlarmScheduler(context).scheduleMedicineAlarm(medicine)
                }
            } catch (e: Exception) {
                Log.e("MedicineReceiver", "Не удалось обработать напоминание", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, medicineName: String, dosage: String, notificationId: Int) {

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
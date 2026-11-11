package com.example.medreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.medreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.time.LocalDate
import java.time.ZonedDateTime

class MedicineAlarmReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    val manager = get<DoseReminderManager>()
                    if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
                        manager.restore()
                        return@withTimeout
                    }
                    val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
                    if (id <= 0) return@withTimeout
                    val medicine = get<MedicineRepository>().getAllMedicines().first().firstOrNull { it.id == id }
                        ?: return@withTimeout
                    val now = ZonedDateTime.now()
                    val day = intent.getLongExtra(DoseReminderManager.DAY, now.toLocalDate().toEpochDay())
                    // Compatibility with alarms scheduled by an earlier app version.
                    val minutes = intent.getIntExtra(DoseReminderManager.MINUTES,
                        medicine.times.filter { it <= now.hour * 60 + now.minute }.maxOrNull() ?: medicine.times.minOrNull() ?: -1)
                    if (minutes !in 0..1439) return@withTimeout
                    LocalDate.ofEpochDay(day)
                    when (intent.action) {
                        DoseReminderManager.SKIP -> manager.skip(id, day, minutes)
                        DoseReminderManager.TAKEN -> {
                            if (manager.take(id, day, minutes)) {
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        context.applicationContext,
                                        R.string.taken_confirmation,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                        DoseReminderManager.SNOOZE -> {
                            val delay = intent.getIntExtra(DoseReminderManager.DELAY, 15)
                            if (manager.snooze(id, day, minutes, delay)) {
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        context.applicationContext,
                                        context.getString(R.string.snooze_confirmation, delay),
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                        DoseReminderManager.REPEAT -> manager.show(id, day, minutes)
                        else -> {
                            // Always schedule the next regular dose, including when this dose was already taken.
                            MedicineAlarmScheduler(context).scheduleMedicineAlarm(medicine)
                            manager.show(id, day, minutes)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MedicineReceiver", "Не удалось обработать напоминание", e)
            } finally {
                result.finish()
            }
        }
    }
}

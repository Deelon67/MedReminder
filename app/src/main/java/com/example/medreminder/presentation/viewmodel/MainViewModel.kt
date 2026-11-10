package com.example.medreminder.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.MedicineAlarmScheduler
import com.example.medreminder.domain.model.Medicine
import com.example.medreminder.domain.repository.MedicineRepository
import com.example.medreminder.domain.usecase.GetMedicinesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val getMedicinesUseCase: GetMedicinesUseCase,
    private val repository: MedicineRepository,
    context: Context,
    private val doseRecords: com.example.medreminder.data.db.DoseRecordDao,
    private val reminders: com.example.medreminder.DoseReminderManager
) : ViewModel() {

    private val alarmScheduler = MedicineAlarmScheduler(context)

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        processIntent(MainIntent.LoadMedicines)
        viewModelScope.launch {
            doseRecords.observeAll().collect { doses -> _state.update { it.copy(doses = doses) } }
        }
        viewModelScope.launch {
            try { reminders.restore() } catch (e: Exception) { Log.e("MedReminder", "Не удалось восстановить напоминания", e) }
        }
    }

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadMedicines -> loadMedicines()
            is MainIntent.AddMedicine -> addMedicine(intent.medicine)
            is MainIntent.DeleteMedicine -> deleteMedicine(intent.medicine)
            is MainIntent.HideMedicineOnDate -> hideMedicineOnDate(intent.medicine, intent.epochDay)
        }
    }

    private fun loadMedicines() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            getMedicinesUseCase().collect { medicines ->
                _state.update {
                    it.copy(
                        medicines = medicines,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun addMedicine(medicine: Medicine) {
        viewModelScope.launch { saveMedicine(medicine) }
    }

    fun clearSaveError() { _state.update { it.copy(saveError = null) } }

    suspend fun saveMedicine(medicine: Medicine): Boolean {
        if (_state.value.isSaving) return false
        if (medicine.name.isBlank() || medicine.dosage.isBlank() ||
            medicine.times.isEmpty() || medicine.times.any { it !in 0..1439 } ||
            medicine.weekdaysMask and 127 == 0 ||
            (medicine.courseDays != null && medicine.courseDays <= 0)) {
            _state.update { it.copy(saveError = "Проверьте название, дозировку и расписание.") }
            return false
        }
        _state.update { it.copy(isSaving = true, saveError = null) }
        try {
            val id = repository.addMedicine(medicine)
            try {
                alarmScheduler.scheduleMedicineAlarm(medicine.copy(id = id.toInt()))
            } catch (e: Exception) {
                _state.update { it.copy(error = "Лекарство сохранено, но не удалось настроить напоминание.") }
                Log.e("MedReminder", "Не удалось запланировать напоминание", e)
            }
            return true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(saveError = "Не удалось сохранить лекарство. Попробуйте ещё раз.") }
            return false
        } finally {
            _state.update { it.copy(isSaving = false) }
        }
    }

    suspend fun savePreparation(medicine: Medicine): Boolean {
        if (_state.value.isSaving) return false
        if (medicine.name.isBlank() || !medicine.cabinetOnly) {
            _state.update { it.copy(saveError = "Укажите название препарата.") }
            return false
        }
        _state.update { it.copy(isSaving = true, saveError = null) }
        return try {
            repository.addMedicine(medicine)
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(saveError = "Не удалось сохранить препарат. Попробуйте ещё раз.") }
            false
        } finally {
            _state.update { it.copy(isSaving = false) }
        }
    }

    fun takeDose(id: Int, day: Long, minutes: Int) = updateDose(id, day, minutes) {
        reminders.take(id, day, minutes)
    }

    fun snoozeDose(id: Int, day: Long, minutes: Int, delay: Int) = updateDose(id, day, minutes) {
        reminders.snooze(id, day, minutes, delay)
    }

    fun skipDose(id: Int, day: Long, minutes: Int) = updateDose(id, day, minutes) {
        reminders.skip(id, day, minutes)
    }

    fun updateReminderSettings(enabled: Boolean, interval: Int) {
        viewModelScope.launch {
            try {
                reminders.updateRepeatSettings(enabled, interval)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Не удалось изменить напоминания.") }
            }
        }
    }

    private fun updateDose(id: Int, day: Long, minutes: Int, action: suspend () -> Unit) {
        val key = com.example.medreminder.DoseReminderManager.key(id, day, minutes)
        if (key in _state.value.updatingDoses) return
        _state.update { it.copy(updatingDoses = it.updatingDoses + key, error = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MedReminder", "Не удалось обновить приём", e)
                _state.update { it.copy(error = "Не удалось обновить приём. Попробуйте ещё раз.") }
            } finally {
                _state.update { it.copy(updatingDoses = it.updatingDoses - key) }
            }
        }
    }

    private fun hideMedicineOnDate(medicine: Medicine, epochDay: Long) {
        viewModelScope.launch {
            try {
                repository.hideMedicineOnDate(medicine, epochDay)
                reminders.cancelDay(medicine.id, epochDay)
                alarmScheduler.scheduleMedicineAlarm(
                    medicine.copy(hiddenDates = medicine.hiddenDates + epochDay)
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MedReminder", "Не удалось убрать лекарство на выбранный день", e)
                _state.update { it.copy(error = "Не удалось обновить расписание.") }
            }
        }
    }

    private fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            try {
                val matching = _state.value.medicines.filter {
                    com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                        com.example.medreminder.domain.model.medicineNameKey(medicine.name)
                }
                matching.forEach { reminders.cancelMedicine(it.id) }
                repository.deleteMedicine(medicine)
                matching.forEach { alarmScheduler.cancelMedicineAlarm(it) }
                Log.d("MedReminder", "Лекарство удалено, будильник отменён")
            } catch (e: Exception) {
                Log.e("MedReminder", "Ошибка при удалении: ${e.message}")
                _state.update { it.copy(error = e.message) }
            }
        }
    }
}
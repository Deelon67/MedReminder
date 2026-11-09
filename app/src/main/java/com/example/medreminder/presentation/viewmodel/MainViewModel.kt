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
    context: Context
) : ViewModel() {

    private val alarmScheduler = MedicineAlarmScheduler(context)

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        processIntent(MainIntent.LoadMedicines)
    }

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadMedicines -> loadMedicines()
            is MainIntent.AddMedicine -> addMedicine(intent.medicine)
            is MainIntent.DeleteMedicine -> deleteMedicine(intent.medicine)
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

    private fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            try {
                repository.deleteMedicine(medicine)
                alarmScheduler.cancelMedicineAlarm(medicine)
                Log.d("MedReminder", "Лекарство удалено, будильник отменён")
            } catch (e: Exception) {
                Log.e("MedReminder", "Ошибка при удалении: ${e.message}")
                _state.update { it.copy(error = e.message) }
            }
        }
    }
}
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
        viewModelScope.launch {
            try {
                // Получаем ID от базы данных
                val generatedId = repository.addMedicine(medicine)

                Log.d("MedReminder", "Лекарство добавлено с ID: $generatedId")

                // Создаём новый объект Medicine с правильным ID
                val medicineWithId = medicine.copy(id = generatedId.toInt())

                // Планируем будильник с правильным ID
                alarmScheduler.scheduleMedicineAlarm(medicineWithId)

                Log.d("MedReminder", "Будильник запланирован на ${medicineWithId.hour}:${medicineWithId.minute}")
            } catch (e: Exception) {
                Log.e("MedReminder", "Ошибка при добавлении: ${e.message}")
                _state.update { it.copy(error = e.message) }
            }
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
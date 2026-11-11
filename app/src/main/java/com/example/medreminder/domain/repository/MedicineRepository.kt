package com.example.medreminder.domain.repository

import com.example.medreminder.domain.model.Medicine
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {

    fun getAllMedicines(): Flow<List<Medicine>>

    // Теперь возвращает ID добавленного лекарства
    suspend fun addMedicine(medicine: Medicine, updateQuantity: Boolean = true): Long

    suspend fun hideMedicineOnDate(medicine: Medicine, epochDay: Long)

    suspend fun recordTakenAndConsume(id: Int, epochDay: Long, minutes: Int, takenAt: Long):
        com.example.medreminder.domain.model.TakenDoseResult

    suspend fun deleteMedicine(medicine: Medicine)
}
package com.example.medreminder.domain.repository

import com.example.medreminder.domain.model.Medicine
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {

    fun getAllMedicines(): Flow<List<Medicine>>

    // Теперь возвращает ID добавленного лекарства
    suspend fun addMedicine(medicine: Medicine): Long

    suspend fun deleteMedicine(medicine: Medicine)
}
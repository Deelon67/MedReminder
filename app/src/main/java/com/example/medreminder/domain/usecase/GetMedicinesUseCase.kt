package com.example.medreminder.domain.usecase

import com.example.medreminder.domain.model.Medicine
import com.example.medreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow

// UseCase - это сценарий использования
// ViewModel вызывает UseCase, а UseCase работает с репозиторием
class GetMedicinesUseCase(
    private val repository: MedicineRepository
) {
    // operator fun invoke позволяет вызывать объект как функцию: getMedicinesUseCase()
    operator fun invoke(): Flow<List<Medicine>> {
        return repository.getAllMedicines()
    }
}
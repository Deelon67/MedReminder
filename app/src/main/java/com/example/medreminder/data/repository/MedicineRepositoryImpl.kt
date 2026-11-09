package com.example.medreminder.data.repository

import com.example.medreminder.data.db.MedicineDao
import com.example.medreminder.data.db.MedicineEntity
import com.example.medreminder.domain.model.Medicine
import com.example.medreminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicineRepositoryImpl(
    private val dao: MedicineDao
) : MedicineRepository {

    private fun MedicineEntity.toDomain() = Medicine(
        id = id,
        name = name,
        dosage = dosage,
        timeOfDay = timeOfDay,
        hour = hour,
        minute = minute,
        startEpochDay = startEpochDay,
        courseDays = courseDays,
        weekdaysMask = weekdaysMask,
        times = times.split(",").mapNotNull { it.toIntOrNull() }.filter { it in 0..1439 }
            .ifEmpty { listOf(hour * 60 + minute) },
        comment = comment
    )

    private fun Medicine.toEntity() = MedicineEntity(
        id = id,
        name = name,
        dosage = dosage,
        timeOfDay = timeOfDay,
        hour = hour,
        minute = minute,
        startEpochDay = startEpochDay,
        courseDays = courseDays,
        weekdaysMask = weekdaysMask,
        times = times.distinct().sorted().joinToString(","),
        comment = comment
    )

    override fun getAllMedicines(): Flow<List<Medicine>> {
        return dao.getAllMedicines().map { list ->
            list.map { it.toDomain() }
        }
    }

    // Теперь возвращаем ID
    override suspend fun addMedicine(medicine: Medicine): Long {
        return dao.insertMedicine(medicine.toEntity())
    }

    override suspend fun deleteMedicine(medicine: Medicine) {
        dao.deleteMedicine(medicine.toEntity())
    }
}
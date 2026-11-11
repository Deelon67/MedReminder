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
            .ifEmpty { if (cabinetOnly) emptyList() else listOf(hour * 60 + minute) },
        comment = com.example.medreminder.domain.model.capitalizeFirstLetter(comment),
        hiddenDates = hiddenDates.split(",").mapNotNull { it.toLongOrNull() }.toSet(),
        cabinetOnly = cabinetOnly, form = form, quantity = quantity, expiryEpochDay = expiryEpochDay,
        preparationId = preparationId
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
        comment = com.example.medreminder.domain.model.capitalizeFirstLetter(comment),
        hiddenDates = hiddenDates.sorted().joinToString(","),
        cabinetOnly = cabinetOnly, form = form, quantity = quantity, expiryEpochDay = expiryEpochDay,
        preparationId = preparationId
    )

    override fun getAllMedicines(): Flow<List<Medicine>> {
        return dao.getAllMedicines().map { list ->
            val inventory = list.filter { it.cabinetOnly }.associateBy { it.id }.mapValues { (_, preparation) ->
                if (preparation.form.isNotBlank()) preparation else {
                    val forms = list.filter {
                        !it.cabinetOnly && (it.preparationId == preparation.id ||
                            com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                                com.example.medreminder.domain.model.medicineNameKey(preparation.name))
                    }.map { com.example.medreminder.domain.model.preparationFormFromDose(it.dosage) }
                        .filter { it.isNotEmpty() }.distinct()
                    preparation.copy(form = forms.singleOrNull().orEmpty())
                }
            }
            list.map { entity ->
                val medicine = (if (entity.cabinetOnly) inventory[entity.id] ?: entity else entity).toDomain()
                val updated = inventory[entity.preparationId]?.let { preparation ->
                    medicine.copy(
                        name = preparation.name, form = preparation.form,
                        quantity = preparation.quantity, expiryEpochDay = preparation.expiryEpochDay
                    )
                } ?: medicine
                if (updated.cabinetOnly) updated else updated.copy(
                    dosage = com.example.medreminder.domain.model.doseWithFormUnit(updated.dosage, updated.form)
                )
            }
        }
    }

    // Теперь возвращаем ID
    override suspend fun addMedicine(medicine: Medicine, updateQuantity: Boolean): Long {
        return dao.saveWithCabinetDetails(medicine.toEntity(), updateQuantity)
    }

    override suspend fun hideMedicineOnDate(medicine: Medicine, epochDay: Long) {
        dao.updateHiddenDates(medicine.id, (medicine.hiddenDates + epochDay).sorted().joinToString(","))
    }

    override suspend fun recordTakenAndConsume(id: Int, epochDay: Long, minutes: Int, takenAt: Long) =
        dao.recordTakenAndConsume(id, epochDay, minutes, takenAt)

    override suspend fun deleteMedicine(medicine: Medicine) {
        dao.deleteByName(medicine.name)
    }
}
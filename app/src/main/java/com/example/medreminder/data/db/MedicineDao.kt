package com.example.medreminder.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Query("SELECT * FROM medicines ORDER BY hour ASC, minute ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    // ВАЖНО: теперь возвращает Long (ID добавленной записи)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Query("UPDATE medicines SET hiddenDates = :dates WHERE id = :id")
    suspend fun updateHiddenDates(id: Int, dates: String)

    @Query("SELECT * FROM medicines")
    suspend fun getMedicinesSnapshot(): List<MedicineEntity>

    @androidx.room.Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @androidx.room.Transaction
    suspend fun saveWithCabinetDetails(medicine: MedicineEntity): Long {
        val matching = getMedicinesSnapshot().filter {
            com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                com.example.medreminder.domain.model.medicineNameKey(medicine.name)
        }
        if (medicine.cabinetOnly && matching.isNotEmpty()) {
            matching.forEach { existing ->
                updateMedicine(existing.copy(
                    dosage = if (existing.cabinetOnly) medicine.dosage else existing.dosage,
                    form = medicine.form,
                    quantity = medicine.quantity,
                    expiryEpochDay = medicine.expiryEpochDay,
                    comment = if (existing.cabinetOnly) medicine.comment else existing.comment
                ))
            }
            val cabinet = matching.firstOrNull { it.cabinetOnly }
            return cabinet?.id?.toLong() ?: insertMedicine(medicine)
        }
        val selected = medicine.preparationId?.let { id ->
            getMedicinesSnapshot().firstOrNull { it.id == id && it.cabinetOnly }
                ?: error("Выбранный препарат удалён из аптечки")
        }
        if (medicine.cabinetOnly) return insertMedicine(medicine)
        val details = selected ?: matching.firstOrNull { it.cabinetOnly }
        val inferredForm = com.example.medreminder.domain.model.preparationFormFromDose(medicine.dosage)
        val inventory = details?.let { existing ->
            if (existing.form.isBlank() && inferredForm.isNotEmpty()) {
                existing.copy(form = inferredForm).also { updateMedicine(it) }
            } else existing
        } ?: medicine.copy(
            id = 0, cabinetOnly = true, dosage = "", comment = "",
            form = medicine.form.ifBlank { inferredForm },
            times = "", weekdaysMask = 0, hiddenDates = "", preparationId = null
        ).let { it.copy(id = insertMedicine(it).toInt()) }
        // A dose keeps its own quantity and instructions; inventory owns package details.
        return insertMedicine(medicine.copy(
            name = inventory.name,
            preparationId = inventory.id,
            form = inventory.form,
            quantity = inventory.quantity,
            expiryEpochDay = inventory.expiryEpochDay
        ))
    }

    @androidx.room.Transaction
    suspend fun deleteByName(name: String) {
        getMedicinesSnapshot().filter {
            com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                com.example.medreminder.domain.model.medicineNameKey(name)
        }.forEach { deleteMedicine(it) }
    }

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)
}
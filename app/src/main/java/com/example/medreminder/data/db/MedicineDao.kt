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
    suspend fun saveWithCabinetDetails(medicine: MedicineEntity, updateQuantity: Boolean = true): Long {
        val matching = getMedicinesSnapshot().filter {
            com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                com.example.medreminder.domain.model.medicineNameKey(medicine.name)
        }
        if (medicine.cabinetOnly && matching.isNotEmpty()) {
            val quantity = if (updateQuantity) medicine.quantity else
                matching.firstOrNull { it.cabinetOnly }?.quantity ?: medicine.quantity
            matching.forEach { existing ->
                updateMedicine(existing.copy(
                    dosage = if (existing.cabinetOnly) medicine.dosage else
                        com.example.medreminder.domain.model.doseAfterFormChange(
                            existing.dosage, existing.form, medicine.form
                        ),
                    form = medicine.form,
                    quantity = quantity,
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

    @Query("SELECT * FROM dose_records WHERE medicineId = :id AND epochDay = :day AND minutes = :minutes")
    suspend fun findDoseForConsumption(id: Int, day: Long, minutes: Int): DoseRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTakenDose(record: DoseRecord)

    @androidx.room.Transaction
    suspend fun recordTakenAndConsume(id: Int, day: Long, minutes: Int, now: Long):
        com.example.medreminder.domain.model.TakenDoseResult {
        val rows = getMedicinesSnapshot()
        val schedule = rows.firstOrNull { it.id == id && !it.cabinetOnly }
            ?: return com.example.medreminder.domain.model.TakenDoseResult(false)
        val model = com.example.medreminder.domain.model.Medicine(
            schedule.id, schedule.name, schedule.dosage, schedule.timeOfDay, schedule.hour, schedule.minute,
            startEpochDay = schedule.startEpochDay, courseDays = schedule.courseDays,
            weekdaysMask = schedule.weekdaysMask,
            hiddenDates = schedule.hiddenDates.split(",").mapNotNull { it.toLongOrNull() }.toSet()
        )
        val times = schedule.times.split(",").mapNotNull { it.toIntOrNull() }
            .ifEmpty { listOf(schedule.hour * 60 + schedule.minute) }
        if (minutes !in times || !model.isScheduledOn(java.time.LocalDate.ofEpochDay(day))) {
            return com.example.medreminder.domain.model.TakenDoseResult(false)
        }
        val existing = findDoseForConsumption(id, day, minutes)
        if (existing?.skippedAt != null) return com.example.medreminder.domain.model.TakenDoseResult(false)
        if (existing?.takenAt != null) return com.example.medreminder.domain.model.TakenDoseResult(true)
        saveTakenDose(DoseRecord(id, day, minutes, takenAt = now))

        val inventory = if (schedule.preparationId != null) {
            rows.firstOrNull { it.id == schedule.preparationId && it.cabinetOnly }
        } else rows.firstOrNull {
            it.cabinetOnly && com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                com.example.medreminder.domain.model.medicineNameKey(schedule.name)
        }
        if (inventory == null) return com.example.medreminder.domain.model.TakenDoseResult(true)
        val consumption = com.example.medreminder.domain.model.consumeMedicineStock(
            inventory.quantity, schedule.dosage, inventory.form
        ) ?: return com.example.medreminder.domain.model.TakenDoseResult(true)
        rows.filter {
            it.id == inventory.id || it.preparationId == inventory.id ||
                (!it.cabinetOnly && it.preparationId == null &&
                    com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                    com.example.medreminder.domain.model.medicineNameKey(inventory.name))
        }.forEach { updateMedicine(it.copy(quantity = consumption.remaining)) }
        val lowStock = if (consumption.isLow)
            com.example.medreminder.domain.model.LowMedicineStock(inventory.id, inventory.name, consumption.remaining)
        else null
        return com.example.medreminder.domain.model.TakenDoseResult(true, lowStock)
    }

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)
}
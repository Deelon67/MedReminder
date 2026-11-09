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

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)
}
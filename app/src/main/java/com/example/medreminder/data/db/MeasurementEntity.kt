package com.example.medreminder.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "health_measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Double,
    val secondary: Double?,
    val pulse: Int?,
    val measuredAt: Long
)

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM health_measurements ORDER BY measuredAt DESC, id DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long
}

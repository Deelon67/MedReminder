package com.example.medreminder.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "dose_records",
    primaryKeys = ["medicineId", "epochDay", "minutes"],
    foreignKeys = [ForeignKey(entity = MedicineEntity::class, parentColumns = ["id"],
        childColumns = ["medicineId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("medicineId")]
)
data class DoseRecord(
    val medicineId: Int,
    val epochDay: Long,
    val minutes: Int,
    val takenAt: Long? = null,
    val snoozedUntil: Long? = null,
    val notifiedAt: Long? = null,
    val skippedAt: Long? = null,
    val autoReminderAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val repeatCount: Int = 0
)

@Dao
interface DoseRecordDao {
    @Query("SELECT * FROM dose_records")
    fun observeAll(): Flow<List<DoseRecord>>

    @Query("SELECT * FROM dose_records WHERE medicineId = :id AND epochDay = :day AND minutes = :minutes")
    suspend fun find(id: Int, day: Long, minutes: Int): DoseRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: DoseRecord)
}

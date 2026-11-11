package com.example.medreminder.domain.repository

import com.example.medreminder.domain.model.HealthMeasurement
import kotlinx.coroutines.flow.Flow

interface MeasurementRepository {
    fun observeAll(): Flow<List<HealthMeasurement>>
    suspend fun add(measurement: HealthMeasurement)
}

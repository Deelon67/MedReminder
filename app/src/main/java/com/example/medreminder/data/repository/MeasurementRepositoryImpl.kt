package com.example.medreminder.data.repository

import com.example.medreminder.data.db.MeasurementDao
import com.example.medreminder.data.db.MeasurementEntity
import com.example.medreminder.domain.model.HealthMeasurement
import com.example.medreminder.domain.model.MeasurementType
import com.example.medreminder.domain.repository.MeasurementRepository
import kotlinx.coroutines.flow.map

class MeasurementRepositoryImpl(private val dao: MeasurementDao) : MeasurementRepository {
    override fun observeAll() = dao.observeAll().map { rows ->
        rows.map { HealthMeasurement(it.id, MeasurementType.valueOf(it.type), it.value,
            it.secondary, it.pulse, it.measuredAt) }
    }

    override suspend fun delete(id: Long) { dao.delete(id) }

    override suspend fun add(measurement: HealthMeasurement) {
        dao.insert(MeasurementEntity(type = measurement.type.name, value = measurement.value,
            secondary = measurement.secondary, pulse = measurement.pulse, measuredAt = measurement.measuredAt))
    }
}

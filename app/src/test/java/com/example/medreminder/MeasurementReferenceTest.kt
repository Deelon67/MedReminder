package com.example.medreminder

import com.example.medreminder.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementReferenceTest {
    private fun level(type: MeasurementType, value: Double, secondary: Double? = null) =
        HealthMeasurement(type = type, value = value, secondary = secondary, measuredAt = 0).reference().level

    @Test fun pressureUsesBothValuesAndPrioritizesOutsideRange() {
        assertEquals(MeasurementLevel.NORMAL, level(MeasurementType.PRESSURE, 119.0, 79.0))
        assertEquals(MeasurementLevel.ELEVATED, level(MeasurementType.PRESSURE, 120.0, 79.0))
        assertEquals(MeasurementLevel.ELEVATED, level(MeasurementType.PRESSURE, 110.0, 80.0))
        assertEquals(MeasurementLevel.OUTSIDE, level(MeasurementType.PRESSURE, 140.0, 70.0))
        assertEquals(MeasurementLevel.OUTSIDE, level(MeasurementType.PRESSURE, 110.0, 90.0))
        assertEquals(MeasurementLevel.OUTSIDE, level(MeasurementType.PRESSURE, 130.0, 59.0))
        assertEquals(MeasurementLevel.UNKNOWN, level(MeasurementType.PRESSURE, 120.0))
    }

    @Test fun adultRestingPulseBoundaries() {
        assertEquals(MeasurementLevel.OUTSIDE, level(MeasurementType.PULSE, 59.0))
        assertEquals(MeasurementLevel.NORMAL, level(MeasurementType.PULSE, 60.0))
        assertEquals(MeasurementLevel.NORMAL, level(MeasurementType.PULSE, 100.0))
        assertEquals(MeasurementLevel.ELEVATED, level(MeasurementType.PULSE, 101.0))
    }

    @Test fun fastingGlucoseBoundaries() {
        assertEquals(MeasurementLevel.OUTSIDE, level(MeasurementType.GLUCOSE, 3.8))
        assertEquals(MeasurementLevel.NORMAL, level(MeasurementType.GLUCOSE, 3.9))
        assertEquals(MeasurementLevel.NORMAL, level(MeasurementType.GLUCOSE, 5.5))
        assertEquals(MeasurementLevel.ELEVATED, level(MeasurementType.GLUCOSE, 5.6))
        assertEquals(MeasurementLevel.ELEVATED, level(MeasurementType.GLUCOSE, 6.9))
        assertEquals(MeasurementLevel.OUTSIDE, level(MeasurementType.GLUCOSE, 7.0))
    }

    @Test fun temperatureBoundaries() {
        assertEquals(MeasurementLevel.OUTSIDE, level(MeasurementType.TEMPERATURE, 36.5))
        assertEquals(MeasurementLevel.NORMAL, level(MeasurementType.TEMPERATURE, 36.6))
        assertEquals(MeasurementLevel.NORMAL, level(MeasurementType.TEMPERATURE, 36.8))
        assertEquals(MeasurementLevel.ELEVATED, level(MeasurementType.TEMPERATURE, 36.9))
        assertEquals(MeasurementLevel.ELEVATED, level(MeasurementType.TEMPERATURE, 37.9))
        assertEquals(MeasurementLevel.OUTSIDE, level(MeasurementType.TEMPERATURE, 38.0))
    }

    @Test fun weightAndInvalidValuesAreNotGivenInventedNorms() {
        assertEquals(MeasurementLevel.UNKNOWN, level(MeasurementType.WEIGHT, 70.0))
        assertEquals(MeasurementLevel.UNKNOWN, level(MeasurementType.WEIGHT, 150.0))
        assertEquals(MeasurementLevel.UNKNOWN, level(MeasurementType.GLUCOSE, Double.NaN))
    }
}

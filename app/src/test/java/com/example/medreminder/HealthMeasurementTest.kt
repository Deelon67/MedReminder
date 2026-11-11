package com.example.medreminder

import com.example.medreminder.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class HealthMeasurementTest {
    @Test fun pressureKeepsBothValuesAndOptionalPulse() {
        val result = parseMeasurement(MeasurementType.PRESSURE, "120", "80", "72", 1234)!!
        assertEquals(120.0, result.value, 0.0)
        assertEquals(80.0, result.secondary!!, 0.0)
        assertEquals(72, result.pulse)
        assertEquals(1234L, result.measuredAt)
        assertEquals("120/80 мм рт. ст.", result.displayValue())
        assertNull(parseMeasurement(MeasurementType.PRESSURE, "80", "120", measuredAt = 0))
        assertNull(parseMeasurement(MeasurementType.PRESSURE, "120", "", measuredAt = 0))
        assertNull(parseMeasurement(MeasurementType.PRESSURE, "120,5", "80", measuredAt = 0))
    }

    @Test fun decimalMeasurementsAcceptCommaAndDotWithoutLosingPrecision() {
        listOf(MeasurementType.GLUCOSE, MeasurementType.WEIGHT, MeasurementType.TEMPERATURE).forEach { type ->
            assertEquals(5.25, parseMeasurement(type, "5,25", measuredAt = 0)!!.value, 0.0)
            assertEquals(5.25, parseMeasurement(type, "5.25", measuredAt = 0)!!.value, 0.0)
        }
        assertEquals("36,6 °C", parseMeasurement(MeasurementType.TEMPERATURE, "36,6", measuredAt = 0)!!.displayValue())
    }

    @Test fun incompleteInvalidAndNonPositiveNumbersCannotBeSaved() {
        listOf("", "0", "-1", "NaN", "Infinity", "1e3", "12,", "1.234", "abc").forEach {
            assertNull(it, parseMeasurement(MeasurementType.WEIGHT, it, measuredAt = 0))
        }
        assertNull(parseMeasurement(MeasurementType.PULSE, "72,5", measuredAt = 0))
        assertNull(parseMeasurement(MeasurementType.PRESSURE, "120", "80", "0", 0))
        val pulse = parseMeasurement(MeasurementType.PULSE, "72", "80", "100", 0)!!
        assertNull(pulse.secondary)
        assertNull(pulse.pulse)
    }
}

package com.example.medreminder.domain.model

enum class MeasurementType(val title: String, val unit: String) {
    PRESSURE("Давление", "мм рт. ст."),
    PULSE("Пульс", "уд/мин"),
    GLUCOSE("Глюкоза", "ммоль/л"),
    WEIGHT("Вес", "кг"),
    TEMPERATURE("Температура", "°C")
}

data class HealthMeasurement(
    val id: Long = 0,
    val type: MeasurementType,
    val value: Double,
    val secondary: Double? = null,
    val pulse: Int? = null,
    val measuredAt: Long
)

fun parseMeasurement(
    type: MeasurementType, first: String, second: String = "", pulse: String = "",
    measuredAt: Long
): HealthMeasurement? {
    fun number(text: String): Double? {
        if (!text.matches(Regex("[0-9]{1,6}([.,][0-9]{1,2})?"))) return null
        return text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    }
    val value = number(first) ?: return null
    if (type == MeasurementType.PULSE && value % 1.0 != 0.0) return null
    val secondary = if (type == MeasurementType.PRESSURE) number(second) ?: return null else null
    if (type == MeasurementType.PRESSURE && (value % 1.0 != 0.0 || secondary!! % 1.0 != 0.0 || value <= secondary)) return null
    val pulseValue = if (type == MeasurementType.PRESSURE && pulse.isNotBlank()) {
        pulse.toIntOrNull()?.takeIf { it > 0 && pulse.length <= 6 } ?: return null
    } else null
    return HealthMeasurement(type = type, value = value, secondary = secondary,
        pulse = pulseValue, measuredAt = measuredAt)
}

fun formatMeasurementValue(value: Double): String =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')

fun HealthMeasurement.displayValue(): String =
    if (type == MeasurementType.PRESSURE)
        "${formatMeasurementValue(value)}/${formatMeasurementValue(secondary ?: 0.0)} ${type.unit}"
    else "${formatMeasurementValue(value)} ${type.unit}"

package com.example.medreminder.domain.model

enum class MeasurementLevel(val title: String) {
    NORMAL("В норме"),
    ELEVATED("Выше нормы"),
    OUTSIDE("Вне нормы"),
    UNKNOWN("Без оценки")
}

data class MeasurementReference(val level: MeasurementLevel, val hint: String)

// Adult resting reference values, not a diagnosis.
// https://medlineplus.gov/ency/article/002341.htm
// https://professional.heart.org/en/science-news/2025-high-blood-pressure-guideline/top-things-to-know
// https://medlineplus.gov/ency/article/003482.htm
// https://www.nhs.uk/symptoms/fever-in-adults/
// https://www.cdc.gov/bmi/adult-calculator/
fun HealthMeasurement.reference(): MeasurementReference {
    val level = when (type) {
        MeasurementType.PRESSURE -> {
            val lower = secondary
            when {
                lower == null || !lower.isFinite() -> MeasurementLevel.UNKNOWN
                value < 90 || lower < 60 || value >= 140 || lower >= 90 -> MeasurementLevel.OUTSIDE
                value >= 120 || lower >= 80 -> MeasurementLevel.ELEVATED
                else -> MeasurementLevel.NORMAL
            }
        }
        MeasurementType.PULSE -> when {
            value < 60 -> MeasurementLevel.OUTSIDE
            value > 100 -> MeasurementLevel.ELEVATED
            else -> MeasurementLevel.NORMAL
        }
        MeasurementType.GLUCOSE -> when {
            value < 3.9 || value >= 7.0 -> MeasurementLevel.OUTSIDE
            value >= 5.6 -> MeasurementLevel.ELEVATED
            else -> MeasurementLevel.NORMAL
        }
        MeasurementType.TEMPERATURE -> when {
            value < 36.6 || value >= 38.0 -> MeasurementLevel.OUTSIDE
            value > 36.8 -> MeasurementLevel.ELEVATED
            else -> MeasurementLevel.NORMAL
        }
        MeasurementType.WEIGHT -> MeasurementLevel.UNKNOWN
    }
    val hint = when (type) {
        MeasurementType.PRESSURE -> "Ориентир в покое: 90–119 / 60–79 мм рт. ст."
        MeasurementType.PULSE -> "Ориентир в покое: 60–100 уд/мин"
        MeasurementType.GLUCOSE -> "Только натощак: 3,9–5,5 ммоль/л. После еды эта оценка не применяется."
        MeasurementType.TEMPERATURE -> "Диапазон приложения: 36,6–36,8 °C. Не универсальная медицинская норма."
        MeasurementType.WEIGHT -> "Норма веса зависит от роста. Здесь отслеживается изменение веса."
    }
    return MeasurementReference(if (value.isFinite() && value > 0) level else MeasurementLevel.UNKNOWN, hint)
}

package com.example.medreminder.domain.model

import java.math.BigDecimal
import java.util.Locale

data class StockConsumption(val remaining: String, val isLow: Boolean)
data class LowMedicineStock(val preparationId: Int, val name: String, val remaining: String)
data class TakenDoseResult(val accepted: Boolean, val lowStock: LowMedicineStock? = null)

private data class StockAmount(val amount: BigDecimal, val unit: String, val originalUnit: String)

private fun parseStockAmount(text: String): StockAmount? {
    val match = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s+([А-Яа-яЁё]+)\\.?\\s*$")
        .matchEntire(text) ?: return null
    val amount = match.groupValues[1].replace(',', '.').toBigDecimalOrNull() ?: return null
    val originalUnit = match.groupValues[2]
    val unit = when (originalUnit.lowercase(Locale.ROOT)) {
        "табл", "таблетка", "таблетки", "таблеток" -> "табл"
        "капс", "капсула", "капсулы", "капсул" -> "капс"
        "кап", "капля", "капли", "капель" -> "кап"
        "шт", "штука", "штуки", "штук" -> "шт"
        "мл", "миллилитр", "миллилитра", "миллилитров" -> "мл"
        "мг", "миллиграмм", "миллиграмма", "миллиграммов" -> "мг"
        "г", "грамм", "грамма", "граммов" -> "г"
        "впрыск", "впрыска", "впрысков" -> "впрыск"
        else -> return null
    }
    val displayUnit = if (unit in setOf("табл", "капс", "кап", "шт")) "$unit." else unit
    return StockAmount(amount, unit, displayUnit)
}

fun preparationStockUnits(form: String): List<String> = when (form) {
    "Таблетки" -> listOf("табл.")
    "Капсулы" -> listOf("капс.")
    "Свечи" -> listOf("шт.")
    "Капли" -> listOf("кап.", "мл")
    "Спрей" -> listOf("впрыск", "мл")
    "Сироп", "Раствор" -> listOf("мл")
    "Мазь", "Крем", "Гель", "Порошок" -> listOf("г")
    "Другое" -> listOf("мг")
    else -> emptyList()
}

fun savedStockUnit(quantity: String): String? = parseStockAmount(quantity)?.originalUnit

// Only mass units have an exact conversion; never infer tablet strength or drops per millilitre.
fun consumeMedicineStock(quantity: String, dosage: String, form: String): StockConsumption? {
    val stock = parseStockAmount(quantity) ?: return null
    val dose = parseStockAmount(doseWithFormUnit(dosage, form)) ?: return null
    if (dose.amount <= BigDecimal.ZERO) return null
    val consumed = when {
        stock.unit == dose.unit -> dose.amount
        stock.unit == "г" && dose.unit == "мг" -> dose.amount.movePointLeft(3)
        stock.unit == "мг" && dose.unit == "г" -> dose.amount.movePointRight(3)
        else -> return null
    }
    val remaining = stock.amount.subtract(consumed).max(BigDecimal.ZERO)
    val number = remaining.stripTrailingZeros().toPlainString().replace('.', ',')
    val threshold = if (stock.unit == "мг") BigDecimal(5000) else BigDecimal(5)
    return StockConsumption("$number ${stock.originalUnit}", remaining < threshold)
}

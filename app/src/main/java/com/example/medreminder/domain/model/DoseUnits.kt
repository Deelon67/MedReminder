package com.example.medreminder.domain.model

fun scheduleDoseUnit(form: String): String = when (form) {
    "Таблетки" -> "табл."
    "Капсулы" -> "капс."
    "Свечи" -> "шт."
    "Сироп", "Раствор" -> "мл"
    "Капли" -> "кап."
    "Спрей" -> "впрыск."
    "Мазь", "Крем", "Гель", "Порошок" -> "г"
    else -> ""
}

fun preparationFormFromDose(dosage: String): String {
    val unit = Regex("^\\s*[0-9]+(?:[.,][0-9]+)?\\s+(.+?)\\s*$")
        .matchEntire(dosage)?.groupValues?.get(1)
        ?.lowercase(java.util.Locale.ROOT)?.removeSuffix(".") ?: return ""
    return when (unit) {
        "табл", "таблетка", "таблетки", "таблеток" -> "Таблетки"
        "капс", "капсула", "капсулы", "капсул" -> "Капсулы"
        "кап", "капля", "капли", "капель" -> "Капли"
        "впрыск", "впрыска", "впрысков" -> "Спрей"
        else -> ""
    }
}

fun doseAfterFormChange(dosage: String, oldForm: String, newForm: String): String {
    if (oldForm == newForm) return dosage
    val oldUnit = scheduleDoseUnit(oldForm)
    val newUnit = scheduleDoseUnit(newForm)
    if (oldUnit.isEmpty() || newUnit.isEmpty()) return dosage
    val match = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s+(.+?)\\s*$")
        .matchEntire(dosage) ?: return dosage
    return if (match.groupValues[2].equals(oldUnit, ignoreCase = true)) {
        "${match.groupValues[1]} $newUnit"
    } else dosage
}

fun formatDoseForDisplay(dosage: String, form: String = ""): String {
    val match = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s+([А-Яа-яЁё]+)\\.?\\s*$")
        .matchEntire(dosage) ?: return dosage
    val amount = match.groupValues[1]
    val unit = match.groupValues[2].lowercase(java.util.Locale.ROOT)
    val words = when (unit) {
        "табл", "таблетка", "таблетки", "таблеток" -> listOf("таблетка", "таблетки", "таблеток")
        "капс", "капсула", "капсулы", "капсул" -> listOf("капсула", "капсулы", "капсул")
        "кап", "капля", "капли", "капель" -> listOf("капля", "капли", "капель")
        "впрыск", "впрыска", "впрысков" -> listOf("впрыск", "впрыска", "впрысков")
        "мл", "миллилитр", "миллилитра", "миллилитров" -> listOf("миллилитр", "миллилитра", "миллилитров")
        "мг", "миллиграмм", "миллиграмма", "миллиграммов" -> listOf("миллиграмм", "миллиграмма", "миллиграммов")
        "г", "грамм", "грамма", "граммов" -> listOf("грамм", "грамма", "граммов")
        "шт" -> if (form == "Свечи") listOf("свеча", "свечи", "свечей")
            else listOf("штука", "штуки", "штук")
        else -> return dosage
    }
    val ending = if (amount.contains('.') || amount.contains(',')) 1 else {
        val lastTwo = amount.takeLast(2).toInt()
        when {
            lastTwo in 11..14 -> 2
            lastTwo % 10 == 1 -> 0
            lastTwo % 10 in 2..4 -> 1
            else -> 2
        }
    }
    return "$amount ${words[ending]}"
}

fun doseWithFormUnit(dosage: String, form: String): String {
    val amount = dosage.trim()
    val unit = scheduleDoseUnit(form)
    return if (unit.isNotEmpty() && amount.matches(Regex("[0-9]+([.,][0-9]+)?"))) {
        "$amount $unit"
    } else dosage
}

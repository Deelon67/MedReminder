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

fun doseWithFormUnit(dosage: String, form: String): String {
    val amount = dosage.trim()
    val unit = scheduleDoseUnit(form)
    return if (unit.isNotEmpty() && amount.matches(Regex("[0-9]+([.,][0-9]+)?"))) {
        "$amount $unit"
    } else dosage
}

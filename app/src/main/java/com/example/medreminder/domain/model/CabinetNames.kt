package com.example.medreminder.domain.model

import java.util.Locale

fun capitalizeFirstLetter(text: String): String {
    val index = text.indexOfFirst { it.isLetter() }
    if (index < 0) return text
    return text.replaceRange(index, index + 1, text[index].uppercaseChar().toString())
}

fun capitalizeMedicineName(name: String): String = capitalizeFirstLetter(name)

fun medicineNameKey(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

fun cabinetMedicines(medicines: List<Medicine>): List<Medicine> =
    medicines.groupBy { medicineNameKey(it.name) }.values.map { entries ->
        val primary = entries.firstOrNull { it.cabinetOnly } ?: entries.first()
        primary.copy(
            form = entries.firstOrNull { it.form.isNotBlank() }?.form.orEmpty(),
            quantity = entries.firstOrNull { it.quantity.isNotBlank() }?.quantity.orEmpty(),
            expiryEpochDay = entries.firstNotNullOfOrNull { it.expiryEpochDay },
            comment = if (primary.cabinetOnly) primary.comment else "",
            dosage = if (primary.cabinetOnly) primary.dosage else ""
        )
    }.sortedBy { medicineNameKey(it.name) }

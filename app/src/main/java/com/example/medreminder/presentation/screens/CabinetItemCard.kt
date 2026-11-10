package com.example.medreminder.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medreminder.domain.model.Medicine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun CabinetMedicineCard(medicine: Medicine, today: LocalDate, onAddSchedule: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    DashboardPanel(Modifier.fillMaxWidth().clickable(onClickLabel = "Подробнее о препарате", onClick = onOpen)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(medicine.name, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Удалить препарат", tint = MaterialTheme.colorScheme.error)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (medicine.form.isNotBlank()) Text("Форма выпуска: ${medicine.form}", color = Muted)
            if (medicine.dosage.isNotBlank()) Text("Дозировка: ${medicine.dosage}", color = Muted)
            if (medicine.quantity.isNotBlank()) Text("В запасе: ${medicine.quantity}", color = Muted)
            medicine.expiryEpochDay?.let {
                val date = LocalDate.ofEpochDay(it)
                val expired = date.isBefore(today)
                Text(
                    (if (expired) "Срок годности истёк: " else "Годен до: ") +
                        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    color = if (expired) MaterialTheme.colorScheme.error else Muted
                )
            }
            if (medicine.comment.isNotBlank()) Text("Заметка: ${medicine.comment}", color = Muted)
            OutlinedButton(onClick = onAddSchedule, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить схему приёма")
            }
        }
    }
}

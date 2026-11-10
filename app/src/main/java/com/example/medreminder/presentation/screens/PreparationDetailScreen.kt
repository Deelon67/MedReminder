package com.example.medreminder.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.medreminder.domain.model.cabinetMedicines
import com.example.medreminder.domain.model.medicineNameKey
import com.example.medreminder.presentation.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PreparationDetailScreen(
    viewModel: MainViewModel,
    medicineId: Int,
    onBackClick: () -> Unit,
    onAddSchedule: (Int) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val source = state.medicines.firstOrNull { it.id == medicineId }
    val medicine = cabinetMedicines(state.medicines).firstOrNull {
        it.id == medicineId || (source != null && medicineNameKey(it.name) == medicineNameKey(source.name))
    }
    var editing by rememberSaveable { mutableStateOf(false) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    MaterialTheme(colorScheme = DashboardColors) {
        Dialog(
            onDismissRequest = onBackClick,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(Modifier.fillMaxSize(), color = Background) {
                Column(
                    Modifier.safeDrawingPadding().widthIn(max = 600.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Карточка препарата", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, "Закрыть") }
                    }
                    if (medicine == null) {
                        Text(if (state.isLoading) "Загрузка…" else "Препарат не найден")
                    } else {
                        DashboardPanel(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                PreparationDetailField("Название препарата", medicine.name)
                                PreparationDetailField("Форма выпуска", medicine.form)
                                PreparationDetailField("Дозировка на упаковке", medicine.dosage)
                                PreparationDetailField("Количество в запасе", medicine.quantity)
                                PreparationDetailField("Срок годности",
                                    medicine.expiryEpochDay?.let { LocalDate.ofEpochDay(it).format(dateFormat) }.orEmpty())
                                if (medicine.expiryEpochDay?.let { LocalDate.ofEpochDay(it).isBefore(LocalDate.now()) } == true) {
                                    Text("Срок годности истёк", color = MaterialTheme.colorScheme.error)
                                }
                                PreparationDetailField("Заметка", medicine.comment)
                            }
                        }
                        Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp)) {
                            Text("Дополнить сведения")
                        }
                        Text("Схемы приёма", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val schedules = state.medicines.filter {
                            !it.cabinetOnly && (it.preparationId == medicine.id ||
                                medicineNameKey(it.name) == medicineNameKey(medicine.name))
                        }
                        if (schedules.isEmpty()) Text("Схем приёма пока нет", color = Muted)
                        schedules.forEach { schedule ->
                            DashboardPanel(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("На один приём: ${schedule.dosage}", fontWeight = FontWeight.Bold)
                                    Text(schedule.times.distinct().sorted().joinToString(" · ") { formatDoseTime(it) })
                                    val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                                        .filterIndexed { index, _ -> schedule.weekdaysMask and (1 shl index) != 0 }
                                    Text(days.joinToString(" · "), color = Muted)
                                    schedule.startEpochDay?.let {
                                        Text("Начало: " + LocalDate.ofEpochDay(it).format(dateFormat), color = Muted)
                                    }
                                    Text(schedule.courseDays?.let { "Курс: $it дней" } ?: "Без ограничения курса", color = Muted)
                                    if (schedule.comment.isNotBlank()) Text(schedule.comment, color = Muted)
                                }
                            }
                        }
                        OutlinedButton(onClick = { onAddSchedule(medicine.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Добавить схему приёма")
                        }
                    }
                }
            }
        }
        if (editing && medicine != null) {
            AddPreparationScreen(
                viewModel = viewModel,
                onBackClick = { editing = false },
                initialMedicine = medicine
            )
        }
    }
}

@Composable
private fun PreparationDetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
        Text(value.ifBlank { "Не указано" }, color = if (value.isBlank()) Muted else Ink,
            style = MaterialTheme.typography.bodyLarge)
    }
}

package com.example.medreminder.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.medreminder.domain.model.*
import com.example.medreminder.presentation.viewmodel.MainState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun MeasurementsSection(
    state: MainState,
    now: ZonedDateTime,
    onSave: suspend (MeasurementType, String, String, String) -> Boolean,
    onClearError: () -> Unit
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru")) }
    if (state.measurementsLoading) {
        CircularProgressIndicator()
    } else if (state.measurementsError != null) {
        Text(state.measurementsError, color = MaterialTheme.colorScheme.error)
    } else if (state.measurements.isEmpty()) {
        DashboardPanel(Modifier.fillMaxWidth()) {
            Text("Пока нет измерений", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Добавляйте давление, пульс, глюкозу, вес и температуру. Все значения и время измерения будут храниться в вашем дневнике.",
                color = Muted)
        }
    }
    Button(
        onClick = { onClearError(); adding = true },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) { Text("Добавить измерение") }

    if (state.measurements.isNotEmpty()) {
        Text("История показателей", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        state.measurements.forEach { measurement ->
            key(measurement.id) {
                DashboardPanel(Modifier.fillMaxWidth()) {
                    Text(measurement.type.title, fontWeight = FontWeight.Bold, color = Ink)
                    Text(measurement.displayValue(), style = MaterialTheme.typography.titleLarge, color = Accent)
                    measurement.pulse?.let { Text("Пульс: $it уд/мин", color = Muted) }
                    Text(Instant.ofEpochMilli(measurement.measuredAt).atZone(now.zone).format(formatter),
                        style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
    }
    if (adding) {
        MeasurementDialog(state, now, onDismiss = { adding = false }, onSave = onSave)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeasurementDialog(
    state: MainState,
    now: ZonedDateTime,
    onDismiss: () -> Unit,
    onSave: suspend (MeasurementType, String, String, String) -> Boolean
) {
    var selected by rememberSaveable { mutableStateOf(MeasurementType.PRESSURE.name) }
    val type = MeasurementType.valueOf(selected)
    var first by rememberSaveable(selected) { mutableStateOf("") }
    var second by rememberSaveable(selected) { mutableStateOf("") }
    var pulse by rememberSaveable(selected) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val valid = parseMeasurement(type, first, second, pulse, now.toInstant().toEpochMilli()) != null
    val pressure = type == MeasurementType.PRESSURE
    AlertDialog(
        containerColor = Background,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        onDismissRequest = { if (!state.isSavingMeasurement) onDismiss() },
        title = { Text("Запись показателя") },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MeasurementType.entries.forEach { option ->
                        SelectionChip(option.title, type == option,
                            onClick = { selected = option.name }, enabled = !state.isSavingMeasurement)
                    }
                }
                if (pressure) {
                    Text("Артериальное давление · мм рт. ст.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MeasurementNumberField(first, { first = it }, "Верхнее", Modifier.weight(1f),
                            enabled = !state.isSavingMeasurement, integer = true)
                        MeasurementNumberField(second, { second = it }, "Нижнее", Modifier.weight(1f),
                            enabled = !state.isSavingMeasurement, integer = true)
                    }
                    MeasurementNumberField(pulse, { pulse = it }, "Пульс, уд/мин (необязательно)",
                        enabled = !state.isSavingMeasurement, integer = true)
                    if (first.isNotBlank() && second.isNotBlank() &&
                        (first.toIntOrNull() ?: 0) <= (second.toIntOrNull() ?: 0)) {
                        Text("Верхнее значение должно быть больше нижнего.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    MeasurementNumberField(first, { first = it }, "${type.title}, ${type.unit}",
                        enabled = !state.isSavingMeasurement, integer = type == MeasurementType.PULSE)
                }
                Text("Время записи: " + now.format(DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.forLanguageTag("ru"))),
                    style = MaterialTheme.typography.bodySmall, color = Muted)
                state.measurementSaveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !state.isSavingMeasurement, onClick = {
                scope.launch { if (onSave(type, first, second, pulse)) onDismiss() }
            }) { Text(if (state.isSavingMeasurement) "Сохранение…" else "Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSavingMeasurement) { Text("Отмена") }
        }
    )
}

@Composable
private fun MeasurementNumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    integer: Boolean = false
) {
    OutlinedTextField(value = value, onValueChange = { input ->
        val pattern = if (integer) Regex("[0-9]*") else Regex("[0-9]*([.,][0-9]*)?")
        if (input.length <= 9 && input.matches(pattern)) onChange(input)
    }, label = { Text(label) }, modifier = modifier.fillMaxWidth(),
        singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal))
}

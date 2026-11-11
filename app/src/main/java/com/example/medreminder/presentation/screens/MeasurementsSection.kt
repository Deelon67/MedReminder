package com.example.medreminder.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
    onClearError: () -> Unit,
    onDelete: suspend (Long) -> Boolean
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var pendingRemovalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var removalError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
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
                    val reference = measurement.reference()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(measurement.type.title, fontWeight = FontWeight.Bold, color = Ink,
                            modifier = Modifier.weight(1f))
                        MeasurementStatusBadge(reference.level, measurement.type == MeasurementType.GLUCOSE)
                    }
                    Row(Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(measurement.displayValue(), style = MaterialTheme.typography.bodyLarge,
                            color = Accent, modifier = Modifier.weight(1f))
                        IconButton(onClick = { removalError = null; pendingRemovalId = measurement.id },
                            enabled = !deleting) {
                            Icon(Icons.Default.Delete, "Удалить показатель", tint = Muted)
                        }
                    }
                    measurement.pulse?.let { Text("Пульс: $it уд/мин", color = Muted) }
                    Text(Instant.ofEpochMilli(measurement.measuredAt).atZone(now.zone).format(formatter),
                        style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text(reference.hint, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
    }
    val removalTarget = state.measurements.firstOrNull { it.id == pendingRemovalId }
    if (removalTarget != null) {
        val dismissRemoval = { if (!deleting) { pendingRemovalId = null; removalError = null } }
        AlertDialog(
            onDismissRequest = dismissRemoval,
            title = { Text("Удалить показатель?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val recordedAt = Instant.ofEpochMilli(removalTarget.measuredAt).atZone(now.zone).format(formatter)
                    Text("Удалить запись «${removalTarget.type.title}: ${removalTarget.displayValue()}» от $recordedAt? Восстановить её будет нельзя.")
                    removalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        deleting = true
                        scope.launch {
                            try {
                                if (onDelete(removalTarget.id)) {
                                    pendingRemovalId = null
                                    removalError = null
                                } else {
                                    removalError = "Не удалось удалить показатель. Попробуйте ещё раз."
                                }
                            } finally {
                                deleting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(if (deleting) "Удаление…" else "Удалить") }
            },
            dismissButton = { TextButton(onClick = dismissRemoval, enabled = !deleting) { Text("Отмена") } }
        )
    }
    if (adding) {
        MeasurementDialog(state, now, onDismiss = { adding = false }, onSave = onSave)
    }
}

@Composable
private fun MeasurementStatusBadge(level: MeasurementLevel, fasting: Boolean) {
    val (background, foreground) = when (level) {
        MeasurementLevel.NORMAL -> androidx.compose.ui.graphics.Color(0xFFDCEEE0) to androidx.compose.ui.graphics.Color(0xFF285C3A)
        MeasurementLevel.ELEVATED -> androidx.compose.ui.graphics.Color(0xFFFFF0C7) to androidx.compose.ui.graphics.Color(0xFF795A13)
        MeasurementLevel.OUTSIDE -> androidx.compose.ui.graphics.Color(0xFFFBE4E1) to androidx.compose.ui.graphics.Color(0xFF9C3634)
        MeasurementLevel.UNKNOWN -> androidx.compose.ui.graphics.Color(0xFFEEF0F2) to Muted
    }
    Surface(color = background, shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)) {
        Text(if (fasting) "Натощак: ${level.title.lowercase(Locale.forLanguageTag("ru"))}" else level.title,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = foreground)
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

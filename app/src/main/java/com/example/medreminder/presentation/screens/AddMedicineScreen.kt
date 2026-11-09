package com.example.medreminder.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.medreminder.domain.TimeUtils
import com.example.medreminder.domain.model.Medicine
import com.example.medreminder.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(viewModel: MainViewModel, onBackClick: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable { mutableStateOf("7") }
    var start by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var weekdays by rememberSaveable { mutableIntStateOf(127) }
    var times by rememberSaveable { mutableStateOf(listOf(480, 720, 1080)) }
    var comment by rememberSaveable { mutableStateOf("") }
    var timeEditor by rememberSaveable { mutableStateOf<Int?>(null) }
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val days = course.toIntOrNull()
    val valid = name.isNotBlank() && dosage.isNotBlank() && days != null &&
        days in 1..3650 && weekdays != 0 && times.isNotEmpty()
    LaunchedEffect(Unit) { viewModel.clearSaveError() }

    MaterialTheme(colorScheme = DashboardColors) {
        Dialog(
            onDismissRequest = { if (!state.isSaving) onBackClick() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
                dismissOnBackPress = !state.isSaving, dismissOnClickOutside = !state.isSaving)
        ) {
            BoxWithConstraints(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().heightIn(max = maxHeight),
                    shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = SoftAccent, shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Новая схема", style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = onBackClick, enabled = !state.isSaving) {
                                        Icon(Icons.Default.Close, "Закрыть")
                                    }
                                }
                                Text("Сначала задайте что принимать, затем добавьте время и длительность курса. Комментарий оставьте как дополнительную подсказку.",
                                    color = Muted)
                            }
                        }
                        OutlinedTextField(name, { name = it }, label = { Text("Название") },
                            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving)
                        OutlinedTextField(dosage, { dosage = it }, label = { Text("Дозировка") },
                            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving)
                        OutlinedTextField(course, { if (it.length <= 4 && it.all(Char::isDigit)) course = it },
                            label = { Text("Курс") }, supportingText = { Text("Дней лечения · от 1 до 3650") },
                            isError = course.isNotEmpty() && (days == null || days !in 1..3650),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(7, 14, 30).forEach { count ->
                                FilterChip(selected = days == count, onClick = { course = count.toString() },
                                    label = { Text("$count дней") }, enabled = !state.isSaving,
                                    leadingIcon = if (days == count) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val date = LocalDate.ofEpochDay(start)
                                DatePickerDialog(context, { _, y, m, d ->
                                    start = LocalDate.of(y, m + 1, d).toEpochDay()
                                }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                            }, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving
                        ) {
                            Icon(Icons.Default.DateRange, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Начало: " + LocalDate.ofEpochDay(start).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                        }
                        Text("Дни приёма", fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 4) {
                            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { index, label ->
                                val bit = 1 shl index
                                val checked = weekdays and bit != 0
                                FilterChip(selected = checked, onClick = { weekdays = weekdays xor bit },
                                    enabled = !state.isSaving, label = { Text(label) },
                                    leadingIcon = if (checked) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null)
                            }
                        }
                        if (weekdays == 0) Text("Выберите хотя бы один день.", color = MaterialTheme.colorScheme.error)
                        Text("Время приёма", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { timeEditor = -1 }, enabled = !state.isSaving) { Text("Добавить") }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            times.sorted().forEach { time ->
                                InputChip(
                                    selected = false, onClick = { timeEditor = time }, enabled = !state.isSaving,
                                    label = { Text(formatDoseTime(time)) },
                                    trailingIcon = {
                                        IconButton(onClick = { times = times - time }, enabled = !state.isSaving,
                                            modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, "Удалить время ${formatDoseTime(time)}", Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                        Text(if (times.isEmpty()) "Добавьте хотя бы одно время." else "Нажмите на время, чтобы изменить его.",
                            color = if (times.isEmpty()) MaterialTheme.colorScheme.error else Muted)
                        OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий") },
                            minLines = 2, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving)
                        state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(
                            enabled = valid && !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(18.dp),
                            onClick = {
                                val sorted = times.distinct().sorted()
                                val first = sorted.first()
                                val medicine = Medicine(0, name.trim(), dosage.trim(),
                                    TimeUtils.getTimeOfDayFromHour(first / 60), first / 60, first % 60,
                                    startEpochDay = start, courseDays = days, weekdaysMask = weekdays,
                                    times = sorted, comment = comment.trim())
                                scope.launch { if (viewModel.saveMedicine(medicine)) onBackClick() }
                            }
                        ) { Text(if (state.isSaving) "Сохранение…" else "Сохранить") }
                    }
                }
            }
        }
        timeEditor?.let { original ->
            key(original) {
                MedicineTimePicker(if (original < 0) 480 else original,
                    onDismiss = { timeEditor = null },
                    onConfirm = { chosen ->
                        times = (times.filter { it != original } + chosen).distinct().sorted()
                        timeEditor = null
                    })
            }
        }
    }
}

internal fun formatDoseTime(minutes: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicineTimePicker(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val time = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
    val configuration = LocalConfiguration.current
    var keyboard by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 400.dp).fillMaxWidth(),
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Выберите время", modifier = Modifier.fillMaxWidth())
                if (keyboard || configuration.screenHeightDp < 500 || configuration.screenWidthDp < 360) {
                    TimeInput(state = time)
                } else {
                    TimePicker(state = time)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { keyboard = !keyboard }) {
                        Icon(if (keyboard) Icons.Default.DateRange else Icons.Default.Edit,
                            if (keyboard) "Выбрать на часах" else "Ввести время цифрами")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = { onConfirm(time.hour * 60 + time.minute) }) { Text("ОК") }
                }
            }
        }
    }
}

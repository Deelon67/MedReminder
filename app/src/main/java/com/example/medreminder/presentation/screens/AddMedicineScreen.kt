package com.example.medreminder.presentation.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.medreminder.DayRoutinePreferences
import com.example.medreminder.RoutineMoment
import com.example.medreminder.mealTimes
import com.example.medreminder.domain.TimeUtils
import com.example.medreminder.domain.model.Medicine
import com.example.medreminder.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(viewModel: MainViewModel, onBackClick: () -> Unit, initialPreparationId: Int? = null) {
    var preparationId by rememberSaveable { mutableStateOf(initialPreparationId) }
    var addingPreparation by rememberSaveable { mutableStateOf(false) }
    var focusedField by remember { mutableStateOf<String?>(null) }
    var selectedSuggestion by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var chosenDoseUnit by rememberSaveable { mutableStateOf<String?>(null) }
    var course by rememberSaveable { mutableStateOf("7") }
    var start by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var weekdays by rememberSaveable { mutableIntStateOf(0) }
    var customTimes by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var times by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var comment by rememberSaveable { mutableStateOf("") }
    var timeEditor by rememberSaveable { mutableStateOf<Int?>(null) }
    val state by viewModel.state.collectAsState()
    val cabinet = com.example.medreminder.domain.model.cabinetMedicines(state.medicines)
    val preparation = cabinet.firstOrNull { it.id == preparationId }
        ?: cabinet.firstOrNull {
            com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                com.example.medreminder.domain.model.medicineNameKey(name)
        }
    LaunchedEffect(initialPreparationId, cabinet.map { it.id }) {
        if (name.isBlank()) cabinet.firstOrNull { it.id == initialPreparationId }?.let {
            name = com.example.medreminder.domain.model.capitalizeMedicineName(it.name)
            selectedSuggestion = name
        }
    }
    val doseUnit = chosenDoseUnit ?: preparation?.form?.let(::scheduleDoseUnit).orEmpty()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val routinePreferences = remember(context.applicationContext) { DayRoutinePreferences(context) }
    val routineFlow = remember(routinePreferences) { routinePreferences.observe() }
    val routine by routineFlow.collectAsState(initial = routinePreferences.read())
    val availableTimes = (routine.mealTimes() + customTimes + times).distinct().sorted()
    val days = course.toIntOrNull()
    val valid = name.isNotBlank() && dosage.isNotBlank() && doseUnit.isNotBlank() &&
        (dosage.toLongOrNull()?.let { it > 0 } == true) && days != null &&
        days in 1..365 && weekdays != 0 && times.isNotEmpty() &&
        start >= LocalDate.now().toEpochDay()
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
                                Text("Сначала введите название препарата, укажите количество на один приём и выберите единицу измерения. Затем задайте длительность курса, дату его начала и время приёма. Комментарий оставьте как дополнительную подсказку.",
                                    color = Muted)
                            }
                        }
                        OutlinedTextField(name, { name = com.example.medreminder.domain.model.capitalizeMedicineName(it); selectedSuggestion = null; preparationId = null; chosenDoseUnit = null }, label = { Text("Название препарата") },
                            placeholder = { Text("Например, Парацетамол") },
                            supportingText = if (focusedField == "name") {
                                { Text("Введите хотя бы одну букву, чтобы найти препарат в аптечке, или укажите новое название.") }
                            } else null,
                            singleLine = true, modifier = Modifier.fillMaxWidth().onFocusChanged {
                                if (it.isFocused) focusedField = "name"
                                else if (focusedField == "name") focusedField = null
                            }, enabled = !state.isSaving)
                        if (focusedField == "name" && name.isNotBlank() && selectedSuggestion != name &&
                            cabinet.none {
                                com.example.medreminder.domain.model.medicineNameKey(it.name) ==
                                    com.example.medreminder.domain.model.medicineNameKey(name)
                            }
                        ) {
                            val query = com.example.medreminder.domain.model.medicineNameKey(name)
                            val suggestions = com.example.medreminder.domain.model.cabinetMedicines(state.medicines)
                                .filter { com.example.medreminder.domain.model.medicineNameKey(it.name).contains(query) }
                            if (suggestions.isNotEmpty()) {
                                Text("Из аптечки", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelMedium, color = Muted)
                                suggestions.forEach { medicine ->
                                    TextButton(
                                        onClick = {
                                            name = com.example.medreminder.domain.model.capitalizeMedicineName(medicine.name)
                                            selectedSuggestion = name
                                            preparationId = medicine.id
                                            chosenDoseUnit = null
                                            dosage = ""
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                        enabled = !state.isSaving
                                    ) { Text(medicine.name, modifier = Modifier.fillMaxWidth()) }
                                }
                            } else {
                                Text("В аптечке не найдено. Препарат добавится при сохранении схемы.",
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.bodySmall, color = Muted)
                            }
                        }
                        preparation?.let { medicine ->
                            Surface(color = SoftAccent, shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Препарат из аптечки", fontWeight = FontWeight.Bold)
                                    if (medicine.form.isNotBlank()) Text("Форма: ${medicine.form}")
                                    if (medicine.dosage.isNotBlank()) Text("На упаковке: ${medicine.dosage}")
                                    Text(if (medicine.quantity.isBlank()) "Запас не указан" else "В запасе: ${medicine.quantity}")
                                }
                            }
                        }
                        if (preparation == null && name.isNotBlank()) {
                            TextButton(
                                onClick = { addingPreparation = true },
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text("Заполнить карточку препарата", modifier = Modifier.fillMaxWidth())
                            }
                        }
                        OutlinedTextField(dosage, { value ->
                            if (value.all { it in '0'..'9' }) dosage = value
                        }, label = { Text("Количество на один приём") },
                            suffix = { Text(doseUnit) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Например, 1") },
                            supportingText = if (focusedField == "dosage") {
                                { Text("Введите число и выберите единицу приёма ниже.") }
                            } else null,
                            singleLine = true, modifier = Modifier.fillMaxWidth().onFocusChanged {
                                if (it.isFocused) focusedField = "dosage"
                                else if (focusedField == "dosage") focusedField = null
                            }, enabled = !state.isSaving)
                        Text("Единица приёма", fontWeight = FontWeight.Bold)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("табл.", "капс.", "кап.", "мл", "мг", "г", "шт.", "впрыск.").forEach { unit ->
                                SelectionChip(
                                    text = when (unit) {
                                        "капс." -> "капсулы"
                                        "кап." -> "капли"
                                        "впрыск." -> "впрыск"
                                        else -> unit
                                    },
                                    selected = doseUnit == unit,
                                    onClick = { chosenDoseUnit = unit },
                                    enabled = !state.isSaving
                                )
                            }
                        }
                        Text("Курс", fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("7" to "7 дней", "14" to "14 дней", "30" to "30 дней")
                                .forEach { (option, label) ->
                                    SelectionChip(label, course == option,
                                        onClick = { course = option }, enabled = !state.isSaving)
                                }
                        }
                        OutlinedButton(
                            onClick = {
                                val today = LocalDate.now()
                                val date = LocalDate.ofEpochDay(maxOf(start, today.toEpochDay()))
                                DatePickerDialog(context, { _, y, m, d ->
                                    start = maxOf(LocalDate.of(y, m + 1, d).toEpochDay(), LocalDate.now().toEpochDay())
                                }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
                                    datePicker.minDate = today.atStartOfDay(java.time.ZoneId.systemDefault())
                                        .toInstant().toEpochMilli()
                                }.show()
                            }, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving
                        ) {
                            Icon(Icons.Default.DateRange, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Начало: " + LocalDate.ofEpochDay(start).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                        }
                        if (start < LocalDate.now().toEpochDay()) {
                            Text("Выберите сегодня или будущую дату начала.",
                                color = MaterialTheme.colorScheme.error)
                        }
                        Text("Дни приёма", fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 4) {
                            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { index, label ->
                                val bit = 1 shl index
                                val checked = weekdays and bit != 0
                                SelectionChip(label, checked,
                                    onClick = { weekdays = weekdays xor bit }, enabled = !state.isSaving)
                            }
                        }
                        if (weekdays == 0) Text("Выберите хотя бы один день.", color = MaterialTheme.colorScheme.error)
                        Text("Время приёма", fontWeight = FontWeight.Bold)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableTimes.sorted().forEach { time ->
                                val selected = time in times
                                SelectionChip(formatDoseTime(time), selected,
                                    onClick = { times = if (selected) times - time else (times + time).sorted() },
                                    enabled = !state.isSaving)
                            }
                            SelectionChip("Своё", false, onClick = { timeEditor = -1 }, enabled = !state.isSaving)
                        }
                        Text(if (times.isEmpty()) "Выберите время." else "Нажмите на время, чтобы выбрать или отменить приём.",
                            color = if (times.isEmpty()) MaterialTheme.colorScheme.error else Muted)
                        OutlinedTextField(comment, { comment = com.example.medreminder.domain.model.capitalizeFirstLetter(it) }, label = { Text("Комментарий") },
                            placeholder = { Text("Например, запить водой") },
                            supportingText = if (focusedField == "comment") {
                                { Text("Необязательная подсказка к приёму. Она будет видна в карточке на экране «Сегодня».") }
                            } else null,
                            minLines = 2, modifier = Modifier.fillMaxWidth().onFocusChanged {
                                if (it.isFocused) focusedField = "comment"
                                else if (focusedField == "comment") focusedField = null
                            }, enabled = !state.isSaving)
                        state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(
                            enabled = valid && !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(18.dp),
                            onClick = {
                                val sorted = times.distinct().sorted()
                                val first = sorted.first()
                                val medicine = Medicine(0, name.trim(), "${dosage.trim()} $doseUnit",
                                    TimeUtils.getTimeOfDayFromHour(first / 60), first / 60, first % 60,
                                    startEpochDay = start, courseDays = days, weekdaysMask = weekdays,
                                    times = sorted, comment = comment.trim(),
                                    preparationId = preparation?.takeIf { it.cabinetOnly }?.id)
                                scope.launch { if (viewModel.saveMedicine(medicine)) onBackClick() }
                            }
                        ) { Text(if (state.isSaving) "Сохранение…" else "Сохранить") }
                    }
                }
            }
        }
        if (addingPreparation) {
            AddPreparationScreen(
                viewModel = viewModel,
                onBackClick = { addingPreparation = false },
                initialName = name
            )
        }
        timeEditor?.let { original ->
            key(original) {
                MedicineTimePicker(if (original < 0) routine.getValue(RoutineMoment.BREAKFAST) else original,
                    onDismiss = { timeEditor = null },
                    onConfirm = { chosen ->
                        customTimes = (customTimes + chosen).distinct().sorted()
                        times = (times + chosen).distinct().sorted()
                        timeEditor = null
                    })
            }
        }
    }
}

internal fun scheduleDoseUnit(form: String): String =
    com.example.medreminder.domain.model.scheduleDoseUnit(form)

internal fun formatDoseTime(minutes: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MedicineTimePicker(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
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

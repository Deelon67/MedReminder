package com.example.medreminder.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.domain.model.Medicine
import com.example.medreminder.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun editablePreparationNumber(value: String, unit: String): String =
    value.removeSuffix(" $unit").takeIf { it.matches(Regex("[0-9]+([.,][0-9]+)?")) }.orEmpty()

internal fun preparationDosageUnit(form: String): String = when (form) {
    "Таблетки", "Капсулы", "Порошок", "Свечи", "Другое" -> "мг"
    "Сироп", "Капли", "Спрей", "Раствор" -> "мл"
    "Мазь", "Крем", "Гель" -> "г"
    else -> ""
}

internal fun preparationStockUnit(form: String, quantity: String = ""): String {
    val options = com.example.medreminder.domain.model.preparationStockUnits(form)
    return com.example.medreminder.domain.model.savedStockUnit(quantity)?.takeIf { it in options }
        ?: options.firstOrNull().orEmpty()
}

@OptIn(ExperimentalLayoutApi::class)
@android.annotation.SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AddPreparationScreen(viewModel: MainViewModel, onBackClick: () -> Unit, initialName: String = "", initialMedicine: Medicine? = null) {
    var name by rememberSaveable { mutableStateOf(com.example.medreminder.domain.model.capitalizeMedicineName(initialMedicine?.name ?: initialName)) }
    var form by rememberSaveable { mutableStateOf(initialMedicine?.form.orEmpty()) }
    var dosageEdited by rememberSaveable { mutableStateOf(false) }
    var quantityEdited by rememberSaveable { mutableStateOf(false) }
    var dosage by rememberSaveable { mutableStateOf(
        initialMedicine?.let { editablePreparationNumber(it.dosage, preparationDosageUnit(it.form)) }.orEmpty()
    ) }
    var chosenStockUnit by rememberSaveable { mutableStateOf(
        preparationStockUnit(initialMedicine?.form.orEmpty(), initialMedicine?.quantity.orEmpty())
    ) }
    var quantity by rememberSaveable { mutableStateOf(
        initialMedicine?.let { editablePreparationNumber(it.quantity, preparationStockUnit(it.form, it.quantity)) }.orEmpty()
    ) }
    var expiry by rememberSaveable { mutableStateOf(initialMedicine?.expiryEpochDay) }
    var note by rememberSaveable { mutableStateOf(com.example.medreminder.domain.model.capitalizeFirstLetter(initialMedicine?.comment.orEmpty())) }
    val dosageUnit = preparationDosageUnit(form)
    val stockUnits = com.example.medreminder.domain.model.preparationStockUnits(form)
    val stockUnit = chosenStockUnit.takeIf { it in stockUnits } ?: preparationStockUnit(form)
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.clearSaveError() }

    MaterialTheme(colorScheme = DashboardColors) {
        Dialog(
            onDismissRequest = { if (!state.isSaving) onBackClick() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = !state.isSaving,
                dismissOnClickOutside = !state.isSaving
            )
        ) {
            BoxWithConstraints(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().heightIn(max = maxHeight),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(color = SoftAccent, shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (initialMedicine == null) "Добавление препарата" else "Сведения о препарате",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = onBackClick, enabled = !state.isSaving) {
                                        Icon(Icons.Default.Close, "Закрыть")
                                    }
                                }
                                Text("Сохраните сведения с упаковки для домашней аптечки. Обязательно только название.",
                                    color = Muted)
                            }
                        }
                        OutlinedTextField(name, { name = com.example.medreminder.domain.model.capitalizeMedicineName(it) }, label = { Text("Название препарата *") },
                            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving,
                            readOnly = initialMedicine != null)
                        Text("Форма выпуска", fontWeight = FontWeight.Bold)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Таблетки", "Капсулы", "Сироп", "Капли", "Спрей",
                                "Раствор", "Мазь", "Крем", "Гель", "Порошок", "Свечи", "Другое"
                            ).forEach { option ->
                                SelectionChip(
                                    text = option,
                                    selected = form == option,
                                    enabled = !state.isSaving,
                                    onClick = {
                                        if (preparationDosageUnit(option) != dosageUnit) { dosage = ""; dosageEdited = true }
                                        if (option != form) {
                                            if (preparationStockUnit(option) != stockUnit) { quantity = ""; quantityEdited = true }
                                            chosenStockUnit = preparationStockUnit(option)
                                        }
                                        form = option
                                    }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { value -> if (value.all { it in '0'..'9' }) { dosage = value; dosageEdited = true } },
                            label = { Text("Дозировка на упаковке") },
                            suffix = { Text(dosageUnit) },
                            supportingText = {
                                Text(if (dosageUnit.isEmpty()) "Сначала выберите форму выпуска."
                                    else if (!dosageEdited && dosage.isEmpty() && !initialMedicine?.dosage.isNullOrBlank()) "Текущее значение: ${initialMedicine?.dosage}. Введите число, чтобы изменить."
                                    else "Введите только число. Единица: $dosageUnit.")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSaving && dosageUnit.isNotEmpty()
                        )
                        if (stockUnits.size > 1) {
                            Text("Единица запаса", fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                stockUnits.forEach { unit ->
                                    SelectionChip(
                                        text = if (unit == "кап.") "капли" else unit,
                                        selected = stockUnit == unit,
                                        enabled = !state.isSaving,
                                        onClick = {
                                            if (stockUnit != unit) {
                                                chosenStockUnit = unit
                                                quantity = ""
                                                quantityEdited = true
                                            }
                                        }
                                    )
                                }
                            }
                            Text("Для автоматического списания выберите ту же единицу, что и в схеме.",
                                style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { value ->
                                if (value.matches(Regex("[0-9]*([.,][0-9]*)?"))) { quantity = value; quantityEdited = true }
                            },
                            label = { Text("Количество в запасе") },
                            suffix = { Text(stockUnit) },
                            supportingText = {
                                Text(if (stockUnit.isEmpty()) "Сначала выберите форму выпуска."
                                    else if (!quantityEdited && quantity.isEmpty() && !initialMedicine?.quantity.isNullOrBlank()) "Текущее значение: ${initialMedicine?.quantity}. Введите число, чтобы изменить."
                                    else "Введите только число. Единица: $stockUnit")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSaving && stockUnit.isNotEmpty()
                        )
                        OutlinedButton(
                            onClick = {
                                val date = expiry?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
                                DatePickerDialog(context, { _, year, month, day ->
                                    expiry = LocalDate.of(year, month + 1, day).toEpochDay()
                                }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving
                        ) {
                            Text(expiry?.let {
                                "Годен до: " + LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            } ?: "Указать срок годности")
                        }
                        expiry?.let { date ->
                            if (LocalDate.ofEpochDay(date).isBefore(LocalDate.now())) {
                                Text("Срок годности истёк", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { expiry = null }, enabled = !state.isSaving) {
                                Text("Убрать срок годности")
                            }
                        }
                        OutlinedTextField(note, { note = com.example.medreminder.domain.model.capitalizeFirstLetter(it) }, label = { Text("Заметка") },
                            placeholder = { Text("Место хранения или другая информация") },
                            minLines = 2, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving)
                        state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(
                            enabled = name.isNotBlank() && !state.isSaving &&
                                (!quantityEdited || quantity.isBlank() || quantity.matches(Regex("[0-9]+([.,][0-9]+)?"))),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(18.dp),
                            onClick = {
                                scope.launch {
                                    val preparation = Medicine(
                                        id = initialMedicine?.takeIf { it.cabinetOnly }?.id ?: 0, name = name.trim(),
                                        dosage = if (initialMedicine != null && !dosageEdited) initialMedicine.dosage
                                            else if (dosage.isBlank()) "" else "$dosage $dosageUnit",
                                        timeOfDay = TimeOfDay.MORNING, hour = 0, minute = 0,
                                        times = emptyList(), weekdaysMask = 0, comment = note.trim(),
                                        cabinetOnly = true, form = form.trim(),
                                        quantity = if (initialMedicine != null && !quantityEdited) initialMedicine.quantity
                                            else if (quantity.isBlank()) "" else "$quantity $stockUnit",
                                        expiryEpochDay = expiry
                                    )
                                    if (viewModel.savePreparation(preparation, updateQuantity = initialMedicine == null || quantityEdited)) onBackClick()
                                }
                            }
                        ) { Text(if (state.isSaving) "Сохранение…" else if (initialMedicine == null) "Сохранить в аптечку" else "Сохранить изменения") }
                    }
                }
            }
        }
    }
}

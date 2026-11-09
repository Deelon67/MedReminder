package com.example.medreminder.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.domain.TimeUtils
import com.example.medreminder.domain.model.Medicine
import com.example.medreminder.presentation.viewmodel.MainIntent
import com.example.medreminder.presentation.viewmodel.MainViewModel

// Экран добавления лекарства
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    // Состояния полей формы
    var medicineName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedTimeOfDay by remember { mutableStateOf(TimeOfDay.MORNING) }

    // Храним время как String для корректного редактирования
    var hourText by remember { mutableStateOf("8") }
    var minuteText by remember { mutableStateOf("00") }

    // Функция для установки времени по умолчанию при выборе периода (пресет)
    fun setTimeForPeriod(timeOfDay: TimeOfDay) {
        selectedTimeOfDay = timeOfDay
        when (timeOfDay) {
            TimeOfDay.MORNING -> hourText = "8"
            TimeOfDay.AFTERNOON -> hourText = "12"
            TimeOfDay.EVENING -> hourText = "18"
        }
        minuteText = "00"
    }

    // Функция для обновления периода на основе введенного часа
    fun updatePeriodFromHour(hour: Int) {
        selectedTimeOfDay = TimeUtils.getTimeOfDayFromHour(hour)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить лекарство") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Поле названия
            OutlinedTextField(
                value = medicineName,
                onValueChange = { medicineName = it },
                label = { Text("Название лекарства") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Поле дозировки
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Дозировка") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Выбор времени приема (пресеты)
            Text(
                text = "Быстрый выбор времени",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeOfDayButton(
                    text = "Утро",
                    isSelected = selectedTimeOfDay == TimeOfDay.MORNING,
                    onClick = { setTimeForPeriod(TimeOfDay.MORNING) }
                )
                TimeOfDayButton(
                    text = "День",
                    isSelected = selectedTimeOfDay == TimeOfDay.AFTERNOON,
                    onClick = { setTimeForPeriod(TimeOfDay.AFTERNOON) }
                )
                TimeOfDayButton(
                    text = "Вечер",
                    isSelected = selectedTimeOfDay == TimeOfDay.EVENING,
                    onClick = { setTimeForPeriod(TimeOfDay.EVENING) }
                )
            }

            // Выбор точного времени
            Text(
                text = "Точное время",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Часы
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { newValue ->
                        // Разрешаем пустую строку (для стирания) или число от 0 до 23
                        if (newValue.isEmpty() || (newValue.toIntOrNull() ?: -1) in 0..23) {
                            hourText = newValue
                            // АВТОМАТИЧЕСКИ обновляем период при изменении часа
                            val hour = newValue.toIntOrNull()
                            if (hour != null) {
                                updatePeriodFromHour(hour)
                            }
                        }
                    },
                    label = { Text("Часы (0-23)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Минуты
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { newValue ->
                        // Разрешаем пустую строку или число от 0 до 59
                        if (newValue.isEmpty() || (newValue.toIntOrNull() ?: -1) in 0..59) {
                            minuteText = newValue
                        }
                    },
                    label = { Text("Минуты (0-59)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

//            // Подсказка о выбранном периоде
//            Text(
//                text = "Выбранный период: ${TimeUtils.formatTimeOfDay(selectedTimeOfDay)}",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.primary
//            )

            Spacer(modifier = Modifier.weight(1f))

            // Кнопка сохранения
            Button(
                onClick = {
                    val hour = hourText.toIntOrNull() ?: 0
                    val minute = minuteText.toIntOrNull() ?: 0

                    if (medicineName.isNotBlank() && dosage.isNotBlank()) {
                        // Определяем период на основе введенного времени
                        val finalTimeOfDay = TimeUtils.getTimeOfDayFromHour(hour)

                        val medicine = Medicine(
                            id = 0, // ID сгенерируется автоматически
                            name = medicineName,
                            dosage = dosage,
                            timeOfDay = finalTimeOfDay, // Используем автоматически определенный период
                            hour = hour,
                            minute = minute
                        )
                        viewModel.processIntent(MainIntent.AddMedicine(medicine))
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = medicineName.isNotBlank() &&
                        dosage.isNotBlank() &&
                        hourText.isNotBlank() &&
                        minuteText.isNotBlank()
            ) {
                Text("Сохранить")
            }
        }
    }
}

// Компонент кнопки выбора времени приема
@Composable
private fun TimeOfDayButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) }
    )
}
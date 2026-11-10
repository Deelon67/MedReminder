package com.example.medreminder.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medreminder.DoseReminderManager
import com.example.medreminder.domain.model.*
import com.example.medreminder.presentation.viewmodel.MainState
import com.example.medreminder.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
internal fun TodayDoseCard(
    medicine: Medicine, date: LocalDate, state: MainState, viewModel: MainViewModel, onHide: () -> Unit
) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(30_000)
        }
    }
    val times = medicine.times.distinct().sorted()
    val records = state.doses.filter { it.medicineId == medicine.id && it.epochDay == date.toEpochDay() }
    val visualStates = times.associateWith { minutes ->
        val record = records.firstOrNull { it.minutes == minutes }
        doseVisualState(
            date.atTime(minutes / 60, minutes % 60).atZone(now.zone).toInstant().toEpochMilli(),
            now.toInstant().toEpochMilli(), record?.takenAt, record?.skippedAt, record?.snoozedUntil
        )
    }
    val cardState = overallDoseVisualState(visualStates.values.toList())
    val neutral = MaterialTheme.colorScheme.surface
    val cardBackground by animateColorAsState(
        lerp(neutral, doseBackground(cardState, neutral), 0.4f), label = "Dose card status"
    )
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp,
            if (cardState == DoseVisualState.UPCOMING) Outline else doseForeground(cardState).copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(medicine.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onHide) {
                    Icon(Icons.Default.Delete, "Убрать приёмы на этот день", tint = Muted)
                }
            }
            Text("На один приём: ${medicine.dosage}", style = MaterialTheme.typography.bodyMedium, color = Muted)
            if (medicine.comment.isNotBlank()) Text(medicine.comment, style = MaterialTheme.typography.bodySmall, color = Muted)

            times.forEachIndexed { index, minutes ->
                key(date.toEpochDay(), minutes) {
                    if (index > 0) HorizontalDivider(color = Outline.copy(alpha = 0.6f))
                    val record = records.firstOrNull { it.minutes == minutes }
                    val taken = record?.takenAt != null
                    val skipped = record?.skippedAt != null
                    val snoozed = record?.snoozedUntil?.let { it > now.toInstant().toEpochMilli() } == true
                    val busy = DoseReminderManager.key(medicine.id, date.toEpochDay(), minutes) in state.updatingDoses
                    val due = date.atTime(minutes / 60, minutes % 60).atZone(now.zone)
                    val visualState = visualStates.getValue(minutes)
                    val statusColor = doseForeground(visualState)
                    val label = when {
                        skipped -> "Пропущено"
                        taken -> "Принято в " + Instant.ofEpochMilli(record!!.takenAt!!).atZone(now.zone)
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        snoozed -> {
                            val reminder = Instant.ofEpochMilli(record!!.snoozedUntil!!).atZone(now.zone)
                            "Напомним " + reminder.format(DateTimeFormatter.ofPattern(
                                if (reminder.toLocalDate() == now.toLocalDate()) "в HH:mm" else "dd.MM в HH:mm"))
                        }
                        due.isAfter(now) && ReminderPolicy.canMarkTaken(
                            due.toInstant().toEpochMilli(), now.toInstant().toEpochMilli()
                        ) -> "Приём через ${((Duration.between(now, due).seconds + 59) / 60).coerceAtLeast(1)} мин"
                        due.isAfter(now) -> "По расписанию"
                        visualState == DoseVisualState.MISSED -> "Больше часа без отметки"
                        else -> "Ожидает отметки"
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = doseBackground(visualState, neutral)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(formatDoseTime(minutes), fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall, color = statusColor)
                                val icon = when (visualState) {
                                    DoseVisualState.TAKEN -> Icons.Default.Check
                                    DoseVisualState.WAITING -> Icons.Default.Info
                                    DoseVisualState.MISSED -> Icons.Default.Warning
                                    DoseVisualState.UPCOMING -> null
                                }
                                icon?.let { Icon(it, null, Modifier.size(16.dp), tint = statusColor) }
                                Text(label, style = MaterialTheme.typography.bodySmall,
                                    color = statusColor, modifier = Modifier.weight(1f))
                            }
                        }
                        if (!taken && !skipped &&
                            ReminderPolicy.canMarkTaken(due.toInstant().toEpochMilli(), now.toInstant().toEpochMilli())) {
                            DoseActionRow(
                                busy = busy,
                                early = due.isAfter(now),
                                canSkip = !due.isAfter(now),
                                onSkip = { viewModel.skipDose(medicine.id, date.toEpochDay(), minutes) },
                                onTake = { viewModel.takeDose(medicine.id, date.toEpochDay(), minutes) },
                                canSnooze = date == now.toLocalDate() && !due.isAfter(now),
                                onSnooze = { viewModel.snoozeDose(medicine.id, date.toEpochDay(), minutes, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun doseBackground(state: DoseVisualState, neutral: Color): Color = when (state) {
    DoseVisualState.TAKEN -> Color(0xFFEAF5EE)
    DoseVisualState.WAITING -> Color(0xFFFFF3D4)
    DoseVisualState.MISSED -> Color(0xFFFCECE9)
    DoseVisualState.UPCOMING -> neutral
}

private fun doseForeground(state: DoseVisualState): Color = when (state) {
    DoseVisualState.TAKEN -> Color(0xFF285C3A)
    DoseVisualState.WAITING -> Color(0xFF795A13)
    DoseVisualState.MISSED -> Color(0xFF9C3634)
    DoseVisualState.UPCOMING -> Muted
}

@Composable
private fun DoseActionButton(text: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Ink.copy(alpha = 0.22f)),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Ink)
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DoseActionRow(
    busy: Boolean, early: Boolean, canSkip: Boolean, onSkip: () -> Unit,
    onTake: () -> Unit, canSnooze: Boolean, onSnooze: (Int) -> Unit
) {
    var snoozeMenu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DoseActionButton(
            text = if (early) "Уже принято" else "Принято",
            onClick = onTake, enabled = !busy, modifier = Modifier.weight(1f)
        )
        if (canSnooze) {
            Box(Modifier.weight(1f)) {
                DoseActionButton(
                    text = "Отложить", onClick = { snoozeMenu = true },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = snoozeMenu, onDismissRequest = { snoozeMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    listOf(15, 30, 60).forEach { minutes ->
                        DropdownMenuItem(text = { Text("Через $minutes мин") }, onClick = {
                            snoozeMenu = false
                            onSnooze(minutes)
                        })
                    }
                }
            }
        }
        if (canSkip) {
            DoseActionButton(
                text = "Пропустить", onClick = onSkip,
                enabled = !busy, modifier = Modifier.weight(1f)
            )
        }
    }
}

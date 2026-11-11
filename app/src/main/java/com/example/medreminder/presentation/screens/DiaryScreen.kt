package com.example.medreminder.presentation.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medreminder.domain.model.*
import com.example.medreminder.presentation.viewmodel.MainState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun DiaryScreen(modifier: Modifier, state: MainState, viewModel: com.example.medreminder.presentation.viewmodel.MainViewModel) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { now = ZonedDateTime.now(); delay(15_000) }
    }
    val statistics = remember(state.medicines, state.doses, now) {
        diaryStatistics(state.medicines, state.doses.map {
            DiaryMark(it.medicineId, it.epochDay, it.minutes, it.takenAt != null, it.skippedAt != null)
        }, now)
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val today = now.toLocalDate()
    val date = LocalDate.ofEpochDay((selectedDay ?: today.toEpochDay())
        .coerceIn(today.withDayOfMonth(1).toEpochDay(), today.toEpochDay()))
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru")) }
    val chartLayoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Дневник")
        DiaryTabs(selected = tab, onSelect = { tab = it })
        if (tab == 1) {
            MeasurementsSection(state, now, viewModel::saveMeasurement, viewModel::clearMeasurementError,
                viewModel::deleteMeasurement)
        } else if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            DashboardPanel(Modifier.fillMaxWidth()) {
                statistics.percent?.let { percent ->
                    Text("$percent%", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Text("Соблюдение за текущий месяц", color = Muted)
                Text("Принято ${statistics.taken} из ${statistics.due} наступивших приёмов", color = Muted,
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("1", modifier = Modifier.align(Alignment.CenterStart), color = Muted,
                        style = MaterialTheme.typography.labelMedium)
                    Text(
                        capitalizeFirstLetter(today.month.getDisplayName(
                            java.time.format.TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"))),
                        modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold
                    )
                    Text(today.lengthOfMonth().toString(), modifier = Modifier.align(Alignment.CenterEnd),
                        color = Muted, style = MaterialTheme.typography.labelMedium)
                }
                Row(Modifier.fillMaxWidth().height(100.dp)
                    .pointerInput(today, chartLayoutDirection) {
                        val gap = 3.dp.toPx()
                        fun selectDay(x: Float) {
                            val width = size.width.toFloat()
                            if (width <= 0f) return
                            val position = if (chartLayoutDirection == LayoutDirection.Rtl) width - x else x
                            val step = (width + gap) / today.lengthOfMonth()
                            val day = ((position + gap / 2) / step).toInt() + 1
                            selectedDay = today.withDayOfMonth(day.coerceIn(1, today.dayOfMonth)).toEpochDay()
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            selectDay(down.position.x)
                            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                                selectDay(change.position.x)
                            }
                            if (drag != null) {
                                horizontalDrag(drag.id) { change ->
                                    change.consume()
                                    selectDay(change.position.x)
                                }
                            }
                        }
                    }, horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom) {
                    statistics.days.forEach { day ->
                        Column(Modifier.weight(1f).fillMaxHeight().semantics {
                            contentDescription = "${day.date.format(formatter)}: " +
                                (day.percent?.let { "$it процентов" } ?: "нет наступивших приёмов") +
                                if (day.date == date) ", выбранный день" else ""
                        }.background(
                            if (day.date == date) Accent.copy(alpha = 0.24f) else Outline.copy(alpha = 0.75f),
                            RoundedCornerShape(3.dp)
                        ).border(
                            1.dp,
                            if (day.date == date) Accent else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(3.dp)
                        ),
                            verticalArrangement = Arrangement.Bottom) {
                            if (day.due > 0) {
                                val fraction = day.taken.toFloat() / day.due
                                Box(Modifier.fillMaxWidth().fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                                    .background(if (day.taken == 0) MaterialTheme.colorScheme.error else Accent,
                                        RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp).height(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    statistics.days.forEach { day ->
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (day.date == date) {
                                Text(date.dayOfMonth.toString(),
                                    modifier = Modifier.wrapContentWidth(unbounded = true),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold, color = Muted, maxLines = 1)
                            }
                        }
                    }
                }
                Text(
                    "Проведите пальцем по диаграмме влево или вправо, чтобы выбрать день. Статистика доступна только по сегодняшний день включительно.",
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            Text("По лекарствам · ${date.format(formatter)}", fontWeight = FontWeight.Bold)
            val dayEntries = statistics.days.first { it.date == date }.entries
            val groups = dayEntries.groupBy { medicineNameKey(it.medicine.name) }
            if (groups.isEmpty()) {
                DashboardPanel(Modifier.fillMaxWidth()) {
                    Text("На выбранный день приёмов нет.", color = Muted)
                }
            }
            groups.toSortedMap().forEach { (_, entries) ->
                val due = entries.count { it.status != DiaryStatus.UPCOMING }
                val taken = entries.count { it.status == DiaryStatus.TAKEN }
                DashboardPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(capitalizeFirstLetter(entries.first().medicine.name), modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold)
                        Text(if (due == 0) "0%" else "${taken * 100 / due}%", fontWeight = FontWeight.Bold)
                    }
                    val fraction = if (due == 0) 0f else taken.toFloat() / due
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp).height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (due == 0) SoftAccent else MaterialTheme.colorScheme.error)
                            .semantics {
                                contentDescription = if (due == 0) "Нет наступивших приёмов"
                                    else "Принято ${taken * 100 / due} процентов"
                            }
                    ) {
                        if (fraction > 0f) {
                            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight()
                                .background(androidx.compose.ui.graphics.Color(0xFF438461)))
                        }
                    }
                    Text(if (due == 0) "Время приёма ещё не наступило"
                        else "Принято $taken из $due", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("История приёмов", fontWeight = FontWeight.Bold)
            val entries = dayEntries
            if (entries.isEmpty()) {
                DashboardPanel(Modifier.fillMaxWidth()) {
                    Text("На выбранный день приёмов нет.", color = Muted)
                }
            }
            entries.forEach { entry ->
                DashboardPanel(Modifier.fillMaxWidth()) {
                    Text("${formatDoseTime(entry.minutes)} · ${capitalizeFirstLetter(entry.medicine.name)}",
                        fontWeight = FontWeight.Bold)
                    Text(entry.medicine.dosage, color = Muted)
                    Text(when (entry.status) {
                        DiaryStatus.TAKEN -> "Принято"
                        DiaryStatus.SKIPPED -> "Пропущено"
                        DiaryStatus.UNMARKED -> "Нет отметки о приёме"
                        DiaryStatus.UPCOMING -> "По расписанию"
                    }, color = if (entry.status == DiaryStatus.TAKEN) Accent else Muted)
                }
            }
        }
    }
}

@Composable
private fun DiaryTabs(selected: Int, onSelect: (Int) -> Unit) {
    val position by animateFloatAsState(
        targetValue = selected.toFloat(),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "Diary tab selection"
    )
    val highlight = Panel
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SoftAccent)
            .border(1.dp, Outline, RoundedCornerShape(20.dp))
            .padding(4.dp)
            .drawBehind {
                val slotWidth = size.width / 2
                val visualPosition = if (layoutDirection == LayoutDirection.Rtl) 1f - position else position
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(slotWidth * visualPosition, 0f),
                    size = Size(slotWidth, size.height),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Приём лекарств", "Показатели").forEachIndexed { index, label ->
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .selectable(selected = selected == index, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (selected == index) Accent else Muted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

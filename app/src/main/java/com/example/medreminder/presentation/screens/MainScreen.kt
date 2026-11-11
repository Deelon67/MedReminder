package com.example.medreminder.presentation.screens

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.medreminder.presentation.viewmodel.MainIntent
import com.example.medreminder.presentation.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MainScreen(viewModel: MainViewModel, onAddMedicineClick: () -> Unit, onAddPreparationClick: () -> Unit, onAddSchedule: (Int) -> Unit, onOpenPreparation: (Int) -> Unit) {
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var chosenDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var pendingRemovalId by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingRemovalDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val onAddClick = if (tab == 1) onAddPreparationClick else onAddMedicineClick
    val cabinet = com.example.medreminder.domain.model.cabinetMedicines(state.medicines)
    val today = rememberPhoneDate()
    val selected = chosenDate?.let(LocalDate::ofEpochDay) ?: today
    val query = com.example.medreminder.domain.model.medicineNameKey(search)
    val medicines = (if (tab == 1) cabinet else state.medicines)
        .filter { medicine ->
            if (tab == 0) medicine.isScheduledOn(selected)
            else query.isEmpty() || com.example.medreminder.domain.model.medicineNameKey(medicine.name).startsWith(query)
        }
        .sortedWith(compareBy({ it.hour }, { it.minute }, { it.name }))
    val context = LocalContext.current
    val openCalendar = {
        DatePickerDialog(context, { _, year, month, day ->
            val date = LocalDate.of(year, month + 1, day)
            chosenDate = if (date == today) null else date.toEpochDay()
        }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
        Unit
    }

    MaterialTheme(
        colorScheme = DashboardColors,
        typography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp)
        )
    ) {
        Scaffold(
            containerColor = Background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = { DashboardNavigation(tab) { tab = it } },
            floatingActionButton = {
                if (tab == 0 || tab == 1) {
                    FloatingActionButton(
                        onClick = onAddClick,
                        containerColor = Accent,
                        contentColor = DashboardColors.onPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        ),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Accent, DashboardColors.onPrimaryContainer)))
                                .border(1.dp, DashboardColors.onPrimary.copy(alpha = .2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, if (tab == 1) "Добавить препарат" else "Добавить приём", Modifier.size(34.dp))
                        }
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0, 1 -> LazyColumn(
                    modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
                    contentPadding = PaddingValues(
                        start = 16.dp + padding.calculateLeftPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                        end = 16.dp + padding.calculateRightPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                        top = 16.dp + padding.calculateTopPadding(),
                        bottom = 96.dp + padding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        if (tab == 0) {
                            DateHeader(selected, today, openCalendar, { chosenDate = null }) {
                                chosenDate = if (it == today) null else it.toEpochDay()
                            }
                        } else {
                            ScreenTitle("Аптечка")
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = search, onValueChange = { search = it },
                                placeholder = { Text("Найти лекарство") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                singleLine = true, shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent, unfocusedBorderColor = Accent,
                                    focusedContainerColor = Panel, unfocusedContainerColor = Panel
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            Surface(color = SoftAccent, shape = RoundedCornerShape(12.dp)) {
                                Text(if (query.isEmpty()) "Все · ${cabinet.size}" else "Найдено · ${medicines.size}", color = Accent, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                    state.error?.let { message ->
                        item { Text(message, color = MaterialTheme.colorScheme.error) }
                    }
                    if (state.isLoading) {
                        item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    } else if (state.medicines.isEmpty()) {
                        item {
                            EmptyMedicines(tab == 1, onAddClick)
                        }
                    } else {
                        if (medicines.isEmpty()) {
                            item {
                                if (tab == 0) EmptyMedicines(false, onAddMedicineClick)
                                else Text("Лекарства не найдены", color = Muted, modifier = Modifier.padding(vertical = 24.dp))
                            }
                        }
                        items(medicines, key = { it.id }) { medicine ->
                            if (tab == 0) {
                                TodayDoseCard(medicine, selected, state, viewModel) {
                                    pendingRemovalDay = selected.toEpochDay()
                                    pendingRemovalId = medicine.id
                                }
                            } else {
                                CabinetMedicineCard(medicine, today, onAddSchedule = { onAddSchedule(medicine.id) },
                                    onOpen = { onOpenPreparation(medicine.id) }) {
                                    pendingRemovalDay = null
                                    pendingRemovalId = medicine.id
                                }
                            }
                        }
                    }
                }
                2 -> DiaryScreen(Modifier.padding(padding), state, viewModel)
                3 -> ProfileScreen(Modifier.padding(padding), cabinet.size, viewModel::updateReminderSettings)
            }
        }
        val removalTarget = (cabinet + state.medicines).firstOrNull { it.id == pendingRemovalId }
        if (removalTarget != null) {
            val day = pendingRemovalDay
            val dismissRemoval = { pendingRemovalId = null; pendingRemovalDay = null }
            AlertDialog(
                onDismissRequest = dismissRemoval,
                title = { Text(if (day == null) "Удалить препарат?" else "Убрать приёмы на этот день?") },
                text = {
                    val name = com.example.medreminder.domain.model.capitalizeFirstLetter(removalTarget.name)
                    Text(if (day == null) {
                        "«$name» будет удалён из аптечки вместе со всеми схемами приёма. Напоминания будут отменены."
                    } else {
                        val date = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        "Убрать приём препарата «$name» на $date? Схема и приёмы в другие дни сохранятся."
                    })
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dismissRemoval()
                            viewModel.processIntent(if (day == null) MainIntent.DeleteMedicine(removalTarget)
                                else MainIntent.HideMedicineOnDate(removalTarget, day))
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(if (day == null) "Удалить" else "Убрать приём(-ы)") }
                },
                dismissButton = { TextButton(onClick = dismissRemoval) { Text("Отмена") } }
            )
        }
    }
}

@Composable
private fun rememberPhoneDate(): LocalDate {
    val context = LocalContext.current
    var date by remember { mutableStateOf(LocalDate.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) date = LocalDate.now()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { date = LocalDate.now() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        date = LocalDate.now()
        onDispose {
            context.unregisterReceiver(receiver)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return date
}

@Composable
private fun DateHeader(
    selected: LocalDate, today: LocalDate, onCalendar: () -> Unit,
    onToday: () -> Unit, onSelect: (LocalDate) -> Unit
) {
    val russian = Locale.forLanguageTag("ru")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(onClick = onCalendar).padding(4.dp)) {
            Text(selected.format(DateTimeFormatter.ofPattern("EEEE", russian)).uppercase(russian),
                color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            ScreenTitle(selected.format(DateTimeFormatter.ofPattern("d MMMM", russian)))
        }
        Surface(shape = RoundedCornerShape(16.dp), color = Panel, border = androidx.compose.foundation.BorderStroke(1.dp, Outline)) {
            IconButton(onClick = onCalendar) { Icon(Icons.Default.DateRange, "Выбрать дату") }
        }
    }
    if (selected != today) {
        TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text("← к сегодняшнему дню")
        }
    } else Spacer(Modifier.height(16.dp))
    val monday = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(7) { index ->
            val day = monday.plusDays(index.toLong())
            val active = day == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .background(if (active) SoftAccent else Background)
                    .clickable { onSelect(day) }
                    .semantics { contentDescription = day.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", russian)) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(day.format(DateTimeFormatter.ofPattern("EE", russian)), fontSize = 12.sp, color = if (active) Accent else Muted)
                Text(day.dayOfMonth.toString(), fontSize = 18.sp, color = if (active || day == today) Accent else Ink)
                Box(Modifier.padding(top = 4.dp).size(5.dp).border(1.5.dp, if (active) Muted else Outline, RoundedCornerShape(50)))
            }
        }
    }
}

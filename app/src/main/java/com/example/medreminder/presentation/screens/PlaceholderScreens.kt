package com.example.medreminder.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.medreminder.DayRoutinePreferences
import com.example.medreminder.RoutineMoment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProfileScreen(modifier: Modifier, medicineCount: Int, onRepeatSettingsChange: (Boolean, Int) -> Unit) {
    val context = LocalContext.current
    val routinePreferences = remember(context.applicationContext) { DayRoutinePreferences(context) }
    val routineFlow = remember(routinePreferences) { routinePreferences.observe() }
    val routine by routineFlow.collectAsState(initial = routinePreferences.read())
    var editingMoment by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenTitle("Настройки")
        SectionCaption("РАСПОРЯДОК ДНЯ")
        DashboardPanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            RoutineMoment.entries.forEachIndexed { index, moment ->
                if (index > 0) HorizontalDivider(color = Outline)
                Box(Modifier.fillMaxWidth()
                    .clickable(onClickLabel = "Изменить время") { editingMoment = moment.name }
                    .heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                    ProfileRow(moment.title, trailing = formatDoseTime(routine.getValue(moment)))
                }
            }
        }
        Text(
            "Нажмите на время, чтобы изменить его. Завтрак, обед и ужин появятся быстрыми кнопками в новой схеме.",
            modifier = Modifier.padding(horizontal = 4.dp),
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp
        )
        SectionCaption("ПРОВЕРКА НАПОМИНАНИЙ")
        DashboardPanel(Modifier.fillMaxWidth()) {
            NotificationPermissionRow()
            PanelDivider()
            BackgroundBatteryRow()
        }
        SectionCaption("НАПОМИНАНИЯ")
        ReminderSettingsCard(onRepeatSettingsChange)
    }
    editingMoment?.let { name ->
        val moment = RoutineMoment.valueOf(name)
        key(moment) {
            MedicineTimePicker(
                initial = routine.getValue(moment),
                onDismiss = { editingMoment = null },
                onConfirm = { minutes ->
                    routinePreferences.update(moment, minutes)
                    editingMoment = null
                }
            )
        }
    }
}

@Composable
private fun SectionCaption(title: String) {
    Text(
        title, modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Muted, letterSpacing = .8.sp
    )
}

@Composable
private fun PanelDivider() {
    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Outline)
}

@Composable
private fun ProfileRow(title: String, subtitle: String? = null, trailing: String? = null, warning: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = if (warning) MaterialTheme.colorScheme.error else Ink)
            subtitle?.let { Text(it, color = Muted, fontSize = 12.sp, lineHeight = 17.sp) }
        }
        trailing?.let {
            Text(it, color = Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

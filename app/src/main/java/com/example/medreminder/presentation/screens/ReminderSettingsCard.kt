package com.example.medreminder.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medreminder.ReminderPreferences

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReminderSettingsCard(onChange: (Boolean, Int) -> Unit) {
    val context = LocalContext.current
    val preferences = remember { ReminderPreferences(context) }
    var enabled by remember { mutableStateOf(preferences.enabled) }
    var interval by remember { mutableIntStateOf(preferences.intervalMinutes) }
    DashboardPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Повторные напоминания", fontWeight = FontWeight.Bold)
                    Text("Если приём не отмечен — не более двух повторов.", color = Muted,
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    onChange(it, interval)
                })
            }
            if (enabled) {
                Text("Интервал повторов", color = Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { value ->
                        SelectionChip("$value мин", interval == value, onClick = {
                            interval = value
                            onChange(enabled, value)
                        })
                    }
                }
            }
            Text("Кнопки «Принято» и «Пропустить» завершают приём без возможности отмены и останавливают повторы. Ручная отсрочка заменяет автоматические.",
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

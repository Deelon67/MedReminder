package com.example.medreminder.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun DiaryScreen(modifier: Modifier, today: LocalDate) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Дневник")
        StaticSegments(listOf("Приём лекарств", "Показатели"))
        DashboardPanel(Modifier.fillMaxWidth()) {
            Text("—", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("соблюдение за 30 дней", color = Muted, fontSize = 13.sp, lineHeight = 16.sp)
            Canvas(Modifier.fillMaxWidth().height(96.dp).padding(top = 14.dp)) {
                drawLine(Outline.copy(alpha = .4f), Offset.Zero, Offset(size.width, 0f))
                drawLine(Outline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(today.minusDays(29).format(formatter), color = Muted, fontSize = 11.sp)
                Text(today.format(formatter), color = Muted, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardPanel(Modifier.weight(1f)) {
                Text("0", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("дней подряд без пропусков", color = Muted, fontSize = 13.sp, lineHeight = 16.sp)
            }
            DashboardPanel(Modifier.weight(1f)) {
                Text("—", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("пропусков нет", color = Muted, fontSize = 13.sp, lineHeight = 16.sp)
            }
        }
        Surface(color = Accent, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(17.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, null, tint = Background, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Отчёт для врача (PDF)", color = Background, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
internal fun ProfileScreen(modifier: Modifier, medicineCount: Int) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenTitle("Профиль")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardPanel(Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(44.dp).background(SoftAccent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Text("Я", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Я", fontWeight = FontWeight.Bold)
                    Text("$medicineCount лек.", fontSize = 12.sp, color = Muted)
                }
            }
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(23.dp), color = Background, border = androidx.compose.foundation.BorderStroke(1.5.dp, Muted.copy(alpha = .7f))) {
                Column(Modifier.padding(14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+", fontSize = 28.sp, color = Muted, fontWeight = FontWeight.Bold)
                    Text("Добавить\nблизкого", color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp)
                }
            }
        }
        SectionCaption("ПРОВЕРКА НАПОМИНАНИЙ")
        DashboardPanel {
            ProfileRow("Уведомления", "не проверено", "—")
            PanelDivider()
            ProfileRow("Будильники и напоминания", "точное время · не проверено", "—")
            PanelDivider()
            ProfileRow("Работа в фоне", "экономия батареи может отменять напоминания", "Исправить", warning = true)
            PanelDivider()
            ProfileRow("Автозапуск", "разрешите приложению запускаться самостоятельно — проверить это автоматически нельзя", "›")
            PanelDivider()
            ProfileRow("Отправить пробное напоминание", "одно уведомление сразу, второе через минуту — закройте приложение и дождитесь", "›")
            PanelDivider()
            ProfileRow("Следующее напоминание: —", "Последнее срабатывание: —")
        }
        SectionCaption("НАПОМИНАНИЯ")
        DashboardPanel {
            StaticToggle("Настойчивые напоминания", "для новых лекарств: повтор каждые 10 мин", true)
            PanelDivider()
            StaticToggle("Тихие часы", "без звука 23:00 – 07:00", false)
        }
        SectionCaption("РАСПОРЯДОК ДНЯ")
        DashboardPanel {
            listOf("Подъём" to "07:00", "Завтрак" to "08:00", "Обед" to "13:00", "Ужин" to "19:00", "Отход ко сну" to "23:00")
                .forEachIndexed { index, (label, time) ->
                    if (index > 0) PanelDivider()
                    ProfileRow(label, trailing = time)
                }
        }
        Text("Эти времена появятся быстрыми кнопками при добавлении лекарства.", color = Muted, fontSize = 12.sp)
        SectionCaption("ОФОРМЛЕНИЕ")
        StaticSegments(listOf("Системная", "Светлая", "Тёмная"), selected = 1)
        DashboardPanel { ProfileRow("Экспорт данных", "Файл .json — можно хранить на Диске или в мессенджере", "›") }
    }
}

@Composable
private fun SectionCaption(title: String) {
    Text(title, modifier = Modifier.padding(start = 4.dp, top = 8.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Muted, letterSpacing = .8.sp)
}

@Composable
private fun PanelDivider() { HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Outline) }

@Composable
private fun ProfileRow(title: String, subtitle: String? = null, trailing: String? = null, warning: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (warning) MaterialTheme.colorScheme.error else Ink)
            subtitle?.let { Text(it, color = Muted, fontSize = 12.sp, lineHeight = 16.sp) }
        }
        trailing?.let { Text(it, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun StaticToggle(title: String, subtitle: String, checked: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = Muted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Color.White, uncheckedTrackColor = Outline, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent))
    }
}

@Composable
private fun StaticSegments(labels: List<String>, selected: Int = 0) {
    Row(Modifier.fillMaxWidth().background(SoftAccent, RoundedCornerShape(15.dp)).padding(3.dp)) {
        labels.forEachIndexed { index, label ->
            Box(Modifier.weight(1f).background(if (index == selected) Panel else Color.Transparent, RoundedCornerShape(12.dp)).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(label, fontSize = 12.sp, color = if (index == selected) Ink else Muted, fontWeight = FontWeight.Medium)
            }
        }
    }
}

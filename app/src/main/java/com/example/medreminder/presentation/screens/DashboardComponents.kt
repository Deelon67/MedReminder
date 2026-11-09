package com.example.medreminder.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val DashboardColors = com.example.medreminder.presentation.theme.LightColorScheme
internal val Background = DashboardColors.background
internal val Panel = DashboardColors.surface
internal val Outline = DashboardColors.outlineVariant
internal val Accent = DashboardColors.primary
internal val Ink = DashboardColors.onSurface
internal val Muted = DashboardColors.onSurfaceVariant
internal val SoftAccent = DashboardColors.primaryContainer

@Composable
internal fun ScreenTitle(text: String) {
    Text(text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
}

@Composable
internal fun DashboardNavigation(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        modifier = Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
        shape = RoundedCornerShape(25.dp), color = Panel, border = BorderStroke(1.dp, Outline)
    ) {
        Row(Modifier.selectableGroup().padding(vertical = 10.dp)) {
            listOf("Сегодня", "Аптечка", "Дневник", "Профиль").forEachIndexed { index, label ->
                val color = if (selected == index) Accent else Muted
                Column(
                    Modifier.weight(1f).selectable(selected == index, role = Role.Tab, onClick = { onSelect(index) }).padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(44.dp, 28.dp).clip(CircleShape).background(if (selected == index) SoftAccent else Color.Transparent), contentAlignment = Alignment.Center) {
                        NavigationGlyph(index, color)
                    }
                    Text(label, color = color, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun NavigationGlyph(index: Int, color: Color) {
    Canvas(Modifier.size(23.dp)) {
        val w = size.width
        val stroke = 1.6.dp.toPx()
        val corner = androidx.compose.ui.geometry.CornerRadius(w * .12f)
        when (index) {
            0 -> {
                // Calendar with a check mark.
                drawRoundRect(color, Offset(w * .12f, w * .18f), Size(w * .76f, w * .72f), corner, style = Stroke(stroke))
                drawLine(color, Offset(w * .12f, w * .38f), Offset(w * .88f, w * .38f), stroke)
                for (x in listOf(.32f, .68f)) drawLine(color, Offset(w * x, w * .08f), Offset(w * x, w * .27f), stroke)
                val check = Path().apply { moveTo(w * .32f, w * .61f); lineTo(w * .46f, w * .74f); lineTo(w * .71f, w * .49f) }
                drawPath(check, color, style = Stroke(stroke))
            }
            1 -> {
                // Outlined medicine case.
                drawRoundRect(color, Offset(w * .09f, w * .29f), Size(w * .82f, w * .59f), corner, style = Stroke(stroke))
                drawRoundRect(color, Offset(w * .34f, w * .12f), Size(w * .32f, w * .17f), corner, style = Stroke(stroke))
                drawLine(color, Offset(w * .5f, w * .43f), Offset(w * .5f, w * .73f), stroke)
                drawLine(color, Offset(w * .35f, w * .58f), Offset(w * .65f, w * .58f), stroke)
            }
            2 -> {
                // Journal with a binding and short entries.
                drawRoundRect(color, Offset(w * .18f, w * .08f), Size(w * .66f, w * .84f), corner, style = Stroke(stroke))
                drawLine(color, Offset(w * .34f, w * .08f), Offset(w * .34f, w * .92f), stroke)
                for (y in listOf(.34f, .52f, .70f)) drawLine(color, Offset(w * .46f, w * y), Offset(w * .70f, w * y), stroke)
            }
            else -> {
                // Airy outline avatar.
                drawCircle(color, w * .44f, style = Stroke(stroke))
                drawCircle(color, w * .14f, Offset(w * .5f, w * .36f), style = Stroke(stroke))
                drawArc(color, 180f, 180f, false, Offset(w * .25f, w * .59f), Size(w * .5f, w * .4f), style = Stroke(stroke))
            }
        }
    }
}

@Composable
internal fun EmptyMedicines(cabinet: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Accent, Color(0xFFFFBD60)).forEach { color ->
                Box(Modifier.size(30.dp).background(Brush.linearGradient(listOf(color.copy(alpha = .7f), color, color)), CircleShape))
            }
            Canvas(Modifier.size(30.dp)) {
                drawCircle(Outline)
                drawCircle(Background, radius = size.width * .42f)
                drawCircle(Outline, radius = size.width * .34f, style = Stroke(2.dp.toPx()))
                drawCircle(Outline, radius = size.width * .48f, style = Stroke(1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(if (cabinet) "Аптечка пуста" else "Добавьте первое лекарство",
            fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            if (cabinet) "Добавьте лекарства, чтобы следить за запасом и сроками годности."
            else "Укажите название и время приёма — напомним вовремя и посчитаем запас.",
            color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd, shape = RoundedCornerShape(17.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp)) {
            Text("Добавить лекарство", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun DashboardPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(23.dp), color = Panel, border = BorderStroke(1.dp, Outline)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

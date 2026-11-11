package com.example.medreminder.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun BackgroundBatteryRow() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var unrestricted by remember { mutableStateOf(context.isBatteryOptimizationDisabled()) }
    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) unrestricted = context.isBatteryOptimizationDisabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        unrestricted = context.isBatteryOptimizationDisabled()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(onClickLabel = "Настроить работу в фоне") {
                unrestricted = context.isBatteryOptimizationDisabled()
                if (unrestricted) {
                    Toast.makeText(context, "Работа в фоне уже включена", Toast.LENGTH_SHORT).show()
                } else {
                    context.requestBatteryOptimizationExemption()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Работа в фоне", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text("Чтобы напоминания приходили регулярно, даже когда телефон не используется. Расход батареи может вырасти.",
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (unrestricted) {
            Icon(Icons.Default.Check, "Оптимизация батареи отключена",
                modifier = Modifier.size(22.dp), tint = Accent)
        } else {
            Text("Настроить", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun Context.isBatteryOptimizationDisabled(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

private fun Context.requestBatteryOptimizationExemption() {
    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.fromParts("package", packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(request)
    } catch (_: ActivityNotFoundException) {
        openBatteryOptimizationSettings()
    } catch (_: SecurityException) {
        openBatteryOptimizationSettings()
    }
}

private fun Context.openBatteryOptimizationSettings() {
    try {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

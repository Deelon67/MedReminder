package com.example.medreminder.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun NotificationPermissionRow() {
    val context = LocalContext.current
    val activity = context.notificationPermissionActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(context.notificationsAllowed()) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var requesting by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        requesting = false
        enabled = context.notificationsAllowed()
        if (!enabled && Build.VERSION.SDK_INT >= 33 && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
            dialog = "settings"
        }
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = context.notificationsAllowed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        enabled = context.notificationsAllowed()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requesting = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(
            enabled = !requesting,
            onClickLabel = "Проверить разрешение на уведомления"
        ) {
            enabled = context.notificationsAllowed()
            when {
                enabled -> Toast.makeText(context, "Уведомления уже включены", Toast.LENGTH_SHORT).show()
                Build.VERSION.SDK_INT < 33 || activity == null -> dialog = "settings"
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> dialog = "settings"
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS) ->
                    dialog = "rationale"
                else -> requestPermission()
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Уведомления", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(if (enabled) "Разрешены" else "Не разрешены", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (enabled) {
            Icon(Icons.Default.Check, "Разрешение получено", tint = Accent, modifier = Modifier.size(22.dp))
        } else {
            Text("Разрешить", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    dialog?.let { kind ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Разрешите уведомления") },
            text = {
                Text(when (kind) {
                    "rationale" -> "Разрешите уведомления, чтобы получать напоминания о приёме лекарств."
                    else -> "Включите уведомления для приложения в системных настройках."
                })
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    when (kind) {
                        "rationale" -> {
                            if (!context.notificationsAllowed()) requestPermission()
                            enabled = context.notificationsAllowed()
                        }
                        "settings" -> context.openNotificationSettings()
                    }
                }) {
                    Text(when (kind) {
                        "rationale" -> "Продолжить"
                        else -> "Открыть настройки"
                    })
                }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Отмена") }
            }
        )
    }
}

private fun Context.notificationsAllowed(): Boolean =
    (Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun Context.notificationPermissionActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openNotificationSettings() {
    val notificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(notificationSettings)
    } catch (_: android.content.ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

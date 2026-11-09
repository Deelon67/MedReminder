package com.example.medreminder.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF237D76),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF2ED),
    onPrimaryContainer = Color(0xFF195B56),
    secondary = Color(0xFF527D94),
    secondaryContainer = Color(0xFFE5F0F6),
    onSecondaryContainer = Color(0xFF304F60),
    tertiary = Color(0xFFA5693F),
    background = Color(0xFFF4F8F7),
    onBackground = Color(0xFF243C3A),
    surface = Color.White,
    onSurface = Color(0xFF243C3A),
    surfaceVariant = Color(0xFFEDF4F2),
    onSurfaceVariant = Color(0xFF607672),
    outline = Color(0xFFB9CECA),
    outlineVariant = Color(0xFFDCE8E5),
    error = Color(0xFFB64F50)
)

@Composable
fun MedReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
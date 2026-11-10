package com.example.medreminder

import android.content.Context

class ReminderPreferences(context: Context, name: String = "reminder_preferences") {
    private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    val enabled: Boolean get() = preferences.getBoolean("repeats_enabled", true)
    val intervalMinutes: Int get() = preferences.getInt("repeat_interval", 15).takeIf { it in listOf(15, 30, 60) } ?: 15
    fun update(enabled: Boolean, intervalMinutes: Int) {
        require(intervalMinutes in listOf(15, 30, 60))
        preferences.edit().putBoolean("repeats_enabled", enabled).putInt("repeat_interval", intervalMinutes).apply()
    }
}

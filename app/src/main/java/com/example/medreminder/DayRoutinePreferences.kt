package com.example.medreminder

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class RoutineMoment(val title: String, val defaultMinutes: Int) {
    WAKE("Подъём", 7 * 60),
    BREAKFAST("Завтрак", 8 * 60),
    LUNCH("Обед", 13 * 60),
    DINNER("Ужин", 19 * 60),
    SLEEP("Отход ко сну", 23 * 60)
}

class DayRoutinePreferences(context: Context, name: String = "day_routine") {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun read(): Map<RoutineMoment, Int> = RoutineMoment.entries.associateWith { moment ->
        preferences.getInt(moment.name, moment.defaultMinutes).takeIf { it in 0..1439 }
            ?: moment.defaultMinutes
    }

    fun update(moment: RoutineMoment, minutes: Int) {
        require(minutes in 0..1439)
        preferences.edit().putInt(moment.name, minutes).apply()
    }

    fun observe(): Flow<Map<RoutineMoment, Int>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
}

fun Map<RoutineMoment, Int>.mealTimes(): List<Int> =
    listOf(RoutineMoment.BREAKFAST, RoutineMoment.LUNCH, RoutineMoment.DINNER)
        .map { getValue(it) }.distinct().sorted()

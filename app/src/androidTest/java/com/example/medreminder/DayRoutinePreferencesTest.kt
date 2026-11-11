package com.example.medreminder

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayRoutinePreferencesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun defaultsAndSavedChangesSupplyMealShortcuts() {
        val name = "routine-defaults-test"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            val routine = DayRoutinePreferences(context, name)
            assertEquals(listOf(480, 780, 1140), routine.read().mealTimes())
            routine.update(RoutineMoment.LUNCH, 810)
            routine.update(RoutineMoment.WAKE, 390)
            val reopened = DayRoutinePreferences(context, name).read()
            assertEquals(810, reopened.getValue(RoutineMoment.LUNCH))
            assertEquals(390, reopened.getValue(RoutineMoment.WAKE))
            assertEquals(listOf(480, 810, 1140), reopened.mealTimes())
            routine.update(RoutineMoment.DINNER, 810)
            assertEquals(listOf(480, 810), routine.read().mealTimes())
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test fun anotherInstanceObservesChangesAndInvalidStoredTimesFallBack() = runBlocking {
        val name = "routine-observe-test"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            val reader = DayRoutinePreferences(context, name)
            val updated = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(3000) {
                    reader.observe().first { it.getValue(RoutineMoment.BREAKFAST) == 540 }
                }
            }
            DayRoutinePreferences(context, name).update(RoutineMoment.BREAKFAST, 540)
            assertEquals(540, updated.await().getValue(RoutineMoment.BREAKFAST))
            preferences.edit().putInt(RoutineMoment.SLEEP.name, 1440).commit()
            assertEquals(1380, reader.read().getValue(RoutineMoment.SLEEP))
            try {
                reader.update(RoutineMoment.WAKE, -1)
                fail("Out-of-day time must be rejected")
            } catch (_: IllegalArgumentException) {
                assertEquals(420, reader.read().getValue(RoutineMoment.WAKE))
            }
        } finally {
            preferences.edit().clear().commit()
        }
    }
}

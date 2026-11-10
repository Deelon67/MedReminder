package com.example.medreminder

import com.example.medreminder.domain.model.doseWithFormUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class DoseUnitsTest {
    @Test fun onlyUnambiguousUnitsDetermineReleaseForm() {
        assertEquals("Таблетки", com.example.medreminder.domain.model.preparationFormFromDose("1 табл."))
        assertEquals("Капсулы", com.example.medreminder.domain.model.preparationFormFromDose("2 КАПС."))
        assertEquals("Капли", com.example.medreminder.domain.model.preparationFormFromDose("5 капли"))
        assertEquals("Спрей", com.example.medreminder.domain.model.preparationFormFromDose("1 впрыск"))
        listOf("5 мл", "20 мг", "3 г", "1 шт.", "3").forEach {
            assertEquals("", com.example.medreminder.domain.model.preparationFormFromDose(it))
        }
    }

    @Test fun numericDoseUsesUpdatedForm() {
        assertEquals("3", doseWithFormUnit("3", ""))
        assertEquals("3 табл.", doseWithFormUnit("3", "Таблетки"))
        assertEquals("3 капс.", doseWithFormUnit("3", "Капсулы"))
        assertEquals("2,5 мл", doseWithFormUnit("2,5", "Сироп"))
    }

    @Test fun explicitInstructionsAndUnitsArePreserved() {
        assertEquals("1 таблетка после еды", doseWithFormUnit("1 таблетка после еды", "Таблетки"))
        assertEquals("3 мг", doseWithFormUnit("3 мг", "Капсулы"))
        assertEquals("", doseWithFormUnit("", "Таблетки"))
    }
}

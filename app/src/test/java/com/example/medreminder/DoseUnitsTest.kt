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

    @Test fun changingFormUpdatesOnlyThePreviousFormUnit() {
        fun change(dose: String, old: String = "Капли", new: String = "Таблетки") =
            com.example.medreminder.domain.model.doseAfterFormChange(dose, old, new)
        assertEquals("3 табл.", change("3 кап."))
        assertEquals("2,5 капс.", change("2,5 табл.", "Таблетки", "Капсулы"))
        assertEquals("3 мг", change("3 мг"))
        assertEquals("3 кап. после еды", change("3 кап. после еды"))
        assertEquals("3 кап.", change("3 кап.", new = ""))
        assertEquals("3 кап.", change("3 кап.", new = "Капли"))
    }

    @Test fun numericDoseUsesUpdatedForm() {
        assertEquals("3", doseWithFormUnit("3", ""))
        assertEquals("3 табл.", doseWithFormUnit("3", "Таблетки"))
        assertEquals("3 капс.", doseWithFormUnit("3", "Капсулы"))
        assertEquals("2,5 мл", doseWithFormUnit("2,5", "Сироп"))
    }

    @Test fun displayUsesFullUnitsAndRussianEndings() {
        fun display(dose: String, form: String = "") =
            com.example.medreminder.domain.model.formatDoseForDisplay(dose, form)
        assertEquals("1 таблетка", display("1 табл."))
        assertEquals("3 таблетки", display("3 табл."))
        assertEquals("5 таблеток", display("5 табл."))
        assertEquals("11 таблеток", display("11 табл."))
        assertEquals("21 таблетка", display("21 табл."))
        assertEquals("22 таблетки", display("22 табл."))
        assertEquals("2 капсулы", display("2 капс."))
        assertEquals("1,5 таблетки", display("1,5 табл."))
        assertEquals("5 капель", display("5 кап."))
        assertEquals("2 миллилитра", display("2 мл"))
        assertEquals("10 миллиграммов", display("10 мг"))
        assertEquals("1 свеча", display("1 шт.", "Свечи"))
        assertEquals("1 таблетка после еды", display("1 таблетка после еды"))
        assertEquals("", display(""))
    }

    @Test fun explicitInstructionsAndUnitsArePreserved() {
        assertEquals("1 таблетка после еды", doseWithFormUnit("1 таблетка после еды", "Таблетки"))
        assertEquals("3 мг", doseWithFormUnit("3 мг", "Капсулы"))
        assertEquals("", doseWithFormUnit("", "Таблетки"))
    }
}

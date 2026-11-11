package com.example.medreminder

import com.example.medreminder.domain.model.consumeMedicineStock
import org.junit.Assert.*
import org.junit.Test

class MedicineStockTest {
    @Test fun doseQuantityIsSubtractedAndFiveIsNotLow() {
        assertEquals("5 табл.", consumeMedicineStock("8 табл.", "3 табл.", "Таблетки")!!.remaining)
        assertFalse(consumeMedicineStock("8 табл.", "3 табл.", "Таблетки")!!.isLow)
        assertEquals("4 табл.", consumeMedicineStock("7 табл.", "3 таблетки", "Таблетки")!!.remaining)
        assertTrue(consumeMedicineStock("7 табл.", "3 таблетки", "Таблетки")!!.isLow)
    }

    @Test fun decimalArithmeticIsExactAndStockCannotBeNegative() {
        assertEquals("0,2 мл", consumeMedicineStock("0,3 мл", "0,1 мл", "Раствор")!!.remaining)
        assertEquals("0 табл.", consumeMedicineStock("1 табл.", "3 табл.", "Таблетки")!!.remaining)
        assertEquals("1,5 табл.", consumeMedicineStock("2 табл.", "0,5", "Таблетки")!!.remaining)
    }

    @Test fun ointmentsLiquidsDropsSpraysAndOtherFormsUseTheirStockUnits() {
        listOf(
            Triple("Мазь", "г", "г"),
            Triple("Крем", "г", "г"),
            Triple("Гель", "г", "г"),
            Triple("Порошок", "г", "г"),
            Triple("Сироп", "мл", "мл"),
            Triple("Раствор", "мл", "мл"),
            Triple("Капли", "кап.", "кап."),
            Triple("Спрей", "впрыск", "впрыск."),
            Triple("Капсулы", "капс.", "капс."),
            Triple("Свечи", "шт.", "шт.")
        ).forEach { (form, stockUnit, doseUnit) ->
            val low = consumeMedicineStock("7 $stockUnit", "3 $doseUnit", form)!!
            assertEquals(form, "4 $stockUnit", low.remaining)
            assertTrue(form, low.isLow)
            assertFalse(form, consumeMedicineStock("8 $stockUnit", "3 $doseUnit", form)!!.isLow)
        }
    }

    @Test fun massConversionIsExactAndLowThresholdIsIndependentOfMassUnit() {
        assertEquals("4,5 г", consumeMedicineStock("5 г", "500 мг", "Мазь")!!.remaining)
        assertTrue(consumeMedicineStock("5 г", "500 мг", "Мазь")!!.isLow)
        assertEquals("4500 мг", consumeMedicineStock("5000 мг", "0,5 г", "Порошок")!!.remaining)
        assertTrue(consumeMedicineStock("5000 мг", "0,5 г", "Порошок")!!.isLow)
        assertFalse(consumeMedicineStock("6000 мг", "1 г", "Порошок")!!.isLow)
    }

    @Test fun dropAndSprayInventorySupportsCountsAndPreservesLegacyVolume() {
        assertEquals(listOf("кап.", "мл"),
            com.example.medreminder.domain.model.preparationStockUnits("Капли"))
        assertEquals(listOf("впрыск", "мл"),
            com.example.medreminder.domain.model.preparationStockUnits("Спрей"))
        assertEquals("мл", com.example.medreminder.domain.model.savedStockUnit("20 мл"))
        assertEquals("кап.", com.example.medreminder.domain.model.savedStockUnit("20 капель"))
        assertEquals("впрыск", com.example.medreminder.domain.model.savedStockUnit("20 впрыск."))
        assertEquals("4 табл.", consumeMedicineStock("7 таблеток", "3 табл.", "Таблетки")!!.remaining)
    }

    @Test fun missingStockAndIncompatibleUnitsAreNeverGuessed() {
        assertNull(consumeMedicineStock("", "2 табл.", "Таблетки"))
        assertNull(consumeMedicineStock("20 мл", "3 кап.", "Капли"))
        assertNull(consumeMedicineStock("20 табл.", "50 мг", "Таблетки"))
        assertNull(consumeMedicineStock("20 табл.", "2 табл. после еды", "Таблетки"))
        assertNull(consumeMedicineStock("20 табл.", "0 табл.", "Таблетки"))
    }
}

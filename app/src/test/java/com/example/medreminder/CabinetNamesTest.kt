package com.example.medreminder

import com.example.medreminder.data.db.TimeOfDay
import com.example.medreminder.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CabinetNamesTest {
    private val dose = Medicine(1, "ПАРАЦЕТАМОЛ", "1 таблетка", TimeOfDay.MORNING, 8, 0)

    @Test fun namesIgnoreCaseAndRepeatedWhitespace() {
        assertEquals(medicineNameKey("  Витамин   С "), medicineNameKey("витамин с"))
        assertEquals(medicineNameKey("ПАРАЦЕТАМОЛ"), medicineNameKey("парацетамол"))
    }

    @Test fun cabinetGroupsDosesWithoutLosingInventoryDetails() {
        val inventory = dose.copy(id = 2, name = "парацетамол", cabinetOnly = true,
            dosage = "500 мг", form = "Таблетки", quantity = "20 шт", expiryEpochDay = 22000)
        val medicines = listOf(dose, inventory, dose.copy(id = 3, hour = 12))
        val card = cabinetMedicines(medicines).single()
        assertEquals("Таблетки", card.form)
        assertEquals("20 шт", card.quantity)
        assertEquals(22000L, card.expiryEpochDay)
        assertTrue(card.dosage.contains("500 мг"))
        assertEquals(3, medicines.size)
    }

    @Test fun differentNamesRemainSeparate() {
        assertEquals(2, cabinetMedicines(listOf(dose, dose.copy(id = 2, name = "Ибупрофен"))).size)
    }
}

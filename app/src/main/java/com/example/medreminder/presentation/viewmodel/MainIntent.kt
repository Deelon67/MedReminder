package com.example.medreminder.presentation.viewmodel

import com.example.medreminder.domain.model.Medicine

// Действия, которые может выполнить пользователь
sealed class MainIntent {
    // Загрузить список лекарств
    object LoadMedicines : MainIntent()

    // Добавить новое лекарство
    data class AddMedicine(val medicine: Medicine) : MainIntent()

    data class HideMedicineOnDate(val medicine: Medicine, val epochDay: Long) : MainIntent()

    // Удалить лекарство
    data class DeleteMedicine(val medicine: Medicine) : MainIntent()
}
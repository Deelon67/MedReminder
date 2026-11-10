package com.example.medreminder.presentation.viewmodel

import com.example.medreminder.domain.model.Medicine

// Состояние главного экрана
data class MainState(
    val medicines: List<Medicine> = emptyList(), // Список лекарств
    val isLoading: Boolean = false, // Загрузка
    val error: String? = null, // Ошибка
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val doses: List<com.example.medreminder.data.db.DoseRecord> = emptyList(),
    val updatingDoses: Set<String> = emptySet()
)
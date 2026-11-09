package com.example.medreminder.domain.model


import com.example.medreminder.data.db.TimeOfDay

// Чистая бизнес-модель лекарства
// Не зависит от базы данных, используется в UI и бизнес-логике
data class Medicine(
    val id: Int,
    val name: String,
    val dosage: String,
    val timeOfDay: TimeOfDay,
    val hour: Int,
    val minute: Int
)
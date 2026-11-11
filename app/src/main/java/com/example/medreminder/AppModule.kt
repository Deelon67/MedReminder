package com.example.medreminder

import androidx.room.Room
import com.example.medreminder.data.db.AppDatabase
import com.example.medreminder.data.repository.MedicineRepositoryImpl
import com.example.medreminder.domain.repository.MedicineRepository
import com.example.medreminder.domain.usecase.GetMedicinesUseCase
import com.example.medreminder.presentation.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// Модуль Koin - здесь описываем все зависимости приложения
val appModule = module {

    // Создаем базу данных (одиночка - singleton)
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "medicines_database"
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_9_8).build()
    }

    // Создаем DAO из базы данных
    single { get<AppDatabase>().medicineDao() }
    single { get<AppDatabase>().doseRecordDao() }
    single { get<AppDatabase>().measurementDao() }
    single<com.example.medreminder.domain.repository.MeasurementRepository> {
        com.example.medreminder.data.repository.MeasurementRepositoryImpl(get())
    }
    single { DoseReminderManager(androidContext(), get(), get()) }

    // Создаем репозиторий
    single<MedicineRepository> { MedicineRepositoryImpl(get()) }

    // Создаем UseCase
    single { GetMedicinesUseCase(get()) }

    // Создаем ViewModel (теперь с контекстом)
    viewModel { MainViewModel(get(), get(), androidContext(), get(), get(), get()) }
}
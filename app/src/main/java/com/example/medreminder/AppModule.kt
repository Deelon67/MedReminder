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
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }

    // Создаем DAO из базы данных
    single { get<AppDatabase>().medicineDao() }

    // Создаем репозиторий
    single<MedicineRepository> { MedicineRepositoryImpl(get()) }

    // Создаем UseCase
    single { GetMedicinesUseCase(get()) }

    // Создаем ViewModel (теперь с контекстом)
    viewModel { MainViewModel(get(), get(), androidContext()) }
}
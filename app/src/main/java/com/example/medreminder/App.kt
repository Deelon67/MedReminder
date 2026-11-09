package com.example.medreminder

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

// Класс приложения - инициализируется при запуске приложения
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Запускаем Koin
        startKoin {
            androidLogger(Level.ERROR) // Логи только для ошибок
            androidContext(this@App) // Передаем контекст приложения
            modules(appModule) // Подключаем наш модуль зависимостей
        }
    }
}
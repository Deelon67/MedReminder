package com.example.medreminder.presentation.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.medreminder.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

// Экраны навигации
object Screens {
    const val MAIN = "main"
    const val ADD_MEDICINE = "add_medicine"
}

// Граф навигации приложения
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = koinViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screens.MAIN
    ) {
        // Главный экран
        composable(Screens.MAIN) {
            MainScreen(
                viewModel = viewModel,
                onAddMedicineClick = {
                    navController.navigate(Screens.ADD_MEDICINE)
                }
            )
        }

        // Экран добавления лекарства
        composable(Screens.ADD_MEDICINE) {
            AddMedicineScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
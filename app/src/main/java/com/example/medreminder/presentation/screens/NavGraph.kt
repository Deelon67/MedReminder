package com.example.medreminder.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.medreminder.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NavGraph(viewModel: MainViewModel = koinViewModel()) {
    var adding by rememberSaveable { mutableStateOf(false) }
    MainScreen(viewModel = viewModel, onAddMedicineClick = { adding = true })
    if (adding) {
        AddMedicineScreen(viewModel = viewModel, onBackClick = { adding = false })
    }
}

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
    var selectedPreparationId by rememberSaveable { mutableStateOf<Int?>(null) }
    var detailId by rememberSaveable { mutableStateOf<Int?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var addingPreparation by rememberSaveable { mutableStateOf(false) }
    MainScreen(
        viewModel = viewModel,
        onAddMedicineClick = { selectedPreparationId = null; adding = true },
        onAddPreparationClick = { addingPreparation = true },
        onAddSchedule = { selectedPreparationId = it; adding = true },
        onOpenPreparation = { detailId = it }
    )
    detailId?.let { id ->
        PreparationDetailScreen(
            viewModel = viewModel, medicineId = id,
            onBackClick = { detailId = null },
            onAddSchedule = { selectedPreparationId = it; detailId = null; adding = true }
        )
    }
    if (addingPreparation) {
        AddPreparationScreen(viewModel = viewModel, onBackClick = { addingPreparation = false })
    }
    if (adding) {
        AddMedicineScreen(viewModel = viewModel, onBackClick = { adding = false }, initialPreparationId = selectedPreparationId)
    }
}

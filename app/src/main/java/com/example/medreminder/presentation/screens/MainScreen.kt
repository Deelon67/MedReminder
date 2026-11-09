package com.example.medreminder.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medreminder.domain.model.Medicine
import com.example.medreminder.presentation.viewmodel.MainIntent
import com.example.medreminder.presentation.viewmodel.MainState
import com.example.medreminder.presentation.viewmodel.MainViewModel

// Главный экран приложения
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddMedicineClick: () -> Unit
) {
    // Получаем состояние из ViewModel
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои лекарства") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedicineClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить лекарство")
            }
        }
    ) { paddingValues ->
        // Отображение контента
        when {
            state.isLoading -> {
                // Индикатор загрузки
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.medicines.isEmpty() -> {
                // Пустое состояние
                EmptyState(paddingValues)
            }
            else -> {
                // Список лекарств
                MedicineList(
                    medicines = state.medicines,
                    paddingValues = paddingValues,
                    onDelete = { medicine ->
                        viewModel.processIntent(MainIntent.DeleteMedicine(medicine))
                    }
                )
            }
        }
    }
}

// Компонент списка лекарств
@Composable
private fun MedicineList(
    medicines: List<Medicine>,
    paddingValues: PaddingValues,
    onDelete: (Medicine) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        items(medicines) { medicine ->
            MedicineCard(
                medicine = medicine,
                onDelete = { onDelete(medicine) }
            )
        }
    }
}

// Компонент пустого состояния
@Composable
private fun EmptyState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Список лекарств пуст",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Нажмите + чтобы добавить первое лекарство",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
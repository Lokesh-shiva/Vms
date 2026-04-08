package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.SystemConfig
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.SystemConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemConfigScreen(
    viewModel: SystemConfigViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingConfig by remember { mutableStateOf<SystemConfig?>(null) }
    var editValue by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadConfigs()
    }

    editingConfig?.let { config ->
        AlertDialog(
            onDismissRequest = { editingConfig = null },
            title = { Text("Edit Configuration") },
            text = {
                Column {
                    Text("Key: ${config.key}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { 
                            editValue = it
                            inputError = false
                        },
                        label = { Text("Value") },
                        isError = inputError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    if (inputError) {
                        Text(
                            text = "Must be a valid number",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intVal = editValue.toIntOrNull()
                        if (intVal == null && editValue.isNotEmpty()) {
                            // basic MVP validation: expect simple integers mainly for timeouts 
                            // fallback for text-based if we must, but the prompt says 
                            // "MVP: use text fields validate basic type (int where needed)"
                            inputError = true
                        } else {
                            viewModel.updateConfig(config.key, editValue)
                            editingConfig = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingConfig = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "System Config",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadConfigs() }) { Text("Retry") }
                }
            }

            uiState.configs.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("No configurations found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.configs) { config ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = config.value,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Updated: ${config.updated_at ?: "Never"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { 
                                editingConfig = config
                                editValue = config.value
                                inputError = false
                            }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit Configuration")
                            }
                        }
                    }
                }
            }
        }
    }
}

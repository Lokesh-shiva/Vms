package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Trainer
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.TrainerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainersScreen(viewModel: TrainerViewModel, onBack: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<Trainer?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(Unit) { viewModel.loadTrainers() }

    if (uiState.showAddDialog) {
        TrainerFormDialog(
            title = "Add Coach",
            isSubmitting = uiState.isSubmitting,
            onConfirm = { name, bio, specialties, rate -> viewModel.addTrainer(name, bio, specialties, rate) },
            onDismiss = { viewModel.dismissAddDialog() },
        )
    }
    if (uiState.showEditDialog && uiState.editingTrainer != null) {
        val editing = uiState.editingTrainer!!
        TrainerFormDialog(
            title = "Edit Coach",
            initialName = editing.name,
            initialBio = editing.bio ?: "",
            initialSpecialties = editing.specialties ?: "",
            initialRate = editing.rate_per_session.toString(),
            isSubmitting = uiState.isSubmitting,
            onConfirm = { name, bio, specialties, rate -> viewModel.updateTrainer(editing.id, name, bio, specialties, rate) },
            onDismiss = { viewModel.dismissEditDialog() },
        )
    }
    deleteTarget?.let { trainer ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove Coach", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${trainer.name}\"? This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTrainer(trainer.id); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("Remove") }
            },
            dismissButton = { OutlinedButton(onClick = { deleteTarget = null }, shape = RoundedCornerShape(8.dp)) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                title = { Text("Coaches", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) { Icon(Icons.Default.Add, contentDescription = "Add Coach") }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading && uiState.trainers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                uiState.trainers.isEmpty() -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No coaches yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to add your first coach", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadTrainers() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.trainers, key = { it.id }) { trainer ->
                                TrainerCard(
                                    trainer = trainer,
                                    isUpdating = uiState.updatingIds.contains(trainer.id),
                                    onEdit = { viewModel.showEditDialog(trainer) },
                                    onDelete = { deleteTarget = trainer },
                                    onToggle = { isActive -> viewModel.toggleTrainer(trainer.id, isActive) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainerCard(trainer: Trainer, isUpdating: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trainer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!trainer.specialties.isNullOrBlank()) {
                    Text(trainer.specialties, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹${"%.2f".format(trainer.rate_per_session)}/session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Switch(
                checked = trainer.is_active,
                onCheckedChange = onToggle,
                enabled = !isUpdating,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(
                onClick = onDelete, shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("Delete", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TrainerFormDialog(
    title: String,
    initialName: String = "",
    initialBio: String = "",
    initialSpecialties: String = "",
    initialRate: String = "",
    isSubmitting: Boolean,
    onConfirm: (String, String, String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(initialBio) }
    var specialties by remember { mutableStateOf(initialSpecialties) }
    var rate by remember { mutableStateOf(initialRate) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val rateValue = rate.toDoubleOrNull()
        error = when {
            name.isBlank() -> "Name is required."
            rateValue == null || rateValue <= 0 -> "Enter a valid rate."
            else -> null
        }
        if (error == null) onConfirm(name.trim(), bio.trim(), specialties.trim(), rate.toDouble())
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = specialties, onValueChange = { specialties = it }, label = { Text("Specialties (comma-separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = rate, onValueChange = { rate = it }, label = { Text("Rate per session (₹)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = { submit() }, enabled = !isSubmitting, shape = RoundedCornerShape(8.dp)) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Save")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !isSubmitting, shape = RoundedCornerShape(8.dp)) { Text("Cancel") } },
    )
}

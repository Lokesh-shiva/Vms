package com.example.vmsadmin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Timeslot
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.TimeslotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeslotsScreen(viewModel: TimeslotViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadTimeslots()
        visible = true
    }

    // ── Add Timeslot Dialog ──────────────────────────────────────────
    if (uiState.showAddDialog) {
        TimeslotFormDialog(
            title = "Add Timeslot",
            initialStartTime = "",
            initialEndTime = "",
            initialCapacity = "",
            onConfirm = { startTime, endTime, capacity ->
                viewModel.addTimeslot(startTime, endTime, capacity)
            },
            onDismiss = { viewModel.dismissAddDialog() }
        )
    }

    // ── Edit Timeslot Dialog ─────────────────────────────────────────
    if (uiState.showEditDialog && uiState.editingTimeslot != null) {
        val ts = uiState.editingTimeslot!!
        TimeslotFormDialog(
            title = "Edit Timeslot",
            initialStartTime = ts.start_time,
            initialEndTime = ts.end_time,
            initialCapacity = ts.capacity.toString(),
            onConfirm = { startTime, endTime, capacity ->
                viewModel.updateTimeslot(ts.id, startTime, endTime, capacity)
            },
            onDismiss = { viewModel.dismissEditDialog() }
        )
    }

    // ── Delete Confirmation Dialog ───────────────────────────────────
    if (uiState.showDeleteConfirm && uiState.deletingTimeslot != null) {
        val ts = uiState.deletingTimeslot!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = {
                Text(
                    "Delete Timeslot",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete the timeslot \"${ts.start_time} - ${ts.end_time}\"? This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTimeslot(ts.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissDeleteConfirm() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Timeslots",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Timeslot")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.timeslots.isEmpty() -> {
                    // Skeleton Loader
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            AppCard {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(160.dp)
                                                .height(22.dp)
                                                .shimmerEffect()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(48.dp)
                                                .height(28.dp)
                                                .shimmerEffect()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(16.dp)
                                            .shimmerEffect()
                                    )
                                }
                            }
                        }
                    }
                }
                uiState.error != null && uiState.timeslots.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "Something went wrong",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTimeslots() }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.timeslots.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No timeslots configured",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add your first timeslot",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshTimeslots() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                uiState.timeslots,
                                key = { _, timeslot -> timeslot.id }
                            ) { index, timeslot ->
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(
                                        animationSpec = tween(
                                            400,
                                            delayMillis = index * 50
                                        )
                                    ) +
                                            slideInVertically(
                                                initialOffsetY = { 50 },
                                                animationSpec = tween(
                                                    400,
                                                    delayMillis = index * 50
                                                )
                                            )
                                ) {
                                    TimeslotCard(
                                        timeslot = timeslot,
                                        isUpdating = uiState.updatingTimeslotIds.contains(timeslot.id),
                                        onEdit = { viewModel.showEditDialog(timeslot) },
                                        onDelete = { viewModel.showDeleteConfirm(timeslot) },
                                        onToggle = { isActive ->
                                            viewModel.toggleTimeslot(timeslot.id, isActive)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeslotCard(
    timeslot: Timeslot,
    isUpdating: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${formatTime(timeslot.start_time)} – ${formatTime(timeslot.end_time)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Capacity: ${timeslot.capacity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (timeslot.is_active) "Active" else "Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (timeslot.is_active)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = timeslot.is_active,
                onCheckedChange = onToggle,
                enabled = !isUpdating,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDelete,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Formats a time string. If already in HH:mm format, returns as-is.
 * If it contains seconds (HH:mm:ss), strips the seconds.
 */
private fun formatTime(time: String): String {
    return try {
        val parts = time.split(":")
        if (parts.size >= 2) {
            "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
        } else {
            time
        }
    } catch (_: Exception) {
        time
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeslotFormDialog(
    title: String,
    initialStartTime: String,
    initialEndTime: String,
    initialCapacity: String,
    onConfirm: (startTime: String, endTime: String, capacity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    var capacityText by remember { mutableStateOf(initialCapacity) }
    var startTimeError by remember { mutableStateOf(false) }
    var endTimeError by remember { mutableStateOf(false) }
    var capacityError by remember { mutableStateOf(false) }
    var timeRangeError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        startTimeError = startTime.isBlank() || !isValidTimeFormat(startTime)
        endTimeError = endTime.isBlank() || !isValidTimeFormat(endTime)
        val cap = capacityText.toIntOrNull()
        capacityError = cap == null || cap <= 0
        timeRangeError = false
        if (!startTimeError && !endTimeError) {
            timeRangeError = endTime <= startTime
        }
        return !startTimeError && !endTimeError && !capacityError && !timeRangeError
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = {
                        startTime = it
                        startTimeError = false
                        timeRangeError = false
                    },
                    label = { Text("Start Time (HH:mm)") },
                    placeholder = { Text("09:00") },
                    singleLine = true,
                    isError = startTimeError,
                    supportingText = if (startTimeError) {
                        { Text("Enter a valid time (e.g. 09:00)") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = {
                        endTime = it
                        endTimeError = false
                        timeRangeError = false
                    },
                    label = { Text("End Time (HH:mm)") },
                    placeholder = { Text("10:00") },
                    singleLine = true,
                    isError = endTimeError || timeRangeError,
                    supportingText = if (endTimeError) {
                        { Text("Enter a valid time (e.g. 10:00)") }
                    } else if (timeRangeError) {
                        { Text("End time must be after start time") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = {
                        capacityText = it
                        capacityError = false
                    },
                    label = { Text("Capacity") },
                    placeholder = { Text("10") },
                    singleLine = true,
                    isError = capacityError,
                    supportingText = if (capacityError) {
                        { Text("Capacity must be a number greater than 0") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validate()) {
                        onConfirm(startTime.trim(), endTime.trim(), capacityText.trim().toInt())
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Simple time format validation for HH:mm pattern.
 */
private fun isValidTimeFormat(time: String): Boolean {
    val regex = Regex("""^\d{1,2}:\d{2}$""")
    if (!regex.matches(time)) return false
    val parts = time.split(":")
    val hour = parts[0].toIntOrNull() ?: return false
    val minute = parts[1].toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59
}

package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.CreateTournamentRequest
import com.example.vmsadmin.models.Tournament
import com.example.vmsadmin.viewmodel.TournamentViewModel
import com.example.vmsadmin.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentsScreen(
    viewModel: TournamentViewModel,
    onBack: () -> Unit = {},
    onOpenDetail: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showCreateDialog) {
        CreateTournamentDialog(
            onConfirm = { request ->
                viewModel.createTournament(request)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Tournaments", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "Create Tournament")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingTournaments(Modifier.padding(padding))
            uiState.error != null -> TournamentErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadTournaments() },
                modifier = Modifier.padding(padding)
            )
            uiState.tournaments.isEmpty() -> EmptyTournaments(Modifier.padding(padding))
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshTournaments() },
                    modifier = Modifier.padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.tournaments, key = { it.id }) { tournament ->
                            TournamentCard(
                                tournament = tournament,
                                isUpdating = tournament.id in uiState.updatingIds,
                                onStatusChange = { newStatus -> viewModel.updateStatus(tournament.id, newStatus) },
                                onOpenDetail = { viewModel.selectTournament(tournament); onOpenDetail() },
                                onDelete = { viewModel.deleteTournament(tournament.id) },
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentCard(
    tournament: Tournament,
    isUpdating: Boolean,
    onStatusChange: (String) -> Unit,
    onOpenDetail: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete tournament") },
            text = { Text("Delete \"${tournament.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    AppCard(onClick = onOpenDetail) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tournament.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tournament.organizer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${tournament.start_date} → ${tournament.end_date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Max teams: ${tournament.max_teams}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TournamentStatusBadge(tournament.status)
                    if (isUpdating) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    listOf("UPCOMING", "ONGOING", "COMPLETED", "CANCELLED").forEach { status ->
                        if (status != tournament.status) {
                            DropdownMenuItem(
                                text = { Text("Set $status") },
                                onClick = {
                                    onStatusChange(status)
                                    showMenu = false
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showDeleteConfirm = true; showMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TournamentStatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "UPCOMING"  -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary
        "ONGOING"   -> Color(0xFF1B5E20).copy(alpha = 0.12f) to Color(0xFF2E7D32)
        "COMPLETED" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "CANCELLED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f) to MaterialTheme.colorScheme.error
        else        -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CreateTournamentDialog(
    onConfirm: (CreateTournamentRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var maxTeams by remember { mutableStateOf("8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tournament") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tournament name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = organizer,
                    onValueChange = { organizer = it },
                    label = { Text("Organizer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxTeams,
                    onValueChange = { maxTeams = it },
                    label = { Text("Max teams") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CreateTournamentRequest(
                            name = name.trim(),
                            organizer = organizer.trim(),
                            start_date = startDate.trim(),
                            end_date = endDate.trim(),
                            max_teams = maxTeams.toIntOrNull() ?: 8
                        )
                    )
                },
                enabled = name.isNotBlank() && organizer.isNotBlank() &&
                        startDate.isNotBlank() && endDate.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LoadingTournaments(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(5) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.55f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                    Box(
                        Modifier
                            .height(12.dp)
                            .fillMaxWidth(0.4f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                    Box(
                        Modifier
                            .height(10.dp)
                            .fillMaxWidth(0.6f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTournaments(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No tournaments yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap + to create one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TournamentErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

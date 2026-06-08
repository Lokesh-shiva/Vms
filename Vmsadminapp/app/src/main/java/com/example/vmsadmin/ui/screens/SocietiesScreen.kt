package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.CreateSocietyRequest
import com.example.vmsadmin.models.Society
import com.example.vmsadmin.models.SocietyLeaderboardEntry
import com.example.vmsadmin.models.SocietyMember
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.SocietyViewModel

private val DEACTIVATE_ROLES = setOf("super_admin", "ops_manager")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocietiesScreen(
    viewModel: SocietyViewModel,
    currentUserRole: String,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Bottom sheet for selected society detail
    if (uiState.selectedSociety != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearDetail() }
        ) {
            SocietyDetailSheet(
                society = uiState.selectedSociety!!,
                members = uiState.members,
                leaderboard = uiState.leaderboard,
                isLoading = uiState.isDetailLoading
            )
        }
    }

    if (showCreateDialog) {
        CreateSocietyDialog(
            isCreating = uiState.isCreating,
            onConfirm = { request ->
                viewModel.createSociety(request)
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
                    Text("Societies", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                Icon(Icons.Outlined.Add, contentDescription = "Create Society")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingSocieties(Modifier.padding(padding))
            uiState.error != null && uiState.societies.isEmpty() -> SocietyErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadSocieties() },
                modifier = Modifier.padding(padding)
            )
            uiState.societies.isEmpty() -> EmptySocieties(Modifier.padding(padding))
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.societies, key = { it.id }) { society ->
                            SocietyCard(
                                society = society,
                                currentUserRole = currentUserRole,
                                isActionInProgress = society.id in uiState.actionInProgressIds,
                                onViewDetail = { viewModel.loadDetail(society) },
                                onDeactivate = { viewModel.deactivateSociety(society.id) },
                                onDelete = { viewModel.deleteSociety(society.id) }
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocietyCard(
    society: Society,
    currentUserRole: String,
    isActionInProgress: Boolean,
    onViewDetail: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Society") },
            text = { Text("Are you sure you want to delete \"${society.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = society.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Region #${society.region_id} • Sport #${society.sport_id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${society.max_members} max members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SocietyStatusBadge(isActive = society.is_active)
                    SocietyVisibilityBadge(isPublic = society.is_public)
                    if (isActionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("View Members & Leaderboard") },
                        onClick = {
                            onViewDetail()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.People, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    if (society.is_active && currentUserRole in DEACTIVATE_ROLES) {
                        DropdownMenuItem(
                            text = { Text("Deactivate") },
                            onClick = {
                                onDeactivate()
                                showMenu = false
                            }
                        )
                    }
                    if (currentUserRole == "super_admin") {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocietyStatusBadge(isActive: Boolean) {
    val (bg, fg, label) = if (isActive) {
        Triple(
            Color(0xFF1B5E20).copy(alpha = 0.12f),
            Color(0xFF2E7D32),
            "Active"
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error,
            "Inactive"
        )
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SocietyVisibilityBadge(isPublic: Boolean) {
    val (bg, fg, label) = if (isPublic) {
        Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.primary,
            "Public"
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Private"
        )
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SocietyDetailSheet(
    society: Society,
    members: List<SocietyMember>,
    leaderboard: List<SocietyLeaderboardEntry>,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = society.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        if (society.description != null) {
            Text(
                text = society.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Members section
            Text(
                text = "Members (${members.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            if (members.isEmpty()) {
                Text(
                    text = "No members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                members.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "User #${member.user_id}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        MemberRoleBadge(role = member.role)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Leaderboard section
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            if (leaderboard.isEmpty()) {
                Text(
                    text = "No leaderboard data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                leaderboard.take(10).forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = "User #${entry.user_id}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${entry.total_points} pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${entry.matches_played}M",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MemberRoleBadge(role: String) {
    val (bg, fg) = when (role.uppercase()) {
        "OWNER" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary
        else    -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = role.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LoadingSocieties(modifier: Modifier = Modifier) {
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
private fun EmptySocieties(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No societies yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap + to create a society",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SocietyErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun CreateSocietyDialog(
    isCreating: Boolean,
    onConfirm: (CreateSocietyRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var regionId by remember { mutableStateOf("") }
    var sportId by remember { mutableStateOf("") }
    var maxMembers by remember { mutableStateOf("50") }
    var isPublic by remember { mutableStateOf(true) }
    var ownerUserId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Society") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = regionId,
                    onValueChange = { regionId = it },
                    label = { Text("Region ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = sportId,
                    onValueChange = { sportId = it },
                    label = { Text("Sport ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = maxMembers,
                    onValueChange = { maxMembers = it },
                    label = { Text("Max members") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = ownerUserId,
                    onValueChange = { ownerUserId = it },
                    label = { Text("Owner User ID (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Public society", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CreateSocietyRequest(
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            region_id = regionId.toIntOrNull() ?: 0,
                            sport_id = sportId.toIntOrNull() ?: 0,
                            max_members = maxMembers.toIntOrNull() ?: 50,
                            is_public = isPublic,
                            owner_user_id = ownerUserId.toIntOrNull()
                        )
                    )
                },
                enabled = name.isNotBlank() && regionId.isNotBlank() && sportId.isNotBlank() && !isCreating
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

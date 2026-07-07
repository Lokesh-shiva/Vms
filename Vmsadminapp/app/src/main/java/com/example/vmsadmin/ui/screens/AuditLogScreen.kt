package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.AuditLogEntry
import com.example.vmsadmin.viewmodel.AuditLogFilters
import com.example.vmsadmin.viewmodel.AuditLogViewModel
import com.example.vmsadmin.ui.components.AppCard

private val SENSITIVE_ACTIONS = setOf(
    "ROLE_CHANGE", "REFUND", "PAYMENT_REFUNDED", "DEACTIVATION",
    "GROUND_DELETED", "TOURNAMENT_DELETED", "BOOKING_CANCELLED",
)

private val KNOWN_ACTIONS = listOf(
    "BOOKING_CANCELLED", "CAPTAIN_PAYOUT", "CAPTAIN_REVIEWED", "CAPTAIN_STATUS_CHANGE",
    "DEACTIVATION", "DISPUTE_RESOLVED", "GROUND_CREATED", "GROUND_DELETED", "GROUND_UPDATED",
    "PAYMENT_APPROVED", "PAYMENT_REFUNDED", "PAYMENT_REJECTED", "REFUND", "ROLE_CHANGE",
    "TOURNAMENT_CREATED", "TOURNAMENT_DELETED", "TOURNAMENT_MATCH_RESULT_RECORDED",
    "TOURNAMENT_UPDATED", "USER_CREATED",
)

private val KNOWN_RESOURCE_TYPES = listOf(
    "booking", "captain", "dispute", "ground", "payment", "tournament", "tournament_match", "user",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(viewModel: AuditLogViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Trigger pagination when the user scrolls near the end of the list.
    LaunchedEffect(listState, uiState.logs.size) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.logs.size - 5
        }.collect { nearEnd ->
            if (nearEnd) viewModel.loadMore()
        }
    }

    if (showFilterSheet) {
        AuditLogFilterSheet(
            current = uiState.filters,
            onApply = { viewModel.applyFilters(it); showFilterSheet = false },
            onDismiss = { showFilterSheet = false },
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Audit Log",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    BadgedBox(
                        badge = { if (uiState.filters.isActive) Badge() }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "Filter")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> AuditLogSkeleton(Modifier.padding(padding))
            uiState.error != null -> AuditLogErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadLogs() },
                modifier = Modifier.padding(padding)
            )
            uiState.logs.isEmpty() -> AuditLogEmptyState(
                hasActiveFilters = uiState.filters.isActive,
                onClearFilters = { viewModel.clearFilters() },
                modifier = Modifier.padding(padding),
            )
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.logs, key = { it.id }) { entry ->
                            AuditLogCard(entry)
                        }
                        if (uiState.isLoadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuditLogFilterSheet(
    current: AuditLogFilters,
    onApply: (AuditLogFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    var action by remember { mutableStateOf(current.action) }
    var resourceType by remember { mutableStateOf(current.targetResourceType) }
    var actorUserId by remember { mutableStateOf(current.actorUserId?.toString() ?: "") }
    var startDate by remember { mutableStateOf(current.startDate ?: "") }
    var endDate by remember { mutableStateOf(current.endDate ?: "") }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var resourceMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Filter Audit Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(expanded = actionMenuExpanded, onExpandedChange = { actionMenuExpanded = it }) {
                OutlinedTextField(
                    value = action ?: "Any action",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Action") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = actionMenuExpanded, onDismissRequest = { actionMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Any action") }, onClick = { action = null; actionMenuExpanded = false })
                    KNOWN_ACTIONS.forEach { a ->
                        DropdownMenuItem(text = { Text(a) }, onClick = { action = a; actionMenuExpanded = false })
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = resourceMenuExpanded, onExpandedChange = { resourceMenuExpanded = it }) {
                OutlinedTextField(
                    value = resourceType ?: "Any resource type",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Resource type") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = resourceMenuExpanded, onDismissRequest = { resourceMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Any resource type") }, onClick = { resourceType = null; resourceMenuExpanded = false })
                    KNOWN_RESOURCE_TYPES.forEach { r ->
                        DropdownMenuItem(text = { Text(r) }, onClick = { resourceType = r; resourceMenuExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = actorUserId,
                onValueChange = { actorUserId = it.filter(Char::isDigit) },
                label = { Text("Actor user ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("From (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("To (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        action = null; resourceType = null; actorUserId = ""; startDate = ""; endDate = ""
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Clear") }

                Button(
                    onClick = {
                        onApply(
                            AuditLogFilters(
                                action = action,
                                actorUserId = actorUserId.toIntOrNull(),
                                targetResourceType = resourceType,
                                startDate = startDate.trim().ifBlank { null },
                                endDate = endDate.trim().ifBlank { null },
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun AuditLogCard(entry: AuditLogEntry) {
    val actionColor = if (entry.action in SENSITIVE_ACTIONS) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = entry.action,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = actionColor
            )

            if (entry.created_at != null) {
                Text(
                    text = entry.created_at,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (entry.actor_user_id != null) {
                Text(
                    text = "by User #${entry.actor_user_id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (entry.target_resource_type != null) {
                val targetText = buildString {
                    append("→ ${entry.target_resource_type}")
                    if (entry.target_resource_id != null) append(" #${entry.target_resource_id}")
                }
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!entry.details.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AuditLogSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(6) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.45f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                    Box(
                        Modifier
                            .height(12.dp)
                            .fillMaxWidth(0.35f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                    Box(
                        Modifier
                            .height(12.dp)
                            .fillMaxWidth(0.55f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditLogEmptyState(
    hasActiveFilters: Boolean = false,
    onClearFilters: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (hasActiveFilters) "No entries match these filters" else "No audit entries found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasActiveFilters) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onClearFilters) { Text("Clear filters") }
            }
        }
    }
}

@Composable
private fun AuditLogErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

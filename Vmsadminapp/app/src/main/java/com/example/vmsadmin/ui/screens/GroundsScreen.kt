package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.GroundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroundsScreen(
    viewModel: GroundViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Grounds",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.grounds.isEmpty() -> {
                    // Shimmer skeleton
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            AppCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.width(160.dp).height(20.dp).shimmerEffect())
                                    Box(modifier = Modifier.width(50.dp).height(28.dp).shimmerEffect())
                                }
                                Spacer(Modifier.height(10.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).shimmerEffect())
                                Spacer(Modifier.height(6.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).shimmerEffect())
                            }
                        }
                    }
                }

                uiState.error != null && uiState.grounds.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            uiState.error ?: "Something went wrong",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadGrounds() }) { Text("Retry") }
                    }
                }

                uiState.grounds.isEmpty() -> {
                    Text(
                        "No grounds configured.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshGrounds() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.grounds, key = { it.id }) { ground ->
                                GroundCard(
                                    ground = ground,
                                    isUpdating = uiState.updatingIds.contains(ground.id),
                                    onToggle = { isActive -> viewModel.toggleGround(ground.id, isActive) }
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
private fun GroundCard(
    ground: Ground,
    isUpdating: Boolean,
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
                    text = ground.name.ifBlank { "Ground #${ground.id}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                StatusBadge(status = ground.status)
            }
            Switch(
                checked = ground.is_active,
                onCheckedChange = onToggle,
                enabled = !isUpdating
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Sport ID: ${ground.sport_id}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Location ID: ${ground.location_id}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

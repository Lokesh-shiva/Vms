package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.QueueOverviewViewModel
import com.example.vmsadmin.viewmodel.RegionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueOverviewScreen(
    viewModel: QueueOverviewViewModel,
    cartTypeViewModel: CartTypeViewModel,
    regionViewModel: RegionViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartTypeState by cartTypeViewModel.uiState.collectAsState()
    val regionState by regionViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
        if (cartTypeState.cartTypes.isEmpty()) cartTypeViewModel.loadCartTypes()
        if (regionState.regions.isEmpty()) regionViewModel.loadRegions()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Queue Overview",
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
                    Button(onClick = { viewModel.loadStats() }) { Text("Retry") }
                }
            }

            uiState.stats.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("No active queues.", color = MaterialTheme.colorScheme.onSurfaceVariant) }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.stats) { stat ->
                    val sportName = cartTypeState.cartTypes.find { it.id == stat.sport_id }?.name ?: "Sport #${stat.sport_id}"
                    val regionName = regionState.regions.find { it.id == stat.region_id }?.name ?: "Region #${stat.region_id}"

                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = sportName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = regionName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${stat.count}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Waiting",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

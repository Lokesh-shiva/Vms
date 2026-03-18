package com.example.vmsuser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsuser.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToBooking: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHome()
    }

    Scaffold { innerPadding ->
        when {
            // ── Loading ──────────────────────────────────────────────────
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // ── Error ────────────────────────────────────────────────────
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "An error occurred",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadHome() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            // ── Content ──────────────────────────────────────────────────
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // ① Header
                    item {
                        Text(
                            text = "Hi 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }

                    // ② Cart Type chips (horizontal row)
                    if (uiState.cartTypes.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                items(
                                    items = uiState.cartTypes,
                                    key = { it.id }
                                ) { cartType ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(cartType.name) }
                                    )
                                }
                            }
                        }
                    }

                    // ③ Items grouped by cart type
                    // Sort cart types that have items first
                    val cartTypesWithItems = uiState.cartTypes.filter {
                        uiState.groupedItems.containsKey(it.id)
                    }

                    cartTypesWithItems.forEach { cartType ->
                        val sectionItems = uiState.groupedItems[cartType.id] ?: return@forEach

                        // Section header
                        item(key = "header_${cartType.id}") {
                            Text(
                                text = cartType.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(
                                    start = 16.dp, end = 16.dp,
                                    top = 16.dp, bottom = 8.dp
                                )
                            )
                        }

                        // Item cards (flat, no nested LazyColumn)
                        items(
                            items = sectionItems,
                            key = { "item_${it.id}" }
                        ) { item ->
                            ItemCard(
                                item = item,
                                quantity = uiState.cart[item.id] ?: 0,
                                onAdd = { viewModel.addItem(item.id) },
                                onRemove = { viewModel.removeItem(item.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Empty state
                    if (cartTypesWithItems.isEmpty() && !uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No items available right now.",
                                    style = MaterialTheme.typography.bodyMedium,
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

package com.example.vmsadmin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Admin Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Text(
                text = uiState.error ?: "Error loading dashboard",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.loadDashboard() }) {
                Text("Retry")
            }
        } else {
            // Using AnimatedVisibility for staggered entry effect
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(500)),
                modifier = Modifier.weight(1f)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DashboardCard(
                            title = "Active Bookings",
                            count = "${uiState.activeBookings}",
                            icon = Icons.AutoMirrored.Outlined.List,
                            iconTint = Color(0xFF2196F3)
                        )
                    }
                    item {
                        DashboardCard(
                            title = "Payments Review",
                            count = "${uiState.pendingPayments}",
                            icon = Icons.Outlined.Warning,
                            iconTint = Color(0xFFFF9800)
                        )
                    }
                    item {
                        DashboardCard(
                            title = "Completed Today",
                            count = "${uiState.completedBookings}",
                            icon = Icons.Outlined.Done,
                            iconTint = Color(0xFF4CAF50)
                        )
                    }
                    item {
                        DashboardCard(
                            title = "Total Bookings",
                            count = "${uiState.totalBookings}",
                            icon = Icons.Outlined.ShoppingCart,
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color = MaterialTheme.colorScheme.primary) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                count,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

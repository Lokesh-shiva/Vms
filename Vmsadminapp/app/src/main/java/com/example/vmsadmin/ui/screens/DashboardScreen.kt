package com.example.vmsadmin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ShoppingCart
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
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(500))
            ) {
                Column {
                    DashboardCard(
                        title = "Active Bookings",
                        count = "${uiState.activeBookings}",
                        icon = Icons.Default.List,
                        iconTint = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DashboardCard(
                        title = "Payments Under Review",
                        count = "${uiState.pendingPayments}",
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DashboardCard(
                        title = "Completed Bookings Today",
                        count = "${uiState.completedBookings}",
                        icon = Icons.Default.Done,
                        iconTint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DashboardCard(
                        title = "Total Bookings (All time)",
                        count = "${uiState.totalBookings}",
                        icon = Icons.Default.ShoppingCart,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color = MaterialTheme.colorScheme.primary) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    count,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

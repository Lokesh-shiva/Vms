package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    onNavigateToRegions: () -> Unit = {},
    onNavigateToCartTypes: () -> Unit = {},
    onNavigateToTimeslots: () -> Unit = {},
    onNavigateToCarts: () -> Unit = {},
    onNavigateToFeeConfig: () -> Unit = {},
    onNavigateToItems: () -> Unit = {},
    onNavigateToMatches: () -> Unit = {},
    onNavigateToGrounds: () -> Unit = {}
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ManageCard(
                    title = "Regions",
                    subtitle = "Manage service regions and availability",
                    icon = Icons.Outlined.LocationOn,
                    enabled = true,
                    onClick = onNavigateToRegions
                )
            }
            item {
                ManageCard(
                    title = "Cart Types",
                    subtitle = "Configure cart categories and pricing",
                    icon = Icons.Outlined.ShoppingCart,
                    enabled = true,
                    onClick = onNavigateToCartTypes
                )
            }
            item {
                ManageCard(
                    title = "Timeslots",
                    subtitle = "Set up available booking timeslots",
                    icon = Icons.Outlined.DateRange,
                    enabled = true,
                    onClick = onNavigateToTimeslots
                )
            }
            item {
                ManageCard(
                    title = "Carts",
                    subtitle = "Manage cart inventory and assignments",
                    icon = Icons.Outlined.Build,
                    enabled = true,
                    onClick = onNavigateToCarts
                )
            }
            item {
                ManageCard(
                    title = "Fee Configuration",
                    subtitle = "Set fees per region and cart type",
                    icon = Icons.Outlined.Info,
                    enabled = true,
                    onClick = onNavigateToFeeConfig
                )
            }
            item {
                ManageCard(
                    title = "Items",
                    subtitle = "Manage menu items by cart type",
                    icon = Icons.Outlined.List,
                    enabled = true,
                    onClick = onNavigateToItems
                )
            }
            item {
                ManageCard(
                    title = "Grounds",
                    subtitle = "View and manage sports grounds",
                    icon = Icons.Outlined.LocationOn,
                    enabled = true,
                    onClick = onNavigateToGrounds
                )
            }
            item {
                ManageCard(
                    title = "Matches",
                    subtitle = "View and manage sports matches",
                    icon = Icons.Outlined.DateRange,
                    enabled = true,
                    onClick = onNavigateToMatches
                )
            }
        }
    }
}

@Composable
private fun ManageCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit = {}
) {
    AppCard(
        onClick = if (enabled) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (enabled)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (enabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 0.7f else 0.4f
                    )
                )
            }
            if (!enabled) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Soon",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

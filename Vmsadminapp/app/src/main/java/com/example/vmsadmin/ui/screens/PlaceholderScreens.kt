package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    onNavigateToRegions: () -> Unit = {},
    onNavigateToSports: () -> Unit = {},
    onNavigateToTimeslots: () -> Unit = {},
    onNavigateToFeeConfig: () -> Unit = {},
    onNavigateToItems: () -> Unit = {},
    onNavigateToMatches: () -> Unit = {},
    onNavigateToGrounds: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToSystemConfig: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    onNavigateToCaptains: () -> Unit = {},
    onNavigateToTournaments: () -> Unit = {},
    onNavigateToVoteRounds: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToTrainers: () -> Unit = {},
    onNavigateToTrainerBookings: () -> Unit = {},
    onNavigateToDisputes: () -> Unit = {},
    onNavigateToAuditLog: () -> Unit = {},
    onNavigateToSocieties: () -> Unit = {},
    role: String = ""
) {
    val isOpsOrAbove = role == "super_admin" || role == "ops_manager"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Manage", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Operations ─────────────────────────────────────────────
            item { SectionHeader("Operations") }
            item { ManageCard("Regions", "Service regions and coverage areas", Icons.Outlined.LocationOn, onNavigateToRegions) }
            item { ManageCard("Timeslots", "Available booking time windows", Icons.Outlined.DateRange, onNavigateToTimeslots) }
            if (isOpsOrAbove) {
                item { ManageCard("Live Queue", "Monitor active cart queues in real time", Icons.AutoMirrored.Outlined.List, onNavigateToQueue) }
                item { ManageCard("Captains", "Ground captains and assignments", Icons.Outlined.Person, onNavigateToCaptains) }
            }

            // ── Catalogue ──────────────────────────────────────────────
            item { SectionHeader("Catalogue") }
            item { ManageCard("Sport Types", "Sport categories and cart configurations", Icons.Outlined.Star, onNavigateToSports) }
            item { ManageCard("Pricing", "Fees per region and sport type", Icons.Outlined.Info, onNavigateToFeeConfig) }
            item { ManageCard("Menu Items", "Add-ons and extras by sport", Icons.AutoMirrored.Outlined.List, onNavigateToItems) }

            // ── Venues & Matches ───────────────────────────────────────
            item { SectionHeader("Venues & Matches") }
            item { ManageCard("Grounds", "Sports grounds and facilities", Icons.Outlined.LocationOn, onNavigateToGrounds) }
            item { ManageCard("Matches", "Scheduled and past matches", Icons.Outlined.DateRange, onNavigateToMatches) }
            item { ManageCard("Tournaments", "Tournament scheduling and status", Icons.Outlined.Star, onNavigateToTournaments) }
            item { ManageCard("Sport Voting", "Vote round options, deadline, and results", Icons.Outlined.Flag, onNavigateToVoteRounds) }
            item { ManageCard("Societies", "Player groups and community clubs", Icons.Outlined.People, onNavigateToSocieties) }
            item { ManageCard("Shop Orders", "Review and approve shop payment submissions", Icons.Outlined.ShoppingCart, onNavigateToOrders) }
            item { ManageCard("Coaches", "Trainer/coach profiles and rates", Icons.Outlined.Person, onNavigateToTrainers) }
            item { ManageCard("Coach Bookings", "Review and confirm session payment submissions", Icons.Outlined.DateRange, onNavigateToTrainerBookings) }
            item { ManageCard("Disputes", "Support tickets and dispute resolution", Icons.Outlined.Flag, onNavigateToDisputes) }

            // ── Admin ──────────────────────────────────────────────────
            if (role == "super_admin") {
                item { SectionHeader("Admin") }
                item { ManageCard("Users", "User roles and account access", Icons.Outlined.Person, onNavigateToUsers) }
                item { ManageCard("System Settings", "Platform-wide configuration", Icons.Outlined.Settings, onNavigateToSystemConfig) }
                item { ManageCard("Audit Log", "Full audit trail of admin actions", Icons.AutoMirrored.Outlined.List, onNavigateToAuditLog) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun ManageCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

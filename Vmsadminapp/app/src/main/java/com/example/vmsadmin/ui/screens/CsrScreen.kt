package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Tournament
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.TournamentViewModel

/**
 * Read-only panel for the CSR_PARTNER role: shows only tournaments this
 * partner is assigned as sponsor for (Tournament.sponsor_user_id ==
 * current_user.id, enforced server-side by GET /tournaments/csr/mine).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsrScreen(tournamentViewModel: TournamentViewModel) {
    val uiState by tournamentViewModel.uiState.collectAsState()
    val tournaments = uiState.sponsoredTournaments
    val isLoading = uiState.sponsoredLoading
    val error = uiState.sponsoredError

    LaunchedEffect(Unit) { tournamentViewModel.loadMySponsoredTournaments() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("CSR Partner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when {
            isLoading && tournaments.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            error != null && tournaments.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { tournamentViewModel.loadMySponsoredTournaments() }) { Text("Retry") }
                }
            }

            tournaments.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No sponsored tournaments yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Ask an admin to assign you as sponsor on a tournament.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "My Sponsored Tournaments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(tournaments, key = { it.id }) { tournament -> CsrTournamentCard(tournament) }
            }
        }
    }
}

@Composable
private fun CsrTournamentCard(tournament: Tournament) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tournament.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${tournament.start_date} → ${tournament.end_date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${tournament.registered_teams ?: 0} registered · max ${tournament.max_teams}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (tournament.prize_pool.isNotBlank()) {
                    Text(
                        text = "Prize pool: ${tournament.prize_pool}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TournamentStatusBadge(tournament.status)
        }
    }
}

@Composable
private fun TournamentStatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "ONGOING" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary
        "COMPLETED" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "CANCELLED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f) to MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.secondary
    }
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp), color = bg) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

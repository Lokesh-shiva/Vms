package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.CreateTournamentMatchRequest
import com.example.vmsadmin.models.TournamentMatch
import com.example.vmsadmin.models.TournamentRegistration
import com.example.vmsadmin.models.TournamentStanding
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.TournamentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    viewModel: TournamentViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val tournament = uiState.selectedTournament
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var resultTarget by remember { mutableStateOf<TournamentMatch?>(null) }

    LaunchedEffect(uiState.detailError) {
        uiState.detailError?.let { snackbarHostState.showSnackbar(it); viewModel.clearDetailError() }
    }

    if (tournament == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tournament selected.")
        }
        return
    }

    val isTeamTournament = tournament.participant_type == "TEAM"

    fun participantName(userId: Int?, teamId: Int?): String {
        if (teamId != null) {
            return uiState.registrations.find { it.team_id == teamId }?.team_name ?: "Team #$teamId"
        }
        if (userId != null) {
            return uiState.registrations.find { it.user_id == userId }?.name ?: "Player #$userId"
        }
        return "TBD"
    }

    if (showScheduleDialog) {
        ScheduleMatchDialog(
            registrations = uiState.registrations,
            isTeamTournament = isTeamTournament,
            scheduling = uiState.schedulingMatch,
            onConfirm = { request -> viewModel.scheduleMatch(tournament.id, request); showScheduleDialog = false },
            onDismiss = { showScheduleDialog = false },
        )
    }

    resultTarget?.let { match ->
        RecordResultDialog(
            homeLabel = participantName(match.home_user_id, match.home_team_id),
            awayLabel = participantName(match.away_user_id, match.away_team_id),
            recording = uiState.recordingResultForMatchId == match.id,
            onConfirm = { home, away -> viewModel.recordMatchResult(tournament.id, match.id, home, away); resultTarget = null },
            onDismiss = { resultTarget = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tournament.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showScheduleDialog = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Schedule match")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Matches") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Standings") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Registrations") })
            }
            when {
                uiState.detailLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> when (selectedTab) {
                    0 -> MatchesTab(uiState.matches, ::participantName, onRecordResult = { resultTarget = it })
                    1 -> StandingsTab(uiState.standings, ::participantName)
                    2 -> RegistrationsTab(uiState.registrations, isTeamTournament)
                }
            }
        }
    }
}

@Composable
private fun MatchesTab(matches: List<TournamentMatch>, participantName: (Int?, Int?) -> String, onRecordResult: (TournamentMatch) -> Unit) {
    if (matches.isEmpty()) {
        EmptyState("No matches scheduled yet.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(matches, key = { it.id }) { match ->
            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Round ${match.round}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val home = participantName(match.home_user_id, match.home_team_id)
                        val away = participantName(match.away_user_id, match.away_team_id)
                        if (match.status == "COMPLETED") {
                            Text("$home ${match.home_score} — ${match.away_score} $away", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("$home vs $away", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        MatchStatusBadge(match.status)
                    }
                    if (match.status != "COMPLETED" && match.status != "CANCELLED") {
                        Button(onClick = { onRecordResult(match) }) { Text("Record Result") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsTab(standings: List<TournamentStanding>, participantName: (Int?, Int?) -> String) {
    if (standings.isEmpty()) {
        EmptyState("No standings yet — record a match result first.")
        return
    }
    val sorted = standings.sortedBy { it.rank ?: Int.MAX_VALUE }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text("#", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Player/Team", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("P", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Pts", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(sorted, key = { it.id }) { s ->
            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.rank ?: "—"}", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(participantName(s.user_id, s.team_id), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("${s.won}W ${s.drawn}D ${s.lost}L", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${s.played}", modifier = Modifier.width(28.dp))
                    Text("${s.points}", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun RegistrationsTab(registrations: List<TournamentRegistration>, isTeamTournament: Boolean) {
    if (registrations.isEmpty()) {
        EmptyState("No one has registered yet.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(registrations, key = { it.team_id ?: it.user_id ?: 0 }) { reg ->
            AppCard {
                if (isTeamTournament) {
                    Text(reg.team_name ?: "Unnamed team", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Captain: ${reg.captain_name ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    reg.members?.let { members ->
                        Spacer(Modifier.height(4.dp))
                        Text(members.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text(reg.name ?: "Unknown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MatchStatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "COMPLETED" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary
        "CANCELLED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f) to MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleMatchDialog(
    registrations: List<TournamentRegistration>,
    isTeamTournament: Boolean,
    scheduling: Boolean,
    onConfirm: (CreateTournamentMatchRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var homeExpanded by remember { mutableStateOf(false) }
    var awayExpanded by remember { mutableStateOf(false) }
    var homeSelection by remember { mutableStateOf<TournamentRegistration?>(null) }
    var awaySelection by remember { mutableStateOf<TournamentRegistration?>(null) }
    var round by remember { mutableStateOf("1") }

    fun label(r: TournamentRegistration?): String =
        if (r == null) "Select" else if (isTeamTournament) r.team_name ?: "Team" else r.name ?: "Player"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Match") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = homeExpanded, onExpandedChange = { homeExpanded = it }) {
                    OutlinedTextField(
                        value = label(homeSelection), onValueChange = {}, readOnly = true,
                        label = { Text("Home") }, modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(homeExpanded) }
                    )
                    ExposedDropdownMenu(expanded = homeExpanded, onDismissRequest = { homeExpanded = false }) {
                        registrations.forEach { r ->
                            DropdownMenuItem(text = { Text(label(r)) }, onClick = { homeSelection = r; homeExpanded = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = awayExpanded, onExpandedChange = { awayExpanded = it }) {
                    OutlinedTextField(
                        value = label(awaySelection), onValueChange = {}, readOnly = true,
                        label = { Text("Away") }, modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(awayExpanded) }
                    )
                    ExposedDropdownMenu(expanded = awayExpanded, onDismissRequest = { awayExpanded = false }) {
                        registrations.forEach { r ->
                            DropdownMenuItem(text = { Text(label(r)) }, onClick = { awaySelection = r; awayExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = round, onValueChange = { round = it }, label = { Text("Round") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        if (isTeamTournament) CreateTournamentMatchRequest(
                            round = round.toIntOrNull() ?: 1,
                            home_team_id = homeSelection?.team_id,
                            away_team_id = awaySelection?.team_id,
                        ) else CreateTournamentMatchRequest(
                            round = round.toIntOrNull() ?: 1,
                            home_user_id = homeSelection?.user_id,
                            away_user_id = awaySelection?.user_id,
                        )
                    )
                },
                enabled = homeSelection != null && awaySelection != null && homeSelection != awaySelection && !scheduling
            ) { Text(if (scheduling) "Scheduling…" else "Schedule") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !scheduling) { Text("Cancel") } }
    )
}

@Composable
private fun RecordResultDialog(
    homeLabel: String,
    awayLabel: String,
    recording: Boolean,
    onConfirm: (homeScore: Int, awayScore: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var homeScore by remember { mutableStateOf("") }
    var awayScore by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Result") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = homeScore, onValueChange = { homeScore = it }, label = { Text(homeLabel) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("—")
                OutlinedTextField(
                    value = awayScore, onValueChange = { awayScore = it }, label = { Text(awayLabel) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(homeScore.toIntOrNull() ?: 0, awayScore.toIntOrNull() ?: 0) },
                enabled = homeScore.toIntOrNull() != null && awayScore.toIntOrNull() != null && !recording
            ) { Text(if (recording) "Saving…" else "Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !recording) { Text("Cancel") } }
    )
}

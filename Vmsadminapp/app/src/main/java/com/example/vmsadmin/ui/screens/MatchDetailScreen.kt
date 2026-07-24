package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.AdminChatMessage
import com.example.vmsadmin.models.MatchPlayerInfo
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.viewmodel.MatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    viewModel: MatchViewModel,
    matchId: Int,
    onBack: () -> Unit,
) {
    val detail by viewModel.matchDetail.collectAsState()
    val detailLoading by viewModel.detailLoading.collectAsState()
    val detailError by viewModel.detailError.collectAsState()
    val messages by viewModel.matchMessages.collectAsState()

    LaunchedEffect(matchId) { viewModel.openMatchDetail(matchId) }
    DisposableEffect(matchId) { onDispose { viewModel.stopMatchDetailPolling() } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Match #$matchId", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when {
            detailLoading && detail == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            detailError != null && detail == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(detailError ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
            detail != null -> {
                val d = detail!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(d.sport ?: "Sport #${d.cart_type_id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    StatusBadge(status = d.status)
                                }
                                d.ground_name?.takeIf { it.isNotBlank() }?.let {
                                    Text("Ground: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                d.scheduled_at?.takeIf { it.isNotBlank() }?.let {
                                    Text("Scheduled: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                d.captain_name?.let {
                                    Text("Captain: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                d.skill_level?.let {
                                    Text("Skill: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    "Players: ${d.joined_players}/${d.max_players}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item {
                        Text("Players", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    if (d.players.isEmpty()) {
                        item {
                            Text("No players have joined yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(d.players, key = { it.id }) { player -> PlayerRow(player) }
                    }

                    item {
                        Text("Chat (read-only)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    if (messages.isEmpty()) {
                        item {
                            Text("No messages yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(messages, key = { it.id }) { msg -> ChatMessageRow(msg) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(player: MatchPlayerInfo) {
    AppCard {
        Column {
            Text(player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(player.phone, player.username?.let { "@$it" }).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatMessageRow(msg: AdminChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(msg.sender_name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text(msg.body, style = MaterialTheme.typography.bodyMedium)
    }
}

package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.vmsadmin.models.VoteRoundState
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.VoteRoundViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val MIN_OPTIONS = 2
private const val MAX_OPTIONS = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteRoundScreen(
    viewModel: VoteRoundViewModel,
    cartTypeViewModel: CartTypeViewModel,
    onBack: () -> Unit = {},
) {
    val round by viewModel.round.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val submitting by viewModel.submitting.collectAsState()
    val error by viewModel.error.collectAsState()
    val cartTypeState by cartTypeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { cartTypeViewModel.loadCartTypes() }
    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }

    val activeSports = cartTypeState.cartTypes.filter { it.is_active }.map { it.name }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sport Voting", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (loading && round == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                CurrentRoundCard(
                    round = round,
                    submitting = submitting,
                    onForceClose = { round?.round_id?.let { viewModel.closeRound(it) } },
                )
                StartRoundCard(
                    activeSports = activeSports,
                    submitting = submitting,
                    onStart = { options, closesAt -> viewModel.startRound(options, closesAt) { viewModel.load() } },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentRoundCard(round: VoteRoundState?, submitting: Boolean, onForceClose: () -> Unit) {
    AppCard {
        Text("Current round", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        if (round == null || round.status == "NONE") {
            Text("No active vote round.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@AppCard
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(round.status)
            Text(
                "${round.total_votes} vote${if (round.total_votes == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        round.closes_at?.let {
            Spacer(Modifier.height(4.dp))
            Text("Closes: ${formatIsoDateTime(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))

        val maxVotes = round.results.maxOfOrNull { it.votes } ?: 0
        round.results.forEach { r ->
            val isWinner = round.status == "CLOSED" && round.winner_sport == r.sport
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(r.sport, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal)
                    if (isWinner) Text("🏆", style = MaterialTheme.typography.bodyMedium)
                }
                Text("${r.votes}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(
                progress = { if (maxVotes > 0) r.votes.toFloat() / maxVotes else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
            )
        }

        if (round.status == "OPEN") {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onForceClose, enabled = !submitting, modifier = Modifier.fillMaxWidth()) {
                Text(if (submitting) "Closing…" else "Force close now")
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bg, fg, label) = when (status) {
        "OPEN" -> Triple(Color(0xFFDCF5E3), Color(0xFF1E7A3E), "OPEN")
        "CLOSED" -> Triple(Color(0xFFF0E3E3), Color(0xFF8A3B3B), "CLOSED")
        else -> Triple(Color(0xFFE8E8E8), Color(0xFF666666), status)
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartRoundCard(
    activeSports: List<String>,
    submitting: Boolean,
    onStart: (List<String>, String) -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var closesAtMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }

    val closesAtLabel = closesAtMillis?.let {
        SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.US).format(java.util.Date(it))
    } ?: "Not set"

    AppCard {
        Text("Start a new round", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Starting a new round replaces the current one (its history is kept).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        Text("Pick $MIN_OPTIONS-$MAX_OPTIONS sports", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        if (activeSports.isEmpty()) {
            Text("No active sports found — add some under Sport Types first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activeSports) { sport ->
                    val isSelected = sport in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selected = if (isSelected) selected - sport
                            else if (selected.size < MAX_OPTIONS) selected + sport else selected
                        },
                        label = { Text(sport) },
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Text("Voting deadline", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !submitting) { showDatePicker = true }
                .padding(14.dp),
        ) {
            Text(closesAtLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))

        val optionsValid = selected.size in MIN_OPTIONS..MAX_OPTIONS
        Button(
            onClick = {
                closesAtMillis?.let { millis ->
                    val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(java.util.Date(millis))
                    onStart(selected.toList(), iso)
                }
            },
            enabled = !submitting && optionsValid && closesAtMillis != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (submitting) "Starting…" else "Start round")
        }
    }

    if (showDatePicker) {
        val today = remember { Calendar.getInstance().timeInMillis }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = today,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= today - 86_400_000L
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 18, initialMinute = 0, is24Hour = false)
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pick a time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Button(onClick = {
                            val base = Calendar.getInstance().apply {
                                timeInMillis = pickedDateMillis ?: System.currentTimeMillis()
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                            }
                            closesAtMillis = base.timeInMillis
                            showTimePicker = false
                        }) { Text("Confirm") }
                    }
                }
            }
        }
    }
}

private fun formatIsoDateTime(iso: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)
    SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.US).format(parsed!!)
} catch (_: Exception) { iso }

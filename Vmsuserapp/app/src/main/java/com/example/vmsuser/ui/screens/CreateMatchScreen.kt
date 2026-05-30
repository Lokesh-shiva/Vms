package com.example.vmsuser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsuser.viewmodel.CreateMatchUiState
import com.example.vmsuser.viewmodel.MatchViewModel

private val SKILL_LEVELS = listOf("", "BEGINNER", "INTERMEDIATE", "ADVANCED")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMatchScreen(
    viewModel: MatchViewModel,
    cartTypes: List<Pair<Int, String>>,   // id to name pairs
    timeslots: List<Pair<Int, String>>,   // id to label pairs
    regions: List<Pair<Int, String>>,     // id to name pairs
    onSuccess: () -> Unit
) {
    val createState by viewModel.createState.collectAsState()

    // Form state
    var selectedCartTypeId by remember { mutableStateOf<Int?>(null) }
    var selectedTimeslotId by remember { mutableStateOf<Int?>(null) }
    var selectedRegionId by remember { mutableStateOf<Int?>(null) }
    var maxPlayersText by remember { mutableStateOf("") }
    var selectedSkillLevel by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    // Dropdown expanded state
    var cartTypeExpanded by remember { mutableStateOf(false) }
    var timeslotExpanded by remember { mutableStateOf(false) }
    var regionExpanded by remember { mutableStateOf(false) }
    var skillExpanded by remember { mutableStateOf(false) }

    // Navigate on success
    LaunchedEffect(createState) {
        if (createState is CreateMatchUiState.Success) {
            viewModel.clearCreateState()
            onSuccess()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Match",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSuccess) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                "Set up your match and we'll find the best available ground.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Sport picker
            ExposedDropdownMenuBox(expanded = cartTypeExpanded, onExpandedChange = { cartTypeExpanded = it }) {
                OutlinedTextField(
                    value = cartTypes.find { it.first == selectedCartTypeId }?.second ?: "Select Sport",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sport *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cartTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = cartTypeExpanded, onDismissRequest = { cartTypeExpanded = false }) {
                    cartTypes.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedCartTypeId = id
                                cartTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Timeslot picker
            ExposedDropdownMenuBox(expanded = timeslotExpanded, onExpandedChange = { timeslotExpanded = it }) {
                OutlinedTextField(
                    value = timeslots.find { it.first == selectedTimeslotId }?.second ?: "Select Time Slot",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time Slot *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(timeslotExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = timeslotExpanded, onDismissRequest = { timeslotExpanded = false }) {
                    timeslots.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedTimeslotId = id
                                timeslotExpanded = false
                            }
                        )
                    }
                }
            }

            // Region picker
            ExposedDropdownMenuBox(expanded = regionExpanded, onExpandedChange = { regionExpanded = it }) {
                OutlinedTextField(
                    value = regions.find { it.first == selectedRegionId }?.second ?: "Select Region",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Region *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(regionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                    regions.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedRegionId = id
                                regionExpanded = false
                            }
                        )
                    }
                }
            }

            // Max players
            OutlinedTextField(
                value = maxPlayersText,
                onValueChange = { maxPlayersText = it.filter { c -> c.isDigit() } },
                label = { Text("Max Players *") },
                placeholder = { Text("e.g. 10") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Skill level (optional)
            ExposedDropdownMenuBox(expanded = skillExpanded, onExpandedChange = { skillExpanded = it }) {
                OutlinedTextField(
                    value = selectedSkillLevel.ifEmpty { "Any Level (Optional)" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Skill Level (Optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(skillExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = skillExpanded, onDismissRequest = { skillExpanded = false }) {
                    SKILL_LEVELS.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.ifEmpty { "Any Level" }) },
                            onClick = {
                                selectedSkillLevel = level
                                skillExpanded = false
                            }
                        )
                    }
                }
            }

            // Form error
            formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // API error
            if (createState is CreateMatchUiState.Error) {
                Text(
                    (createState as CreateMatchUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    formError = null
                    val maxPlayers = maxPlayersText.toIntOrNull()
                    when {
                        selectedCartTypeId == null -> formError = "Please select a sport."
                        selectedTimeslotId == null -> formError = "Please select a time slot."
                        selectedRegionId == null -> formError = "Please select a region."
                        maxPlayers == null || maxPlayers <= 1 -> formError = "Max players must be a number greater than 1."
                        else -> viewModel.createMatch(
                            cartTypeId = selectedCartTypeId!!,
                            timeslotId = selectedTimeslotId!!,
                            regionId = selectedRegionId!!,
                            maxPlayers = maxPlayers,
                            skillLevel = selectedSkillLevel.ifEmpty { null }
                        )
                    }
                },
                enabled = createState !is CreateMatchUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (createState is CreateMatchUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create Match", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

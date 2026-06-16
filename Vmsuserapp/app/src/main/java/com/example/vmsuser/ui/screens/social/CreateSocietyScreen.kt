package com.example.vmsuser.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.SocialViewModel

private val ALL_SPORTS = listOf("Cricket", "Football", "Badminton", "Basketball", "Tennis", "Swimming", "Volleyball", "Running")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSocietyScreen(navController: NavController) {
    val vm: SocialViewModel = viewModel()
    var name by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sportExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(PlixoSurface).statusBarsPadding()) {
        PlixoTopBar(title = "Create Society", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Society name") },
                placeholder = { Text("e.g. Indiranagar Badminton Club") },
                shape = PlixoShape.Input,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlixoPrimary, unfocusedBorderColor = PlixoBorder, focusedContainerColor = PlixoSurface2, unfocusedContainerColor = PlixoSurface2),
                singleLine = true,
            )
            Spacer(Modifier.height(14.dp))
            ExposedDropdownMenuBox(expanded = sportExpanded, onExpandedChange = { sportExpanded = it }) {
                OutlinedTextField(
                    value = sport, onValueChange = {}, readOnly = true,
                    label = { Text("Sport") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = PlixoShape.Input,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlixoPrimary, unfocusedBorderColor = PlixoBorder, focusedContainerColor = PlixoSurface2, unfocusedContainerColor = PlixoSurface2),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded) },
                )
                ExposedDropdownMenu(expanded = sportExpanded, onDismissRequest = { sportExpanded = false }) {
                    ALL_SPORTS.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { sport = s; sportExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = { Text("About this society") },
                placeholder = { Text("What sports do you play? When? Where?") },
                shape = PlixoShape.Input,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlixoPrimary, unfocusedBorderColor = PlixoBorder, focusedContainerColor = PlixoSurface2, unfocusedContainerColor = PlixoSurface2),
                maxLines = 4,
            )
            Spacer(Modifier.height(26.dp))
            PlixoButton(
                label = if (loading) "Creating…" else "Create society",
                onClick = {
                    loading = true
                    vm.create(name, sport, description) { navController.popBackStack() }
                },
                enabled = name.isNotBlank() && sport.isNotBlank() && description.isNotBlank() && !loading,
            )
        }
    }
}

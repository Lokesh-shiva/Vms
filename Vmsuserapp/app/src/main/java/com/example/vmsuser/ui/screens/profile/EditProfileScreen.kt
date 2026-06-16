package com.example.vmsuser.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ProfileViewModel

private val REGIONS = listOf(
    "Indiranagar, Bengaluru",
    "Koramangala, Bengaluru",
    "HSR Layout, Bengaluru",
    "Whitefield, Bengaluru",
    "Jayanagar, Bengaluru",
    "Bandra, Mumbai",
    "Andheri, Mumbai",
    "Connaught Place, Delhi",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val vm: ProfileViewModel = viewModel()
    val user by vm.user.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(user?.name ?: "") }
    var region by remember { mutableStateOf(user?.region ?: "") }
    var regionExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(PlixoSurface).statusBarsPadding()) {
        PlixoTopBar(title = "Edit Profile", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full name") },
                shape = PlixoShape.Input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PlixoPrimary,
                    unfocusedBorderColor = PlixoBorder,
                    focusedContainerColor = PlixoSurface2,
                    unfocusedContainerColor = PlixoSurface2,
                ),
                singleLine = true,
            )
            Spacer(Modifier.height(14.dp))
            ExposedDropdownMenuBox(
                expanded = regionExpanded,
                onExpandedChange = { regionExpanded = it },
            ) {
                OutlinedTextField(
                    value = region,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Your area") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = PlixoShape.Input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = regionExpanded,
                    onDismissRequest = { regionExpanded = false },
                ) {
                    REGIONS.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r) },
                            onClick = { region = r; regionExpanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            PlixoButton(
                label = if (loading) "Saving…" else "Save changes",
                onClick = {
                    loading = true
                    vm.updateProfile(name, region) { navController.popBackStack() }
                },
                enabled = name.isNotBlank() && !loading,
            )
        }
    }
}

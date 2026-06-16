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
import com.example.vmsuser.models.LocationOption
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val vm: ProfileViewModel = viewModel()
    val user by vm.user.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(user?.name ?: "") }
    var city by remember { mutableStateOf(user?.city ?: user?.region ?: "") }
    var areaExpanded by remember { mutableStateOf(false) }
    var locations by remember { mutableStateOf<List<LocationOption>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val res = RetrofitClient.api.getLocations()
            if (res.success && res.data != null) locations = res.data
        } catch (_: Exception) {}
    }

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
                expanded = areaExpanded,
                onExpandedChange = { areaExpanded = it },
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Your area") },
                    placeholder = { Text(if (locations.isEmpty()) "Loading areas…" else "Select your area") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = PlixoShape.Input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpanded) },
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = areaExpanded && locations.isNotEmpty(),
                    onDismissRequest = { areaExpanded = false },
                ) {
                    locations.forEach { loc ->
                        DropdownMenuItem(
                            text = { Text(loc.name) },
                            onClick = { city = loc.name; areaExpanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            PlixoButton(
                label = if (loading) "Saving…" else "Save changes",
                onClick = {
                    loading = true
                    scope.launch {
                        vm.updateProfile(name, city) {
                            loading = false
                            navController.popBackStack()
                        }
                    }
                },
                enabled = name.isNotBlank() && !loading,
            )
        }
    }
}

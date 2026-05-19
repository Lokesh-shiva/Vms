package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.UserManagementState
import com.example.vmsadmin.viewmodel.UserManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: UserManagementViewModel,
    currentUserId: Int?,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val pendingIds by viewModel.pendingIds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        if (state is UserManagementState.Error) {
            snackbarHostState.showSnackbar((state as UserManagementState.Error).message)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Users",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state is UserManagementState.Loading -> {
                    // Shimmer skeleton
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            AppCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.width(160.dp).height(20.dp).shimmerEffect())
                                        Spacer(Modifier.height(6.dp))
                                        Box(modifier = Modifier.width(120.dp).height(14.dp).shimmerEffect())
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.width(80.dp).height(28.dp).shimmerEffect())
                                            Box(modifier = Modifier.width(60.dp).height(24.dp).shimmerEffect())
                                        }
                                    }
                                    Box(modifier = Modifier.size(24.dp).shimmerEffect())
                                }
                            }
                        }
                    }
                }

                state is UserManagementState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (state as UserManagementState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadUsers() }) { Text("Retry") }
                    }
                }

                state is UserManagementState.Success &&
                        (state as UserManagementState.Success).users.isEmpty() -> {
                    Text(
                        "No users found.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state is UserManagementState.Success -> {
                    val users = (state as UserManagementState.Success).users
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { viewModel.loadUsers() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(users, key = { it.id }) { user ->
                                UserRow(
                                    user = user,
                                    isCurrentUser = user.id == currentUserId,
                                    isPending = pendingIds.contains(user.id),
                                    onChangeRole = { newRole -> viewModel.changeRole(user.id, newRole) },
                                    onToggleActive = { viewModel.toggleActive(user.id, user.is_active) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: AppUser,
    isCurrentUser: Boolean,
    isPending: Boolean,
    onChangeRole: (String) -> Unit,
    onToggleActive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrentUser) {
                        Text(
                            text = " (you)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = user.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(user.role) }
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (user.is_active)
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = if (user.is_active) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.is_active)
                                Color(0xFF388E3C)
                            else
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            when {
                isPending -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                !isCurrentUser -> {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change role") },
                                onClick = {
                                    showMenu = false
                                    showRoleDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (user.is_active) "Deactivate" else "Reactivate") },
                                onClick = {
                                    showMenu = false
                                    onToggleActive()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRoleDialog) {
        val roles = listOf(
            "super_admin",
            "ops_manager",
            "ground_owner",
            "tournament_manager",
            "support",
            "finance",
            "csr_partner",
            "user"
        )
        var selectedRole by remember(user.role) { mutableStateOf(user.role) }

        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Change Role") },
            text = {
                Column {
                    roles.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = role }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = role, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRoleDialog = false
                    onChangeRole(selectedRole)
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

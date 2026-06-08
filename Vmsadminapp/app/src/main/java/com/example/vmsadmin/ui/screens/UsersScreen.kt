package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = (state as? UserManagementState.Error)?.message
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
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
            val currentState = state
            when {
                currentState is UserManagementState.Loading -> {
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

                currentState is UserManagementState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadUsers() }) { Text("Retry") }
                    }
                }

                currentState is UserManagementState.Success &&
                        currentState.users.isEmpty() -> {
                    Text(
                        "No users found.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                currentState is UserManagementState.Success -> {
                    val users = currentState.users
                    val assignableRoles by viewModel.assignableRoles.collectAsState()

                    // Roles present in the data, ordered by ROLE_ORDER
                    val rolesPresent = ROLE_ORDER.filter { role -> users.any { it.role == role } } +
                            users.map { it.role }.filter { it !in ROLE_ORDER }.distinct()

                    var selectedRole by rememberSaveable { mutableStateOf<String?>(null) }
                    // Reset filter if the selected role disappears from the data
                    LaunchedEffect(rolesPresent) {
                        if (selectedRole != null && selectedRole !in rolesPresent) selectedRole = null
                    }

                    val visibleUsers = remember(users, selectedRole) {
                        if (selectedRole == null) users else users.filter { it.role == selectedRole }
                    }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshUsers() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── Filter chips ───────────────────────────────
                            item {
                                RoleFilterRow(
                                    rolesPresent = rolesPresent,
                                    selectedRole = selectedRole,
                                    totalCount = users.size,
                                    countFor = { role -> users.count { it.role == role } },
                                    onSelect = { selectedRole = it }
                                )
                            }

                            if (selectedRole == null) {
                                // Grouped by role with section headers
                                rolesPresent.forEach { role ->
                                    val group = visibleUsers.filter { it.role == role }
                                    if (group.isNotEmpty()) {
                                        item(key = "header_$role") {
                                            RoleSectionHeader(role = role, count = group.size)
                                        }
                                        items(group, key = { it.id }) { user ->
                                            UserRow(
                                                user = user,
                                                isCurrentUser = user.id == currentUserId,
                                                isPending = pendingIds.contains(user.id),
                                                assignableRoles = assignableRoles,
                                                onChangeRole = { newRole -> viewModel.changeRole(user.id, newRole) },
                                                onToggleActive = { viewModel.toggleActive(user.id, user.is_active) },
                                                onToggleSocietyPermission = { viewModel.toggleSocietyPermission(user.id, user.can_create_society) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(visibleUsers, key = { it.id }) { user ->
                                    UserRow(
                                        user = user,
                                        isCurrentUser = user.id == currentUserId,
                                        isPending = pendingIds.contains(user.id),
                                        assignableRoles = assignableRoles,
                                        onChangeRole = { newRole -> viewModel.changeRole(user.id, newRole) },
                                        onToggleActive = { viewModel.toggleActive(user.id, user.is_active) },
                                        onToggleSocietyPermission = { viewModel.toggleSocietyPermission(user.id, user.can_create_society) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Canonical display order for role grouping/filtering
private val ROLE_ORDER = listOf(
    "super_admin",
    "ops_manager",
    "ground_owner",
    "tournament_manager",
    "support",
    "finance",
    "csr_partner",
    "user"
)

private fun roleLabel(role: String): String =
    role.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

@Composable
private fun RoleFilterRow(
    rolesPresent: List<String>,
    selectedRole: String?,
    totalCount: Int,
    countFor: (String) -> Int,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedRole == null,
            onClick = { onSelect(null) },
            label = { Text("All ($totalCount)") }
        )
        rolesPresent.forEach { role ->
            FilterChip(
                selected = selectedRole == role,
                onClick = { onSelect(role) },
                label = { Text("${roleLabel(role)} (${countFor(role)})") }
            )
        }
    }
}

@Composable
private fun RoleSectionHeader(role: String, count: Int) {
    Text(
        text = "${roleLabel(role).uppercase()}  ·  $count",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun UserRow(
    user: AppUser,
    isCurrentUser: Boolean,
    isPending: Boolean,
    assignableRoles: List<String>,
    onChangeRole: (String) -> Unit,
    onToggleActive: () -> Unit,
    onToggleSocietyPermission: () -> Unit
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
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = user.role,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
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
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (user.can_create_society) "Revoke Society Creation" else "Grant Society Creation"
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleSocietyPermission()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRoleDialog) {
        val roles = assignableRoles
        var selectedRole by remember(user.role) { mutableStateOf(user.role) }

        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Change Role") },
            text = {
                if (roles.isEmpty()) {
                    Text(
                        "No roles available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
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

package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.CartViewModel
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.FeeConfigViewModel
import com.example.vmsadmin.viewmodel.ItemViewModel
import com.example.vmsadmin.viewmodel.GroundViewModel
import com.example.vmsadmin.viewmodel.MatchViewModel
import com.example.vmsadmin.viewmodel.PaymentViewModel
import com.example.vmsadmin.viewmodel.RegionViewModel
import com.example.vmsadmin.viewmodel.TimeslotViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Outlined.Home)
    object Bookings : BottomNavItem("bookings", "Bookings", Icons.Outlined.DateRange)
    object Payments : BottomNavItem("payments", "Payments", Icons.Outlined.ShoppingCart)
    object Manage : BottomNavItem("manage", "Manage", Icons.Outlined.Settings)
}

private val MANAGE_ROLES = setOf("super_admin", "ops_manager")
private val PAYMENT_ROLES = setOf("super_admin", "finance")

@Composable
fun MainScreen(
    viewModel: DashboardViewModel,
    paymentViewModel: PaymentViewModel,
    bookingViewModel: BookingViewModel,
    regionViewModel: RegionViewModel,
    cartTypeViewModel: CartTypeViewModel,
    timeslotViewModel: TimeslotViewModel,
    cartViewModel: CartViewModel,
    feeConfigViewModel: FeeConfigViewModel,
    itemViewModel: ItemViewModel,
    matchViewModel: MatchViewModel,
    groundViewModel: GroundViewModel,
    role: String = "",
    isDebugMode: Boolean = false,
    onSetDebugRole: (String?) -> Unit = {},
    onForbidden: () -> Unit = {}
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Bookings,
        BottomNavItem.Payments,
        BottomNavItem.Manage,
    ).filter { item ->
        when (item) {
            BottomNavItem.Payments -> role in setOf("super_admin", "finance")
            BottomNavItem.Manage -> role in setOf("super_admin", "ops_manager")
            else -> true
        }
    }

    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF2E2458), Color(0xFF0F1115))
    } else {
        listOf(Color(0xFFFFFDF5), Color(0xFFF2E3C6))
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.radialGradient(
            colors = gradientColors,
            radius = 1800f
        )
    )) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp)),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(24.dp)) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route ||
                                (item.route == "manage" && currentRoute?.startsWith("manage/") == true),
                        onClick = {
                            navController.navigate(item.route) {
                                navController.graph.startDestinationRoute?.let { route ->
                                    popUpTo(route) {
                                        saveState = true
                                    }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(viewModel)
            }
            composable(BottomNavItem.Bookings.route) {
                BookingsScreen(bookingViewModel)
            }
            composable(BottomNavItem.Payments.route) {
                if (role !in PAYMENT_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    PaymentsScreen(paymentViewModel)
                }
            }
            composable(BottomNavItem.Manage.route) {
                if (role !in MANAGE_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    ManageScreen(
                        onNavigateToRegions = {
                            navController.navigate("manage/regions")
                        },
                        onNavigateToSports = {
                            navController.navigate("manage/sports")
                        },
                        onNavigateToTimeslots = {
                            navController.navigate("manage/timeslots")
                        },
                        onNavigateToFeeConfig = {
                            navController.navigate("manage/fee-config")
                        },
                        onNavigateToItems = {
                            navController.navigate("manage/items")
                        },
                        onNavigateToMatches = {
                            navController.navigate("manage/matches")
                        },
                        onNavigateToGrounds = {
                            navController.navigate("manage/grounds")
                        }
                    )
                }
            }
            composable("manage/regions") {
                if (role !in MANAGE_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    RegionsScreen(viewModel = regionViewModel, onBack = { navController.popBackStack() })
                }
            }
            composable("manage/sports") {
                if (role !in MANAGE_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    CartTypesScreen(viewModel = cartTypeViewModel, onBack = { navController.popBackStack() })
                }
            }
            composable("manage/timeslots") {
                if (role !in MANAGE_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    TimeslotsScreen(viewModel = timeslotViewModel, onBack = { navController.popBackStack() })
                }
            }
            composable("manage/fee-config") {
                if (role !in MANAGE_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    FeeConfigScreen(
                        viewModel = feeConfigViewModel,
                        regionViewModel = regionViewModel,
                        cartTypeViewModel = cartTypeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("manage/items") {
                if (role !in MANAGE_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    ItemsScreen(
                        viewModel = itemViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("manage/matches") {
                if (role !in MANAGE_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    MatchesScreen(
                        viewModel = matchViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("manage/grounds") {
                if (role !in MANAGE_ROLES) {
                    LaunchedEffect(Unit) { onForbidden() }
                } else {
                    GroundsScreen(
                        viewModel = groundViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
        if (isDebugMode) {
            DebugRoleSwitcher(
                currentRole = role,
                onRoleSelected = onSetDebugRole,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 88.dp)
            )
        }
    }
}

@Composable
private fun DebugRoleSwitcher(
    currentRole: String,
    onRoleSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val isOverriding = currentRole != "super_admin"

    SmallFloatingActionButton(
        onClick = { showDialog = true },
        modifier = modifier,
        containerColor = if (isOverriding) Color(0xFFE65100) else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isOverriding) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (isOverriding) currentRole.take(6) else "🔧",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }

    if (showDialog) {
        val roles = listOf(
            "super_admin" to "Super Admin (reset)",
            "ops_manager" to "Ops Manager",
            "ground_owner" to "Ground Owner",
            "tournament_manager" to "Tournament Manager",
            "support" to "Support",
            "finance" to "Finance",
            "csr_partner" to "CSR Partner",
            "user" to "User (will trigger Forbidden)"
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Debug: Switch Role") },
            text = {
                Column {
                    roles.forEach { (roleValue, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRoleSelected(if (roleValue == "super_admin") null else roleValue)
                                    showDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentRole == roleValue,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Close") }
            }
        )
    }
}

package com.example.vmsadmin.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vmsadmin.viewmodel.AuditLogViewModel
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.SocietyViewModel
import com.example.vmsadmin.viewmodel.CaptainViewModel
import com.example.vmsadmin.viewmodel.CartViewModel
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.DisputeViewModel
import com.example.vmsadmin.viewmodel.FeeConfigViewModel
import com.example.vmsadmin.viewmodel.ItemViewModel
import com.example.vmsadmin.viewmodel.GroundViewModel
import com.example.vmsadmin.viewmodel.MatchViewModel
import com.example.vmsadmin.viewmodel.UserManagementViewModel
import com.example.vmsadmin.viewmodel.PaymentViewModel
import com.example.vmsadmin.viewmodel.QueueOverviewViewModel
import com.example.vmsadmin.viewmodel.RegionViewModel
import com.example.vmsadmin.viewmodel.TournamentViewModel
import com.example.vmsadmin.viewmodel.SystemConfigViewModel
import com.example.vmsadmin.viewmodel.TimeslotViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Outlined.Home)
    object Bookings  : BottomNavItem("bookings",  "Bookings",  Icons.Outlined.DateRange)
    object Payments  : BottomNavItem("payments",  "Payments",  Icons.Outlined.ShoppingCart)
    object Manage    : BottomNavItem("manage",    "Manage",    Icons.Outlined.Settings)
    object Support   : BottomNavItem("support",   "Support",   Icons.Outlined.SupportAgent)
    object MyGrounds : BottomNavItem("my-grounds", "My Grounds", Icons.Outlined.LocationOn)
    object Matches   : BottomNavItem("matches",   "Matches",   Icons.Outlined.Star)
    object Csr       : BottomNavItem("csr",       "CSR",       Icons.Outlined.Info)
}

// ── Role permission sets ─────────────────────────────────────────────
private val MANAGE_ROLES        = setOf("super_admin", "ops_manager")
private val PAYMENT_ROLES       = setOf("super_admin", "finance")
private val USERS_ROLES         = setOf("super_admin")
private val SYSTEM_CONFIG_ROLES = setOf("super_admin")
private val QUEUE_ROLES         = setOf("super_admin", "ops_manager")
private val CAPTAIN_ROLES       = setOf("super_admin", "ops_manager")
private val SUPPORT_ROLES       = setOf("support", "super_admin", "ops_manager")
private val GROUND_OWNER_ROLES  = setOf("ground_owner")
private val TOURNAMENT_ROLES    = setOf("tournament_manager", "super_admin", "ops_manager")
private val CSR_ROLES           = setOf("csr_partner", "super_admin")
private val DISPUTE_ROLES       = setOf("support", "super_admin", "ops_manager")
private val AUDIT_LOG_ROLES     = setOf("super_admin")
private val SOCIETY_ROLES       = setOf("super_admin", "ops_manager")

@OptIn(ExperimentalMaterial3Api::class)
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
    userManagementViewModel: UserManagementViewModel,
    systemConfigViewModel: SystemConfigViewModel,
    queueOverviewViewModel: QueueOverviewViewModel,
    captainViewModel: CaptainViewModel,
    tournamentViewModel: TournamentViewModel,
    disputeViewModel: DisputeViewModel,
    auditLogViewModel: AuditLogViewModel,
    societyViewModel: SocietyViewModel,
    currentUserId: Int? = null,
    role: String = "",
    isDebugMode: Boolean = false,
    onSetDebugRole: (String?) -> Unit = {},
    onLogout: () -> Unit = {},
    onForbidden: () -> Unit = {}
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Bookings,
        BottomNavItem.Payments,
        BottomNavItem.Support,
        BottomNavItem.MyGrounds,
        BottomNavItem.Matches,
        BottomNavItem.Csr,
        BottomNavItem.Manage,
    ).filter { item ->
        when (item) {
            BottomNavItem.Payments -> role in PAYMENT_ROLES
            BottomNavItem.Manage   -> role in MANAGE_ROLES
            BottomNavItem.Support  -> role in SUPPORT_ROLES
            BottomNavItem.MyGrounds -> role == "ground_owner"
            // Matches as a dedicated tab only for tournament_manager
            // (ops_manager / super_admin reach Matches via the Manage screen)
            BottomNavItem.Matches  -> role == "tournament_manager"
            BottomNavItem.Csr      -> role == "csr_partner"
            else -> true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Plixo",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (role.isNotEmpty()) {
                                Spacer(Modifier.width(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = role.replace("_", " ").uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ExitToApp,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(22.dp)) },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute == item.route ||
                                    (item.route == "manage" && currentRoute?.startsWith("manage/") == true),
                            onClick = {
                                navController.navigate(item.route) {
                                    navController.graph.startDestinationRoute?.let { route ->
                                        popUpTo(route) { saveState = true }
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
                // ── Main tabs ──────────────────────────────────────────
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
                composable(BottomNavItem.Support.route) {
                    if (role !in SUPPORT_ROLES) {
                        LaunchedEffect(Unit) { onForbidden() }
                    } else {
                        SupportScreen(
                            userManagementViewModel = userManagementViewModel,
                            bookingViewModel = bookingViewModel,
                            disputeViewModel = disputeViewModel
                        )
                    }
                }
                composable(BottomNavItem.MyGrounds.route) {
                    if (role !in GROUND_OWNER_ROLES) {
                        LaunchedEffect(Unit) { onForbidden() }
                    } else {
                        GroundOwnerScreen(
                            groundViewModel = groundViewModel,
                            bookingViewModel = bookingViewModel
                        )
                    }
                }
                composable(BottomNavItem.Matches.route) {
                    if (role !in (MANAGE_ROLES + TOURNAMENT_ROLES)) {
                        LaunchedEffect(Unit) { onForbidden() }
                    } else {
                        MatchesScreen(viewModel = matchViewModel, onBack = null)
                    }
                }
                composable(BottomNavItem.Csr.route) {
                    if (role !in CSR_ROLES) {
                        LaunchedEffect(Unit) { onForbidden() }
                    } else {
                        CsrScreen(matchViewModel = matchViewModel)
                    }
                }
                composable(BottomNavItem.Manage.route) {
                    if (role !in MANAGE_ROLES) {
                        LaunchedEffect(Unit) { onForbidden() }
                    } else {
                        ManageScreen(
                            onNavigateToRegions      = { navController.navigate("manage/regions") },
                            onNavigateToSports       = { navController.navigate("manage/sports") },
                            onNavigateToTimeslots    = { navController.navigate("manage/timeslots") },
                            onNavigateToFeeConfig    = { navController.navigate("manage/fee-config") },
                            onNavigateToItems        = { navController.navigate("manage/items") },
                            onNavigateToMatches      = { navController.navigate("manage/matches") },
                            onNavigateToGrounds      = { navController.navigate("manage/grounds") },
                            onNavigateToUsers        = { navController.navigate("manage/users") },
                            onNavigateToSystemConfig = { navController.navigate("manage/system-config") },
                            onNavigateToQueue        = { navController.navigate("manage/queue") },
                            onNavigateToCaptains     = { navController.navigate("manage/captains") },
                            onNavigateToTournaments  = { navController.navigate("manage/tournaments") },
                            onNavigateToDisputes     = { navController.navigate("manage/disputes") },
                            onNavigateToAuditLog     = { navController.navigate("manage/audit-logs") },
                            onNavigateToSocieties    = { navController.navigate("manage/societies") },
                            role = role
                        )
                    }
                }

                // ── Manage sub-routes ──────────────────────────────────
                composable("manage/regions") {
                    if (role !in MANAGE_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else RegionsScreen(viewModel = regionViewModel, onBack = { navController.popBackStack() })
                }
                composable("manage/sports") {
                    if (role !in MANAGE_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else CartTypesScreen(viewModel = cartTypeViewModel, onBack = { navController.popBackStack() })
                }
                composable("manage/timeslots") {
                    if (role !in MANAGE_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else TimeslotsScreen(viewModel = timeslotViewModel, onBack = { navController.popBackStack() })
                }
                composable("manage/fee-config") {
                    if (role !in MANAGE_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else FeeConfigScreen(
                        viewModel = feeConfigViewModel,
                        regionViewModel = regionViewModel,
                        cartTypeViewModel = cartTypeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage/items") {
                    if (role !in MANAGE_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else ItemsScreen(viewModel = itemViewModel, onBack = { navController.popBackStack() })
                }
                composable("manage/matches") {
                    if (role !in (MANAGE_ROLES + TOURNAMENT_ROLES)) { LaunchedEffect(Unit) { onForbidden() } }
                    else MatchesScreen(viewModel = matchViewModel, onBack = { navController.popBackStack() })
                }
                composable("manage/grounds") {
                    if (role !in MANAGE_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else GroundsScreen(viewModel = groundViewModel, currentUserRole = role, onBack = { navController.popBackStack() })
                }
                composable("manage/users") {
                    if (role !in USERS_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else UsersScreen(
                        viewModel = userManagementViewModel,
                        currentUserId = currentUserId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage/system-config") {
                    if (role !in SYSTEM_CONFIG_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else SystemConfigScreen(
                        viewModel = systemConfigViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage/queue") {
                    if (role !in QUEUE_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else QueueOverviewScreen(
                        viewModel = queueOverviewViewModel,
                        cartTypeViewModel = cartTypeViewModel,
                        regionViewModel = regionViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage/captains") {
                    if (role !in CAPTAIN_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else CaptainScreen(
                        viewModel = captainViewModel,
                        userManagementViewModel = userManagementViewModel,
                        regionViewModel = regionViewModel,
                        currentUserRole = role,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage/tournaments") {
                    if (role !in TOURNAMENT_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else TournamentsScreen(
                        viewModel = tournamentViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage/disputes") {
                    if (role !in DISPUTE_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else DisputesScreen(
                        viewModel = disputeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage/audit-logs") {
                    if (role !in AUDIT_LOG_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else AuditLogScreen(
                        viewModel = auditLogViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage/societies") {
                    if (role !in SOCIETY_ROLES) { LaunchedEffect(Unit) { onForbidden() } }
                    else SocietiesScreen(
                        viewModel = societyViewModel,
                        currentUserRole = role,
                        onBack = { navController.popBackStack() }
                    )
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
            "super_admin"        to "Super Admin (reset)",
            "ops_manager"        to "Ops Manager",
            "ground_owner"       to "Ground Owner",
            "tournament_manager" to "Tournament Manager",
            "support"            to "Support",
            "finance"            to "Finance",
            "csr_partner"        to "CSR Partner",
            "user"               to "User (will trigger Forbidden)"
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
                            RadioButton(selected = currentRole == roleValue, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Close") } }
        )
    }
}

package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.PaymentViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Default.Home)
    object Bookings : BottomNavItem("bookings", "Bookings", Icons.Default.DateRange)
    object Payments : BottomNavItem("payments", "Payments", Icons.Default.ShoppingCart)
    object Manage : BottomNavItem("manage", "Manage", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    viewModel: DashboardViewModel,
    paymentViewModel: PaymentViewModel,
    bookingViewModel: BookingViewModel
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Bookings,
        BottomNavItem.Payments,
        BottomNavItem.Manage
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
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
                PaymentsScreen(paymentViewModel)
            }
            composable(BottomNavItem.Manage.route) {
                ManageScreen()
            }
        }
    }
}

package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.PaymentViewModel
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.RegionViewModel
import com.example.vmsadmin.viewmodel.TimeslotViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Outlined.Home)
    object Bookings : BottomNavItem("bookings", "Bookings", Icons.Outlined.DateRange)
    object Payments : BottomNavItem("payments", "Payments", Icons.Outlined.ShoppingCart)
    object Manage : BottomNavItem("manage", "Manage", Icons.Outlined.Settings)
}

@Composable
fun MainScreen(
    viewModel: DashboardViewModel,
    paymentViewModel: PaymentViewModel,
    bookingViewModel: BookingViewModel,
    regionViewModel: RegionViewModel,
    cartTypeViewModel: CartTypeViewModel,
    timeslotViewModel: TimeslotViewModel,
    cartViewModel: CartViewModel
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Bookings,
        BottomNavItem.Payments,
        BottomNavItem.Manage
    )

    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF2E2458), // Deep purple glow
            Color(0xFF0F1115)  // Dark outer background
        )
    } else {
        listOf(
            Color(0xFFFFFDF5), // Soft white
            Color(0xFFF2E3C6)  // Soft gold outer background
        )
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
                ManageScreen(
                    onNavigateToRegions = {
                        navController.navigate("manage/regions")
                    },
                    onNavigateToCartTypes = {
                        navController.navigate("manage/cart-types")
                    },
                    onNavigateToTimeslots = {
                        navController.navigate("manage/timeslots")
                    },
                    onNavigateToCarts = {
                        navController.navigate("manage/carts")
                    }
                )
            }
            composable("manage/regions") {
                RegionsScreen(viewModel = regionViewModel)
            }
            composable("manage/cart-types") {
                CartTypesScreen(viewModel = cartTypeViewModel)
            }
            composable("manage/timeslots") {
                TimeslotsScreen(viewModel = timeslotViewModel)
            }
            composable("manage/carts") {
                CartsScreen(
                    viewModel = cartViewModel,
                    regionViewModel = regionViewModel,
                    cartTypeViewModel = cartTypeViewModel
                )
            }
        }
    }
    }
}

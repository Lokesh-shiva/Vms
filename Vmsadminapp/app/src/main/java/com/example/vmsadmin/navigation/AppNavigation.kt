package com.example.vmsadmin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vmsadmin.network.ApiClient
import com.example.vmsadmin.ui.screens.ForbiddenScreen
import com.example.vmsadmin.ui.screens.LoginScreen
import com.example.vmsadmin.ui.screens.MainScreen
import com.example.vmsadmin.viewmodel.AuthViewModel
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.CartViewModel
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.FeeConfigViewModel
import com.example.vmsadmin.viewmodel.ItemViewModel
import com.example.vmsadmin.viewmodel.GroundViewModel
import com.example.vmsadmin.viewmodel.MatchViewModel
import com.example.vmsadmin.viewmodel.UserManagementViewModel
import com.example.vmsadmin.viewmodel.PaymentViewModel
import com.example.vmsadmin.viewmodel.RegionViewModel
import com.example.vmsadmin.viewmodel.TimeslotViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
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
    startDestination: String
) {
    val navController = rememberNavController()
    val realRole by authViewModel.currentRole.collectAsState()
    val role by authViewModel.effectiveRole.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState()

    // Auto-logout on 401
    LaunchedEffect(Unit) {
        ApiClient.logoutEvent.collect {
            navController.navigate("login") {
                popUpTo(0)
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                viewModel = dashboardViewModel,
                paymentViewModel = paymentViewModel,
                bookingViewModel = bookingViewModel,
                regionViewModel = regionViewModel,
                cartTypeViewModel = cartTypeViewModel,
                timeslotViewModel = timeslotViewModel,
                cartViewModel = cartViewModel,
                feeConfigViewModel = feeConfigViewModel,
                itemViewModel = itemViewModel,
                matchViewModel = matchViewModel,
                groundViewModel = groundViewModel,
                userManagementViewModel = userManagementViewModel,
                currentUserId = currentUserId,
                role = role ?: "",
                isDebugMode = realRole == "super_admin",
                onSetDebugRole = { authViewModel.setDebugRole(it) },
                onForbidden = {
                    navController.navigate("forbidden") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("forbidden") {
            ForbiddenScreen(onLogout = {
                authViewModel.logout()
                navController.navigate("login") {
                    popUpTo(0)
                }
            })
        }
    }
}

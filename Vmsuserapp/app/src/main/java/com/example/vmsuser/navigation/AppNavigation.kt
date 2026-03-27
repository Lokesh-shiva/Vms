package com.example.vmsuser.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vmsuser.data.AddressManager
import com.example.vmsuser.network.ApiClient
import com.example.vmsuser.ui.screens.BookingScreen
import com.example.vmsuser.ui.screens.BookingStatusScreen
import com.example.vmsuser.ui.screens.CartScreen
import com.example.vmsuser.ui.screens.CreateMatchScreen
import com.example.vmsuser.ui.screens.HomeScreen
import com.example.vmsuser.ui.screens.LoginScreen
import com.example.vmsuser.ui.screens.MatchListScreen
import com.example.vmsuser.ui.screens.PaymentScreen
import com.example.vmsuser.ui.screens.RegisterScreen
import com.example.vmsuser.viewmodel.AuthViewModel
import com.example.vmsuser.viewmodel.BookingViewModel
import com.example.vmsuser.viewmodel.HomeViewModel
import com.example.vmsuser.viewmodel.MatchViewModel
import com.example.vmsuser.viewmodel.PaymentViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    bookingViewModel: BookingViewModel,
    paymentViewModel: PaymentViewModel,
    matchViewModel: MatchViewModel,
    addressManager: AddressManager,
    startDestination: String
) {
    val navController = rememberNavController()

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
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegistrationSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToBooking = {
                    navController.navigate("booking")
                },
                onNavigateToCart = {
                    navController.navigate("cart")
                },
                onNavigateToMatches = {
                    navController.navigate("matches")
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }

        // ── Cart Screen ──────────────────────────────────────────────
        composable("cart") {
            CartScreen(
                homeViewModel = homeViewModel,
                bookingViewModel = bookingViewModel,
                addressManager = addressManager,
                onNavigateToBookingStatus = { bookingId ->
                    navController.navigate("booking_status/$bookingId") {
                        popUpTo("cart") { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("booking") {
            BookingScreen(
                homeViewModel = homeViewModel,
                bookingViewModel = bookingViewModel,
                onNavigateToPayment = { bookingId ->
                    navController.navigate("payment/$bookingId")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "payment/{bookingId}",
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getInt("bookingId") ?: return@composable
            PaymentScreen(
                bookingId = bookingId,
                paymentViewModel = paymentViewModel,
                onNavigateToStatus = {
                    navController.navigate("booking_status/$bookingId") {
                        popUpTo("payment/$bookingId") { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "booking_status/{bookingId}",
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getInt("bookingId") ?: return@composable
            BookingStatusScreen(
                bookingId = bookingId,
                bookingViewModel = bookingViewModel,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo(0)
                    }
                }
            )
        }

        // ── Match routes ──────────────────────────────────────────────
        composable("matches") {
            MatchListScreen(
                viewModel = matchViewModel,
                currentUserId = null,   // Server enforces auth; join/leave guarded by token
                onCreateMatch = { navController.navigate("matches/create") }
            )
        }

        composable("matches/create") {
            val homeUiState by homeViewModel.uiState.collectAsState()
            val cartTypes = homeUiState.cartTypes.map { it.id to it.name }
            val timeslots = homeUiState.timeslots.map { it.id to "${it.start_time} - ${it.end_time}" }
            val regions = homeUiState.regions.map { it.id to it.name }
            CreateMatchScreen(
                viewModel = matchViewModel,
                cartTypes = cartTypes,
                timeslots = timeslots,
                regions = regions,
                onSuccess = { navController.popBackStack() }
            )
        }
    }
}

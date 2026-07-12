package com.example.vmsuser.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.notifications.PendingDeepLink
import com.example.vmsuser.ui.components.PlixoBottomNav
import com.example.vmsuser.ui.screens.auth.*
import com.example.vmsuser.ui.screens.captain.*
import com.example.vmsuser.ui.screens.chat.*
import com.example.vmsuser.ui.screens.home.HomeScreen
import com.example.vmsuser.ui.screens.kyc.*
import com.example.vmsuser.ui.screens.play.*
import com.example.vmsuser.ui.screens.profile.*
import com.example.vmsuser.ui.screens.social.*
import com.example.vmsuser.ui.screens.tournaments.*

// Bottom nav is ONLY shown on the five root tab destinations.
// All sub-screens (detail, flow, settings) are full-screen without a nav bar.
private val tabRoutes = setOf(
    Screen.Home.route,
    Screen.Play.route,
    Screen.Tournaments.route,
    Screen.Societies.route,
    Screen.Profile.route,
    Screen.Captain.route,
    Screen.Chat.route,
)

// Maps any route (including sub-screen routes) to the tab that owns it.
// Used by BottomNav to highlight the correct tab when deep inside a flow.
fun parentTabRoute(route: String?): String? = when {
    route == null -> null
    route == Screen.Home.route -> Screen.Home.route
    route.startsWith("play") || route.startsWith("queue") ||
        route.startsWith("active_match") || route == Screen.OpenMatches.route -> Screen.Play.route
    route.startsWith("tournament") -> Screen.Tournaments.route
    route.startsWith("societ") || route == Screen.CreateSociety.route -> Screen.Societies.route
    route == Screen.Profile.route || route == Screen.EditProfile.route ||
        route == Screen.Settings.route || route == Screen.Wallet.route ||
        route == Screen.Notifications.route || route == Screen.Support.route -> Screen.Profile.route
    route.startsWith("captain") || route.startsWith("kyc") ||
        route == Screen.BecomeACaptain.route || route == Screen.CaptainApplication.route ||
        route == Screen.CaptainEarnings.route -> Screen.Captain.route
    route.startsWith("chat") -> Screen.Chat.route
    else -> null
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in tabRoutes

    val user by UserSession.user.collectAsState()
    val pendingDeepLink = PendingDeepLink.route
    LaunchedEffect(pendingDeepLink, user) {
        if (pendingDeepLink != null && user != null) {
            navController.navigate(pendingDeepLink)
            PendingDeepLink.route = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Auth
            composable(Screen.Splash.route) { SplashScreen(navController) }
            composable(Screen.Phone.route) { PhoneInputScreen(navController) }
            composable(
                Screen.Otp.route,
                arguments = listOf(navArgument("phone") { type = NavType.StringType })
            ) { entry ->
                OtpScreen(navController, phone = entry.arguments?.getString("phone") ?: "")
            }
            composable(Screen.ProfileSetup.route) { ProfileSetupScreen(navController) }

            // Main
            composable(Screen.Home.route) { HomeScreen(navController) }

            // Play
            composable(Screen.Play.route) { PlayScreen(navController) }
            composable(
                Screen.Queue.route,
                arguments = listOf(navArgument("sport") { type = NavType.StringType })
            ) { entry ->
                QueueTrackerScreen(navController, sport = entry.arguments?.getString("sport") ?: "")
            }
            composable(
                Screen.ActiveMatch.route,
                arguments = listOf(navArgument("matchId") { type = NavType.IntType })
            ) { entry ->
                ActiveMatchScreen(navController, matchId = entry.arguments?.getInt("matchId") ?: 0)
            }
            composable(Screen.OpenMatches.route) { OpenMatchesScreen(navController) }

            // Tournaments
            composable(Screen.Tournaments.route) { TournamentsScreen(navController) }
            composable(
                Screen.TournamentDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                TournamentDetailScreen(navController, id = entry.arguments?.getInt("id") ?: 0)
            }

            // Chat
            composable(Screen.Chat.route) { ChatListScreen(navController) }
            composable(
                Screen.ChatThread.route,
                arguments = listOf(navArgument("threadId") { type = NavType.StringType })
            ) { entry ->
                ChatThreadScreen(navController, threadId = entry.arguments?.getString("threadId") ?: "")
            }

            // Social
            composable(Screen.Societies.route) { SocietiesScreen(navController) }
            composable(
                Screen.SocietyDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                SocietyDetailScreen(navController, id = entry.arguments?.getInt("id") ?: 0)
            }
            composable(Screen.CreateSociety.route) { CreateSocietyScreen(navController) }

            // Profile
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.Wallet.route) { WalletScreen(navController) }
            composable(Screen.Notifications.route) { NotificationsScreen(navController) }
            composable(Screen.Support.route) { SupportScreen(navController) }
            composable(
                Screen.TicketDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                TicketDetailScreen(navController, ticketId = entry.arguments?.getInt("id") ?: 0)
            }

            // Captain
            composable(Screen.Captain.route) { CaptainDashboardScreen(navController) }
            composable(
                Screen.CaptainMatch.route,
                arguments = listOf(navArgument("matchId") { type = NavType.IntType })
            ) { entry ->
                CaptainMatchDetailScreen(navController, matchId = entry.arguments?.getInt("matchId") ?: 0)
            }
            composable(Screen.CaptainEarnings.route) { CaptainEarningsScreen(navController) }
            composable(Screen.BecomeACaptain.route) { BecomeACaptainScreen(navController) }
            composable(Screen.CaptainApplication.route) { CaptainApplicationScreen(navController) }
            composable(Screen.KycIntro.route) { KycIntroScreen(navController) }
            composable(Screen.KycUpload.route) { KycUploadScreen(navController) }
            composable(Screen.KycSubmitted.route) { KycSubmittedScreen(navController) }
            composable(Screen.KycStatus.route) { KycStatusScreen(navController) }
        }

        if (showBottomNav) {
            PlixoBottomNav(
                currentRoute = currentRoute ?: "",
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                isCaptain = UserSession.isCaptain,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        }
    }
}

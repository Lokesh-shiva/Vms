package com.example.vmsuser.navigation

sealed class Screen(val route: String) {
    // Auth
    object Splash : Screen("splash")
    object Phone : Screen("phone")
    object Otp : Screen("otp/{phone}") {
        fun create(phone: String) = "otp/$phone"
    }
    object ProfileSetup : Screen("profile_setup")

    // Main
    object Home : Screen("home")

    // Play
    object Play : Screen("play")
    object Queue : Screen("queue/{sport}") {
        fun create(sport: String) = "queue/$sport"
    }
    object ActiveMatch : Screen("active_match/{matchId}") {
        fun create(matchId: Int) = "active_match/$matchId"
    }
    object OpenMatches : Screen("open_matches")

    // Shop
    object Shop : Screen("shop")
    object Checkout : Screen("checkout")
    object OrderPayment : Screen("order_payment/{orderId}") {
        fun create(orderId: Int) = "order_payment/$orderId"
    }
    object Orders : Screen("orders")

    // Trainers
    object Trainers : Screen("trainers")
    object TrainerDetail : Screen("trainer_detail/{id}") {
        fun create(id: Int) = "trainer_detail/$id"
    }
    object TrainerBookingPayment : Screen("trainer_booking_payment/{bookingId}") {
        fun create(bookingId: Int) = "trainer_booking_payment/$bookingId"
    }
    object TrainerBookings : Screen("trainer_bookings")

    // Tournaments
    object Tournaments : Screen("tournaments")
    object TournamentDetail : Screen("tournament_detail/{id}") {
        fun create(id: Int) = "tournament_detail/$id"
    }

    // Chat
    object Chat : Screen("chat")
    object ChatThread : Screen("chat_thread/{threadId}") {
        fun create(threadId: String) = "chat_thread/$threadId"
    }

    // Social
    object Societies : Screen("societies")
    object SocietyDetail : Screen("society_detail/{id}") {
        fun create(id: Int) = "society_detail/$id"
    }
    object CreateSociety : Screen("create_society")

    // Profile
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
    object Wallet : Screen("wallet")
    object Notifications : Screen("notifications")
    object Support : Screen("support")
    object TicketDetail : Screen("ticket_detail/{id}") {
        fun create(id: Int) = "ticket_detail/$id"
    }

    // Captain
    object Captain : Screen("captain")
    object CaptainMatch : Screen("captain_match/{matchId}") {
        fun create(matchId: Int) = "captain_match/$matchId"
    }
    object CaptainEarnings : Screen("captain_earnings")
    object BecomeACaptain : Screen("become_captain")
    object CaptainApplication : Screen("captain_application")
    object KycIntro : Screen("kyc_intro")
    object KycUpload : Screen("kyc_upload")
    object KycSubmitted : Screen("kyc_submitted")
    object KycStatus : Screen("kyc_status")
}

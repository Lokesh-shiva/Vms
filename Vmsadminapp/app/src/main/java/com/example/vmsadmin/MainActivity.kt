package com.example.vmsadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.vmsadmin.data.AuditLogRepository
import com.example.vmsadmin.data.BookingRepository
import com.example.vmsadmin.data.OrderRepository
import com.example.vmsadmin.data.SocietyRepository
import com.example.vmsadmin.data.VoteRoundRepository
import com.example.vmsadmin.data.CartRepository
import com.example.vmsadmin.data.CartTypeRepository
import com.example.vmsadmin.data.DashboardRepository
import com.example.vmsadmin.data.FeeConfigRepository
import com.example.vmsadmin.data.ItemRepository
import com.example.vmsadmin.data.PaymentRepository
import com.example.vmsadmin.data.GroundRepository
import com.example.vmsadmin.data.MatchRepository
import com.example.vmsadmin.data.CaptainRepository
import com.example.vmsadmin.data.DisputeRepository
import com.example.vmsadmin.data.TournamentRepository
import com.example.vmsadmin.data.QueueRepository
import com.example.vmsadmin.data.SystemConfigRepository
import com.example.vmsadmin.data.UserManagementRepository
import com.example.vmsadmin.data.RegionRepository
import com.example.vmsadmin.data.TimeslotRepository
import com.example.vmsadmin.data.TokenManager
import com.example.vmsadmin.navigation.AppNavigation
import com.example.vmsadmin.network.ApiClient
import com.example.vmsadmin.ui.theme.VmsAdminTheme
import com.example.vmsadmin.viewmodel.AuditLogViewModel
import com.example.vmsadmin.viewmodel.AuditLogViewModelFactory
import com.example.vmsadmin.viewmodel.AuthViewModel
import com.example.vmsadmin.viewmodel.OrderViewModel
import com.example.vmsadmin.viewmodel.OrderViewModelFactory
import com.example.vmsadmin.viewmodel.SocietyViewModel
import com.example.vmsadmin.viewmodel.SocietyViewModelFactory
import com.example.vmsadmin.viewmodel.VoteRoundViewModel
import com.example.vmsadmin.viewmodel.VoteRoundViewModelFactory
import com.example.vmsadmin.viewmodel.AuthViewModelFactory
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.BookingViewModelFactory
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.CartTypeViewModelFactory
import com.example.vmsadmin.viewmodel.CartViewModel
import com.example.vmsadmin.viewmodel.CartViewModelFactory
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.DashboardViewModelFactory
import com.example.vmsadmin.viewmodel.FeeConfigViewModel
import com.example.vmsadmin.viewmodel.FeeConfigViewModelFactory
import com.example.vmsadmin.viewmodel.ItemViewModel
import com.example.vmsadmin.viewmodel.ItemViewModelFactory
import com.example.vmsadmin.viewmodel.GroundViewModel
import com.example.vmsadmin.viewmodel.GroundViewModelFactory
import com.example.vmsadmin.viewmodel.MatchViewModel
import com.example.vmsadmin.viewmodel.MatchViewModelFactory
import com.example.vmsadmin.viewmodel.UserManagementViewModel
import com.example.vmsadmin.viewmodel.UserManagementViewModelFactory
import com.example.vmsadmin.viewmodel.PaymentViewModel
import com.example.vmsadmin.viewmodel.PaymentViewModelFactory
import com.example.vmsadmin.viewmodel.CaptainViewModel
import com.example.vmsadmin.viewmodel.CaptainViewModelFactory
import com.example.vmsadmin.viewmodel.DisputeViewModel
import com.example.vmsadmin.viewmodel.DisputeViewModelFactory
import com.example.vmsadmin.viewmodel.TournamentViewModel
import com.example.vmsadmin.viewmodel.TournamentViewModelFactory
import com.example.vmsadmin.viewmodel.QueueOverviewViewModel
import com.example.vmsadmin.viewmodel.QueueOverviewViewModelFactory
import com.example.vmsadmin.viewmodel.RegionViewModel
import com.example.vmsadmin.viewmodel.RegionViewModelFactory
import com.example.vmsadmin.viewmodel.SystemConfigViewModel
import com.example.vmsadmin.viewmodel.SystemConfigViewModelFactory
import com.example.vmsadmin.viewmodel.TimeslotViewModel
import com.example.vmsadmin.viewmodel.TimeslotViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(this)
        val apiService = ApiClient.create(tokenManager)
        
        val authViewModelFactory = AuthViewModelFactory(apiService, tokenManager)
        val authViewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        val paymentRepository = PaymentRepository(apiService)
        val paymentViewModelFactory = PaymentViewModelFactory(paymentRepository)
        val paymentViewModel = ViewModelProvider(this, paymentViewModelFactory)[PaymentViewModel::class.java]

        val bookingRepository = BookingRepository(apiService)
        val bookingViewModelFactory = BookingViewModelFactory(bookingRepository)
        val bookingViewModel = ViewModelProvider(this, bookingViewModelFactory)[BookingViewModel::class.java]

        val dashboardRepository = DashboardRepository(apiService)
        val dashboardViewModelFactory = DashboardViewModelFactory(dashboardRepository)
        val dashboardViewModel = ViewModelProvider(this, dashboardViewModelFactory)[DashboardViewModel::class.java]

        val regionRepository = RegionRepository(apiService)
        val regionViewModelFactory = RegionViewModelFactory(regionRepository)
        val regionViewModel = ViewModelProvider(this, regionViewModelFactory)[RegionViewModel::class.java]

        val cartTypeRepository = CartTypeRepository(apiService)
        val cartTypeViewModelFactory = CartTypeViewModelFactory(cartTypeRepository)
        val cartTypeViewModel = ViewModelProvider(this, cartTypeViewModelFactory)[CartTypeViewModel::class.java]

        val timeslotRepository = TimeslotRepository(apiService)
        val timeslotViewModelFactory = TimeslotViewModelFactory(timeslotRepository)
        val timeslotViewModel = ViewModelProvider(this, timeslotViewModelFactory)[TimeslotViewModel::class.java]

        val cartRepository = CartRepository(apiService)
        val cartViewModelFactory = CartViewModelFactory(cartRepository)
        val cartViewModel = ViewModelProvider(this, cartViewModelFactory)[CartViewModel::class.java]

        val feeConfigRepository = FeeConfigRepository(apiService)
        val feeConfigViewModelFactory = FeeConfigViewModelFactory(feeConfigRepository)
        val feeConfigViewModel = ViewModelProvider(this, feeConfigViewModelFactory)[FeeConfigViewModel::class.java]

        val itemRepository = ItemRepository(apiService)
        val itemViewModelFactory = ItemViewModelFactory(itemRepository, cartTypeRepository)
        val itemViewModel = ViewModelProvider(this, itemViewModelFactory)[ItemViewModel::class.java]

        val matchRepository = MatchRepository(apiService)
        val matchViewModelFactory = MatchViewModelFactory(matchRepository)
        val matchViewModel = ViewModelProvider(this, matchViewModelFactory)[MatchViewModel::class.java]

        val groundRepository = GroundRepository(apiService)
        val groundViewModelFactory = GroundViewModelFactory(groundRepository)
        val groundViewModel = ViewModelProvider(this, groundViewModelFactory)[GroundViewModel::class.java]

        val userManagementRepository = UserManagementRepository(apiService)
        val initialUserId = runBlocking { tokenManager.userIdFlow.firstOrNull() }
        val userManagementViewModelFactory = UserManagementViewModelFactory(userManagementRepository, initialUserId)
        val userManagementViewModel = ViewModelProvider(this, userManagementViewModelFactory)[UserManagementViewModel::class.java]

        val systemConfigRepository = SystemConfigRepository(apiService)
        val systemConfigViewModelFactory = SystemConfigViewModelFactory(systemConfigRepository)
        val systemConfigViewModel = ViewModelProvider(this, systemConfigViewModelFactory)[SystemConfigViewModel::class.java]

        val queueRepository = QueueRepository(apiService)
        val queueOverviewViewModelFactory = QueueOverviewViewModelFactory(queueRepository)
        val queueOverviewViewModel = ViewModelProvider(this, queueOverviewViewModelFactory)[QueueOverviewViewModel::class.java]

        val captainRepository = CaptainRepository(apiService)
        val captainViewModelFactory = CaptainViewModelFactory(captainRepository)
        val captainViewModel = ViewModelProvider(this, captainViewModelFactory)[CaptainViewModel::class.java]

        val tournamentRepository = TournamentRepository(apiService)
        val tournamentViewModelFactory = TournamentViewModelFactory(tournamentRepository)
        val tournamentViewModel = ViewModelProvider(this, tournamentViewModelFactory)[TournamentViewModel::class.java]

        val disputeRepository = DisputeRepository(apiService)
        val disputeViewModelFactory = DisputeViewModelFactory(disputeRepository)
        val disputeViewModel = ViewModelProvider(this, disputeViewModelFactory)[DisputeViewModel::class.java]

        val auditLogRepository = AuditLogRepository(apiService)
        val auditLogViewModelFactory = AuditLogViewModelFactory(auditLogRepository)
        val auditLogViewModel = ViewModelProvider(this, auditLogViewModelFactory)[AuditLogViewModel::class.java]

        val societyRepository = SocietyRepository(apiService)
        val societyViewModelFactory = SocietyViewModelFactory(societyRepository)
        val societyViewModel = ViewModelProvider(this, societyViewModelFactory)[SocietyViewModel::class.java]

        val voteRoundRepository = VoteRoundRepository(apiService)
        val voteRoundViewModelFactory = VoteRoundViewModelFactory(voteRoundRepository)
        val voteRoundViewModel = ViewModelProvider(this, voteRoundViewModelFactory)[VoteRoundViewModel::class.java]

        val orderRepository = OrderRepository(apiService)
        val orderViewModelFactory = OrderViewModelFactory(orderRepository)
        val orderViewModel = ViewModelProvider(this, orderViewModelFactory)[OrderViewModel::class.java]

        val initialToken = runBlocking { tokenManager.tokenFlow.firstOrNull() }
        val startDestination = if (initialToken.isNullOrEmpty()) "login" else "main"

        setContent {
            VmsAdminTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        authViewModel = authViewModel,
                        dashboardViewModel = dashboardViewModel,
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
                        systemConfigViewModel = systemConfigViewModel,
                        queueOverviewViewModel = queueOverviewViewModel,
                        captainViewModel = captainViewModel,
                        tournamentViewModel = tournamentViewModel,
                        disputeViewModel = disputeViewModel,
                        auditLogViewModel = auditLogViewModel,
                        societyViewModel = societyViewModel,
                        voteRoundViewModel = voteRoundViewModel,
                        orderViewModel = orderViewModel,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
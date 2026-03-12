package com.example.vmsadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.vmsadmin.data.BookingRepository
import com.example.vmsadmin.data.CartTypeRepository
import com.example.vmsadmin.data.PaymentRepository
import com.example.vmsadmin.data.RegionRepository
import com.example.vmsadmin.data.TimeslotRepository
import com.example.vmsadmin.data.TokenManager
import com.example.vmsadmin.navigation.AppNavigation
import com.example.vmsadmin.network.ApiClient
import com.example.vmsadmin.ui.theme.VmsAdminTheme
import com.example.vmsadmin.viewmodel.AuthViewModel
import com.example.vmsadmin.viewmodel.AuthViewModelFactory
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.BookingViewModelFactory
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.CartTypeViewModelFactory
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.DashboardViewModelFactory
import com.example.vmsadmin.viewmodel.PaymentViewModel
import com.example.vmsadmin.viewmodel.PaymentViewModelFactory
import com.example.vmsadmin.viewmodel.RegionViewModel
import com.example.vmsadmin.viewmodel.RegionViewModelFactory
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

        val dashboardViewModelFactory = DashboardViewModelFactory(paymentRepository, bookingRepository)
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

        val initialToken = runBlocking { tokenManager.tokenFlow.firstOrNull() }
        val startDestination = if (initialToken.isNullOrEmpty()) "login" else "main"

        setContent {
            VmsAdminTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    dashboardViewModel = dashboardViewModel,
                    paymentViewModel = paymentViewModel,
                    bookingViewModel = bookingViewModel,
                    regionViewModel = regionViewModel,
                    cartTypeViewModel = cartTypeViewModel,
                    timeslotViewModel = timeslotViewModel,
                    startDestination = startDestination
                )
            }
        }
    }
}
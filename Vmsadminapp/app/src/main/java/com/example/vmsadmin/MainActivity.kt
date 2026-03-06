package com.example.vmsadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.vmsadmin.data.BookingRepository
import com.example.vmsadmin.data.PaymentRepository
import com.example.vmsadmin.data.TokenManager
import com.example.vmsadmin.navigation.AppNavigation
import com.example.vmsadmin.network.ApiClient
import com.example.vmsadmin.ui.theme.VmsAdminTheme
import com.example.vmsadmin.viewmodel.AuthViewModel
import com.example.vmsadmin.viewmodel.AuthViewModelFactory
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.BookingViewModelFactory
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.DashboardViewModelFactory
import com.example.vmsadmin.viewmodel.PaymentViewModel
import com.example.vmsadmin.viewmodel.PaymentViewModelFactory
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

        val initialToken = runBlocking { tokenManager.tokenFlow.firstOrNull() }
        val startDestination = if (initialToken.isNullOrEmpty()) "login" else "main"

        setContent {
            VmsAdminTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    dashboardViewModel = dashboardViewModel,
                    paymentViewModel = paymentViewModel,
                    bookingViewModel = bookingViewModel,
                    startDestination = startDestination
                )
            }
        }
    }
}
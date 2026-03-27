package com.example.vmsuser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.vmsuser.data.AddressManager
import com.example.vmsuser.data.TokenManager
import com.example.vmsuser.navigation.AppNavigation
import com.example.vmsuser.network.ApiClient
import com.example.vmsuser.repository.AuthRepository
import com.example.vmsuser.repository.BookingRepository
import com.example.vmsuser.repository.ItemRepository
import com.example.vmsuser.repository.LocationRepository
import com.example.vmsuser.repository.MatchRepository
import com.example.vmsuser.repository.PaymentRepository
import com.example.vmsuser.ui.theme.VmsUserTheme
import com.example.vmsuser.viewmodel.AuthViewModel
import com.example.vmsuser.viewmodel.AuthViewModelFactory
import com.example.vmsuser.viewmodel.BookingViewModel
import com.example.vmsuser.viewmodel.BookingViewModelFactory
import com.example.vmsuser.viewmodel.HomeViewModel
import com.example.vmsuser.viewmodel.HomeViewModelFactory
import com.example.vmsuser.viewmodel.MatchViewModel
import com.example.vmsuser.viewmodel.MatchViewModelFactory
import com.example.vmsuser.viewmodel.PaymentViewModel
import com.example.vmsuser.viewmodel.PaymentViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(this)
        val addressManager = AddressManager(this)
        val apiService = ApiClient.create(tokenManager)

        val authRepository = AuthRepository(apiService, tokenManager)
        val authViewModelFactory = AuthViewModelFactory(authRepository)
        val authViewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        val locationRepository = LocationRepository(apiService)
        val itemRepository = ItemRepository(apiService)
        val homeViewModelFactory = HomeViewModelFactory(locationRepository, itemRepository)
        val homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

        val bookingRepository = BookingRepository(apiService)
        val bookingViewModelFactory = BookingViewModelFactory(bookingRepository)
        val bookingViewModel = ViewModelProvider(this, bookingViewModelFactory)[BookingViewModel::class.java]

        val paymentRepository = PaymentRepository(apiService)
        val paymentViewModelFactory = PaymentViewModelFactory(paymentRepository)
        val paymentViewModel = ViewModelProvider(this, paymentViewModelFactory)[PaymentViewModel::class.java]

        val matchRepository = MatchRepository(apiService)
        val matchViewModelFactory = MatchViewModelFactory(matchRepository)
        val matchViewModel = ViewModelProvider(this, matchViewModelFactory)[MatchViewModel::class.java]

        val initialToken = runBlocking { tokenManager.tokenFlow.firstOrNull() }
        val startDestination = if (initialToken.isNullOrEmpty()) "login" else "home"

        setContent {
            VmsUserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        authViewModel = authViewModel,
                        homeViewModel = homeViewModel,
                        bookingViewModel = bookingViewModel,
                        paymentViewModel = paymentViewModel,
                        matchViewModel = matchViewModel,
                        addressManager = addressManager,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}

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
import com.example.vmsadmin.data.BookingRepository
import com.example.vmsadmin.data.CartRepository
import com.example.vmsadmin.data.CartTypeRepository
import com.example.vmsadmin.data.DashboardRepository
import com.example.vmsadmin.data.FeeConfigRepository
import com.example.vmsadmin.data.ItemRepository
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
import com.example.vmsadmin.viewmodel.CartViewModel
import com.example.vmsadmin.viewmodel.CartViewModelFactory
import com.example.vmsadmin.viewmodel.DashboardViewModel
import com.example.vmsadmin.viewmodel.DashboardViewModelFactory
import com.example.vmsadmin.viewmodel.FeeConfigViewModel
import com.example.vmsadmin.viewmodel.FeeConfigViewModelFactory
import com.example.vmsadmin.viewmodel.ItemViewModel
import com.example.vmsadmin.viewmodel.ItemViewModelFactory
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
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
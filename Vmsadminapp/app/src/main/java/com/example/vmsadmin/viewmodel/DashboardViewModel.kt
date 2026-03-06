package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.BookingRepository
import com.example.vmsadmin.data.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val totalBookings: Int = 0,
    val activeBookings: Int = 0,
    val pendingPayments: Int = 0,
    val completedBookings: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val bookings = bookingRepository.fetchBookings()

                var pendingPayments = 0
                try {
                    val payments = paymentRepository.fetchPayments()
                    pendingPayments = payments.count { it.status == "UNDER_REVIEW" }
                } catch (_: Exception) {
                    // Payment fetch may fail independently
                }

                val totalBookings = bookings.size
                val activeBookings = bookings.count {
                    it.status in listOf("PENDING_PAYMENT", "CONFIRMED", "IN_PROGRESS")
                }
                val completedBookings = bookings.count { it.status == "COMPLETED" }

                _uiState.value = DashboardUiState(
                    totalBookings = totalBookings,
                    activeBookings = activeBookings,
                    pendingPayments = pendingPayments,
                    completedBookings = completedBookings,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard"
                )
            }
        }
    }
}

class DashboardViewModelFactory(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(paymentRepository, bookingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.PaymentRepository
import com.example.vmsadmin.models.Payment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PaymentFilter { PENDING_REVIEW, ALL }

data class PaymentUiState(
    val payments: List<Payment> = emptyList(),   // filtered view (what the UI renders)
    val filter: PaymentFilter = PaymentFilter.PENDING_REVIEW,
    val totalRevenue: Double = 0.0,              // sum of SUCCESS payment amounts
    val pendingReviewCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PaymentViewModel(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    // Full unfiltered list kept in memory; UI list derived from this + current filter
    private var allPayments: List<Payment> = emptyList()

    fun loadPayments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                allPayments = paymentRepository.fetchPayments()
                _uiState.value = applyFilter(_uiState.value.filter).copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun setFilter(filter: PaymentFilter) {
        _uiState.value = applyFilter(filter)
    }

    private fun applyFilter(filter: PaymentFilter): PaymentUiState {
        val visible = when (filter) {
            PaymentFilter.PENDING_REVIEW -> allPayments.filter { it.status == "UNDER_REVIEW" }
            PaymentFilter.ALL -> allPayments
        }
        val revenue = allPayments
            .filter { it.status == "SUCCESS" }
            .sumOf { it.amount ?: 0.0 }
        val pendingCount = allPayments.count { it.status == "UNDER_REVIEW" }
        return _uiState.value.copy(
            payments = visible,
            filter = filter,
            totalRevenue = revenue,
            pendingReviewCount = pendingCount,
            error = null
        )
    }

    fun approvePayment(paymentId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                paymentRepository.approvePayment(paymentId)
                loadPayments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to approve payment"
                )
            }
        }
    }

    fun rejectPayment(paymentId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                paymentRepository.rejectPayment(paymentId)
                loadPayments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to reject payment"
                )
            }
        }
    }

    fun refundPayment(paymentId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                paymentRepository.refundPayment(paymentId)
                loadPayments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to refund payment"
                )
            }
        }
    }
}

class PaymentViewModelFactory(
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentViewModel(paymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

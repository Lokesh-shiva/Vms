package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.models.InitiatePaymentResponse
import com.example.vmsuser.models.Payment
import com.example.vmsuser.models.UiState
import com.example.vmsuser.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel(private val paymentRepository: PaymentRepository) : ViewModel() {

    private val _initiateState = MutableStateFlow<UiState<InitiatePaymentResponse>>(UiState.Idle)
    val initiateState: StateFlow<UiState<InitiatePaymentResponse>> = _initiateState.asStateFlow()

    private val _confirmState = MutableStateFlow<UiState<Payment>>(UiState.Idle)
    val confirmState: StateFlow<UiState<Payment>> = _confirmState.asStateFlow()

    fun initiatePayment(bookingId: Int) {
        if (_initiateState.value is UiState.Loading) return
        viewModelScope.launch {
            _initiateState.value = UiState.Loading
            try {
                val result = paymentRepository.initiatePayment(bookingId)
                _initiateState.value = UiState.Success(result)
            } catch (e: Exception) {
                _initiateState.value = UiState.Error(e.message ?: "Failed to initiate payment")
            }
        }
    }

    fun submitTransactionId(bookingId: Int, transactionId: String) {
        if (_confirmState.value is UiState.Loading) return
        if (transactionId.isBlank()) {
            _confirmState.value = UiState.Error("Transaction ID is required")
            return
        }
        viewModelScope.launch {
            _confirmState.value = UiState.Loading
            try {
                val payment = paymentRepository.submitTransactionId(bookingId, transactionId)
                _confirmState.value = UiState.Success(payment)
            } catch (e: Exception) {
                _confirmState.value = UiState.Error(e.message ?: "Failed to submit transaction ID")
            }
        }
    }

    fun resetInitiateState() {
        _initiateState.value = UiState.Idle
    }

    fun resetConfirmState() {
        _confirmState.value = UiState.Idle
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

package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.OrderRepository
import com.example.vmsadmin.models.AdminOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _orders = MutableStateFlow<List<AdminOrder>>(emptyList())
    val orders: StateFlow<List<AdminOrder>> = _orders.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pendingIds = MutableStateFlow<Set<Int>>(emptySet())
    val pendingIds: StateFlow<Set<Int>> = _pendingIds.asStateFlow()

    init { loadOrders() }

    fun loadOrders(status: String? = "UNDER_REVIEW") {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _orders.value = repository.getOrders(status)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load orders"
            } finally {
                _loading.value = false
            }
        }
    }

    fun approveOrder(orderId: Int) {
        viewModelScope.launch {
            _pendingIds.value = _pendingIds.value + orderId
            try {
                repository.approveOrder(orderId)
                _orders.value = _orders.value.filterNot { it.id == orderId }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to approve order"
            } finally {
                _pendingIds.value = _pendingIds.value - orderId
            }
        }
    }

    fun rejectOrder(orderId: Int) {
        viewModelScope.launch {
            _pendingIds.value = _pendingIds.value + orderId
            try {
                repository.rejectOrder(orderId)
                _orders.value = _orders.value.filterNot { it.id == orderId }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reject order"
            } finally {
                _pendingIds.value = _pendingIds.value - orderId
            }
        }
    }

    fun clearError() { _error.value = null }
}

class OrderViewModelFactory(
    private val repository: OrderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.TrainerRepository
import com.example.vmsadmin.models.AdminTrainerBooking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrainerBookingViewModel(private val repository: TrainerRepository) : ViewModel() {

    private val _bookings = MutableStateFlow<List<AdminTrainerBooking>>(emptyList())
    val bookings: StateFlow<List<AdminTrainerBooking>> = _bookings.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pendingIds = MutableStateFlow<Set<Int>>(emptySet())
    val pendingIds: StateFlow<Set<Int>> = _pendingIds.asStateFlow()

    init { loadBookings() }

    fun loadBookings(status: String? = "UNDER_REVIEW") {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _bookings.value = repository.getBookings(status)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load bookings"
            } finally {
                _loading.value = false
            }
        }
    }

    fun approveBooking(id: Int) {
        viewModelScope.launch {
            _pendingIds.value = _pendingIds.value + id
            try {
                repository.approveBooking(id)
                _bookings.value = _bookings.value.filterNot { it.id == id }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to approve booking"
            } finally {
                _pendingIds.value = _pendingIds.value - id
            }
        }
    }

    fun rejectBooking(id: Int) {
        viewModelScope.launch {
            _pendingIds.value = _pendingIds.value + id
            try {
                repository.rejectBooking(id)
                _bookings.value = _bookings.value.filterNot { it.id == id }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reject booking"
            } finally {
                _pendingIds.value = _pendingIds.value - id
            }
        }
    }

    fun clearError() { _error.value = null }
}

class TrainerBookingViewModelFactory(
    private val repository: TrainerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainerBookingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrainerBookingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

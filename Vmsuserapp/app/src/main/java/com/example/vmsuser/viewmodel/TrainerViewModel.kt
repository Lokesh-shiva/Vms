package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.TrainerRepository
import com.example.vmsuser.models.Trainer
import com.example.vmsuser.models.TrainerBookingDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrainerViewModel : ViewModel() {
    private val repo = TrainerRepository()

    private val _trainers = MutableStateFlow<List<Trainer>>(emptyList())
    val trainers: StateFlow<List<Trainer>> = _trainers
    private val _trainersLoading = MutableStateFlow(true)
    val trainersLoading: StateFlow<Boolean> = _trainersLoading
    private val _trainersError = MutableStateFlow<String?>(null)
    val trainersError: StateFlow<String?> = _trainersError

    private val _selectedTrainer = MutableStateFlow<Trainer?>(null)
    val selectedTrainer: StateFlow<Trainer?> = _selectedTrainer

    private val _booking = MutableStateFlow(false)
    val booking: StateFlow<Boolean> = _booking
    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError: StateFlow<String?> = _bookingError
    private val _lastBooking = MutableStateFlow<TrainerBookingDto?>(null)
    val lastBooking: StateFlow<TrainerBookingDto?> = _lastBooking

    private val _bookings = MutableStateFlow<List<TrainerBookingDto>>(emptyList())
    val bookings: StateFlow<List<TrainerBookingDto>> = _bookings
    private val _bookingsLoading = MutableStateFlow(false)
    val bookingsLoading: StateFlow<Boolean> = _bookingsLoading

    init { loadTrainers() }

    fun loadTrainers() {
        viewModelScope.launch {
            _trainersLoading.value = true
            _trainersError.value = null
            repo.getTrainers()
                .onSuccess { _trainers.value = it.filter { t -> t.isActive } }
                .onFailure { e -> _trainersError.value = e.message ?: "Failed to load trainers." }
            _trainersLoading.value = false
        }
    }

    fun selectTrainer(id: Int) {
        viewModelScope.launch {
            repo.getTrainer(id).onSuccess { _selectedTrainer.value = it }
                .onFailure { _selectedTrainer.value = _trainers.value.find { t -> t.id == id } }
        }
    }

    fun bookSession(trainerId: Int, sessionDate: String, sessionTime: String, onSuccess: (TrainerBookingDto) -> Unit) {
        viewModelScope.launch {
            _booking.value = true
            _bookingError.value = null
            repo.createBooking(trainerId, sessionDate, sessionTime)
                .onSuccess { booking ->
                    _lastBooking.value = booking
                    onSuccess(booking)
                }
                .onFailure { e -> _bookingError.value = e.message ?: "Failed to book session." }
            _booking.value = false
        }
    }

    fun loadBooking(id: Int) {
        viewModelScope.launch {
            repo.getBooking(id).onSuccess { _lastBooking.value = it }
        }
    }

    fun submitPayment(id: Int, transactionId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _booking.value = true
            _bookingError.value = null
            repo.submitPayment(id, transactionId)
                .onSuccess { _lastBooking.value = it; onSuccess() }
                .onFailure { e -> _bookingError.value = e.message ?: "Failed to submit payment." }
            _booking.value = false
        }
    }

    fun loadBookings() {
        viewModelScope.launch {
            _bookingsLoading.value = true
            repo.getMyBookings().onSuccess { _bookings.value = it }
            _bookingsLoading.value = false
        }
    }

    fun clearBookingError() { _bookingError.value = null }
}

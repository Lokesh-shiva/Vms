package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.models.Booking
import com.example.vmsuser.models.UiState
import com.example.vmsuser.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookingViewModel(private val bookingRepository: BookingRepository) : ViewModel() {

    private val _createState = MutableStateFlow<UiState<Booking>>(UiState.Idle)
    val createState: StateFlow<UiState<Booking>> = _createState.asStateFlow()

    private val _myBookingsState = MutableStateFlow<UiState<List<Booking>>>(UiState.Idle)
    val myBookingsState: StateFlow<UiState<List<Booking>>> = _myBookingsState.asStateFlow()

    private val _bookingDetailState = MutableStateFlow<UiState<Booking>>(UiState.Idle)
    val bookingDetailState: StateFlow<UiState<Booking>> = _bookingDetailState.asStateFlow()

    private val _cancelState = MutableStateFlow<UiState<Booking>>(UiState.Idle)
    val cancelState: StateFlow<UiState<Booking>> = _cancelState.asStateFlow()

    fun createBooking(
        regionId: Int,
        cartTypeId: Int,
        timeslotId: Int,
        address: String
    ) {
        if (_createState.value is UiState.Loading) return
        viewModelScope.launch {
            _createState.value = UiState.Loading
            try {
                val booking = bookingRepository.createBooking(regionId, cartTypeId, timeslotId, address)
                _createState.value = UiState.Success(booking)
            } catch (e: Exception) {
                _createState.value = UiState.Error(e.message ?: "Failed to create booking")
            }
        }
    }

    fun getMyBookings() {
        if (_myBookingsState.value is UiState.Loading) return
        viewModelScope.launch {
            _myBookingsState.value = UiState.Loading
            try {
                val bookings = bookingRepository.getMyBookings()
                _myBookingsState.value = UiState.Success(bookings)
            } catch (e: Exception) {
                _myBookingsState.value = UiState.Error(e.message ?: "Failed to fetch bookings")
            }
        }
    }

    fun refreshBookingStatus(bookingId: Int) {
        viewModelScope.launch {
            try {
                val booking = bookingRepository.getBooking(bookingId)
                _bookingDetailState.value = UiState.Success(booking)
            } catch (e: Exception) {
                _bookingDetailState.value = UiState.Error(e.message ?: "Failed to refresh booking")
            }
        }
    }

    fun cancelBooking(bookingId: Int) {
        if (_cancelState.value is UiState.Loading) return
        viewModelScope.launch {
            _cancelState.value = UiState.Loading
            try {
                val booking = bookingRepository.cancelBooking(bookingId)
                _cancelState.value = UiState.Success(booking)
                getMyBookings()
            } catch (e: Exception) {
                _cancelState.value = UiState.Error(e.message ?: "Failed to cancel booking")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = UiState.Idle
    }

    fun resetCancelState() {
        _cancelState.value = UiState.Idle
    }
}

class BookingViewModelFactory(
    private val bookingRepository: BookingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookingViewModel(bookingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.vmsadmin.data

import com.example.vmsadmin.models.Booking
import com.example.vmsadmin.network.ApiService

class BookingRepository(private val apiService: ApiService) {

    suspend fun fetchBookings(): List<Booking> {
        val response = apiService.getBookings()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch bookings")
    }

    suspend fun startBooking(bookingId: Int) {
        val response = apiService.startBooking(bookingId)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to start booking")
        }
    }

    suspend fun completeBooking(bookingId: Int) {
        val response = apiService.completeBooking(bookingId)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to complete booking")
        }
    }

    suspend fun cancelBooking(bookingId: Int) {
        val response = apiService.cancelBooking(bookingId)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to cancel booking")
        }
    }

    suspend fun startSession(bookingId: Int) {
        val response = apiService.startSession(bookingId)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to start session")
        }
    }

    suspend fun endSession(bookingId: Int) {
        val response = apiService.endSession(bookingId)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to end session")
        }
    }
}

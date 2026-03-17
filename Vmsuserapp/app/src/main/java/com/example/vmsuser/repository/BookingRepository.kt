package com.example.vmsuser.repository

import com.example.vmsuser.models.Booking
import com.example.vmsuser.models.CreateBookingRequest
import com.example.vmsuser.network.ApiService
import retrofit2.HttpException

class BookingRepository(private val apiService: ApiService) : BaseRepository() {

    suspend fun createBooking(
        regionId: Int,
        cartTypeId: Int,
        timeslotId: Int,
        address: String
    ): Booking {
        try {
            val response = apiService.createBooking(
                CreateBookingRequest(
                    region_id = regionId,
                    cart_type_id = cartTypeId,
                    timeslot_id = timeslotId,
                    address = address
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to create booking")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create booking")
        }
    }

    suspend fun getBooking(bookingId: Int): Booking {
        val response = apiService.getBooking(bookingId)
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Booking not found")
    }

    suspend fun getMyBookings(): List<Booking> {
        val response = apiService.getMyBookings()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch bookings")
    }

    suspend fun cancelBooking(bookingId: Int): Booking {
        try {
            val response = apiService.cancelBooking(bookingId)
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to cancel booking")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to cancel booking")
        }
    }
}

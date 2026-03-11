package com.example.vmsadmin.network

import com.example.vmsadmin.models.ApiResponse
import com.example.vmsadmin.models.Booking
import com.example.vmsadmin.models.CreateRegionRequest
import com.example.vmsadmin.models.LoginRequest
import com.example.vmsadmin.models.LoginResponse
import com.example.vmsadmin.models.Payment
import com.example.vmsadmin.models.PaymentConfig
import com.example.vmsadmin.models.Region
import com.example.vmsadmin.models.UpdateRegionRequest
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @GET("/api/v1/bookings")
    suspend fun getBookings(): ApiResponse<List<Booking>>

    @POST("/api/v1/bookings/{booking_id}/start")
    suspend fun startBooking(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/bookings/{booking_id}/complete")
    suspend fun completeBooking(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/bookings/{booking_id}/cancel")
    suspend fun cancelBooking(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

    @GET("/api/v1/payments/config")
    suspend fun getPaymentsConfig(): ApiResponse<PaymentConfig>

    @GET("/api/v1/payments")
    suspend fun getPayments(): ApiResponse<List<Payment>>

    @POST("/api/v1/payments/approve/{payment_id}")
    suspend fun approvePayment(@Path("payment_id") paymentId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/payments/reject/{payment_id}")
    suspend fun rejectPayment(@Path("payment_id") paymentId: Int): ApiResponse<JsonElement>

    // ── Region (Location) endpoints ──────────────────────────────────
    @GET("/api/v1/locations")
    suspend fun getRegions(): ApiResponse<List<Region>>

    @POST("/api/v1/locations")
    suspend fun createRegion(@Body request: CreateRegionRequest): ApiResponse<Region>

    @PUT("/api/v1/locations/{location_id}")
    suspend fun updateRegion(
        @Path("location_id") locationId: Int,
        @Body request: UpdateRegionRequest
    ): ApiResponse<Region>

    @DELETE("/api/v1/locations/{location_id}")
    suspend fun deleteRegion(@Path("location_id") locationId: Int): ApiResponse<JsonElement>
}

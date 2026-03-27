package com.example.vmsuser.network

import com.example.vmsuser.models.ApiResponse
import com.example.vmsuser.models.Booking
import com.example.vmsuser.models.CartType
import com.example.vmsuser.models.ConfirmPaymentRequest
import com.example.vmsuser.models.CreateBookingRequest
import com.example.vmsuser.models.InitiatePaymentResponse
import com.example.vmsuser.models.Item
import com.example.vmsuser.models.LoginRequest
import com.example.vmsuser.models.LoginResponse
import com.example.vmsuser.models.Match
import com.example.vmsuser.models.CreateMatchRequest
import com.example.vmsuser.models.Payment
import com.example.vmsuser.models.Region
import com.example.vmsuser.models.RegisterRequest
import com.example.vmsuser.models.Timeslot
import com.example.vmsuser.models.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<User>

    @GET("/api/v1/auth/me")
    suspend fun getMe(): ApiResponse<User>

    // ── Config (public lookups) ───────────────────────────────────────
    @GET("/api/v1/locations")
    suspend fun getRegions(): ApiResponse<List<Region>>

    @GET("/api/v1/cart-types")
    suspend fun getCartTypes(): ApiResponse<List<CartType>>

    @GET("/api/v1/timeslots")
    suspend fun getTimeslots(): ApiResponse<List<Timeslot>>

    @GET("/api/v1/items")
    suspend fun getItems(): ApiResponse<List<Item>>

    // ── Bookings ─────────────────────────────────────────────────────
    @POST("/api/v1/bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): ApiResponse<Booking>

    @GET("/api/v1/bookings/{booking_id}")
    suspend fun getBooking(@Path("booking_id") bookingId: Int): ApiResponse<Booking>

    @GET("/api/v1/bookings")
    suspend fun getMyBookings(): ApiResponse<List<Booking>>

    @POST("/api/v1/bookings/{booking_id}/cancel")
    suspend fun cancelBooking(@Path("booking_id") bookingId: Int): ApiResponse<Booking>

    // ── Payments ──────────────────────────────────────────────────────
    @POST("/api/v1/payments/initiate/{booking_id}")
    suspend fun initiatePayment(@Path("booking_id") bookingId: Int): ApiResponse<InitiatePaymentResponse>

    @POST("/api/v1/payments/confirm-manual/{booking_id}")
    suspend fun confirmManualPayment(
        @Path("booking_id") bookingId: Int,
        @Body request: ConfirmPaymentRequest
    ): ApiResponse<Payment>

    // ── Matches ───────────────────────────────────────────────
    @GET("/api/v1/matches")
    suspend fun getMatches(
        @Query("sport_id") sportId: Int? = null,
        @Query("region_id") regionId: Int? = null
    ): ApiResponse<List<Match>>

    @POST("/api/v1/matches")
    suspend fun createMatch(@Body request: CreateMatchRequest): ApiResponse<Match>

    @POST("/api/v1/matches/{match_id}/join")
    suspend fun joinMatch(@Path("match_id") matchId: Int): ApiResponse<Match>

    @POST("/api/v1/matches/{match_id}/leave")
    suspend fun leaveMatch(@Path("match_id") matchId: Int): ApiResponse<Match>
}

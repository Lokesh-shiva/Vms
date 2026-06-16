package com.example.vmsuser.network

import com.example.vmsuser.models.*
import retrofit2.http.*

interface ApiService {

    // Auth — OTP flow
    @POST("api/v1/auth/send-otp")
    suspend fun sendOtp(@Body body: Map<String, String>): ApiResponse<Unit>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body body: Map<String, String>): ApiResponse<OtpVerifyResponse>

    @POST("api/v1/auth/complete-profile")
    suspend fun completeProfile(@Body body: CompleteProfileRequest): ApiResponse<User>

    @GET("api/v1/auth/me")
    suspend fun getMe(): ApiResponse<User>

    @GET("api/v1/locations")
    suspend fun getLocations(): ApiResponse<List<LocationOption>>

    @GET("api/v1/sports")
    suspend fun getSports(): ApiResponse<List<SportItem>>

    @PUT("api/v1/users/me")
    suspend fun updateProfile(@Body body: Map<String, String>): ApiResponse<User>

    @PUT("api/v1/users/me/fcm-token")
    suspend fun updateFcmToken(@Body body: Map<String, String>): ApiResponse<Unit>

    // Matchmaking
    @GET("api/v1/matchmaking/status")
    suspend fun getQueueStatus(): ApiResponse<QueueStatus>

    @POST("api/v1/matchmaking/play-now")
    suspend fun joinQueue(@Body request: JoinQueueRequest): ApiResponse<QueueStatus>

    @DELETE("api/v1/matchmaking/leave")
    suspend fun leaveQueue(): ApiResponse<Unit>

    // Matches
    @GET("api/v1/matches/mine")
    suspend fun getMyMatches(): ApiResponse<List<Match>>

    @GET("api/v1/matches/mine/active")
    suspend fun getActiveMatch(): ApiResponse<Match?>

    @GET("api/v1/matches/{id}")
    suspend fun getMatch(@Path("id") id: Int): ApiResponse<Match>

    // Tournaments
    @GET("api/v1/tournaments")
    suspend fun getTournaments(): ApiResponse<List<Tournament>>

    @GET("api/v1/tournaments/{id}")
    suspend fun getTournament(@Path("id") id: Int): ApiResponse<Tournament>

    @POST("api/v1/tournaments/{id}/register")
    suspend fun registerTournament(@Path("id") id: Int, @Body body: Map<String, String> = emptyMap()): ApiResponse<Unit>

    // Societies
    @GET("api/v1/societies")
    suspend fun getSocieties(): ApiResponse<List<Society>>

    @GET("api/v1/societies/{id}")
    suspend fun getSociety(@Path("id") id: Int): ApiResponse<Society>

    @POST("api/v1/societies/{id}/join")
    suspend fun joinSociety(@Path("id") id: Int): ApiResponse<Unit>

    @DELETE("api/v1/societies/{id}/leave")
    suspend fun leaveSociety(@Path("id") id: Int): ApiResponse<Unit>

    @GET("api/v1/societies/{id}/members")
    suspend fun getSocietyMembers(@Path("id") id: Int): ApiResponse<List<SocietyMember>>

    @POST("api/v1/societies")
    suspend fun createSociety(@Body request: CreateSocietyRequest): ApiResponse<Society>

    // Wallet
    @GET("api/v1/wallet/transactions")
    suspend fun getWalletTransactions(): ApiResponse<List<WalletTransaction>>

    @GET("api/v1/wallet/balance")
    suspend fun getWalletBalance(): ApiResponse<Map<String, Int>>

    // Notifications
    @GET("api/v1/notifications")
    suspend fun getNotifications(): ApiResponse<List<Notification>>

    // Captain
    @POST("api/v1/captains/apply")
    suspend fun applyCaptain(@Body body: Map<String, String>): ApiResponse<Unit>

    @GET("api/v1/captains/me/stats")
    suspend fun getCaptainStats(): ApiResponse<CaptainStats>
}

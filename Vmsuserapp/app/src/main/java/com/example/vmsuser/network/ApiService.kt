package com.example.vmsuser.network

import com.example.vmsuser.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    // Auth — OTP flow
    @POST("api/v1/auth/send-otp")
    suspend fun sendOtp(@Body body: Map<String, String>): ApiResponse<Unit>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body body: Map<String, String>): ApiResponse<OtpVerifyResponse>

    @POST("api/v1/auth/complete-profile")
    suspend fun completeProfile(@Body body: CompleteProfileRequest): ApiResponse<User>

    @Multipart
    @POST("api/v1/auth/me/profile-photo")
    suspend fun uploadProfilePhoto(@Part file: MultipartBody.Part): ApiResponse<User>

    @GET("api/v1/auth/me")
    suspend fun getMe(): ApiResponse<User>

    @GET("api/v1/auth/check-username")
    suspend fun checkUsername(@Query("username") username: String): ApiResponse<UsernameAvailability>

    @GET("api/v1/locations")
    suspend fun getLocations(): ApiResponse<List<LocationOption>>

    @GET("api/v1/locations/nearest")
    suspend fun getNearestLocations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): ApiResponse<List<LocationOption>>

    @GET("api/v1/sports")
    suspend fun getSports(): ApiResponse<List<SportItem>>

    @PUT("api/v1/users/me")
    suspend fun updateProfile(@Body body: Map<String, String>): ApiResponse<User>

    @PUT("api/v1/users/me/fcm-token")
    suspend fun updateFcmToken(@Body body: Map<String, String>): ApiResponse<Unit>

    // Matchmaking
    @GET("api/v1/matchmaking/price")
    suspend fun getMatchmakingPrice(@Query("sport") sport: String): ApiResponse<MatchmakingPrice>

    @GET("api/v1/matchmaking/status")
    suspend fun getQueueStatus(): ApiResponse<QueueStatus>

    @POST("api/v1/matchmaking/play-now")
    suspend fun joinQueue(@Body request: PlayNowRequest): ApiResponse<QueueStatus>

    @DELETE("api/v1/matchmaking/leave")
    suspend fun leaveQueue(): ApiResponse<Unit>

    // Open matches (browse + join)
    @GET("api/v1/matches/open")
    suspend fun getOpenMatches(@Query("sport") sport: String? = null): ApiResponse<List<OpenMatch>>

    @POST("api/v1/matches/{id}/join")
    suspend fun joinOpenMatch(@Path("id") id: Int): ApiResponse<Match>

    // Matches
    @GET("api/v1/matches/mine")
    suspend fun getMyMatches(): ApiResponse<List<Match>>

    @GET("api/v1/matches/mine/active")
    suspend fun getActiveMatch(): ApiResponse<Match?>

    @GET("api/v1/matches/{id}")
    suspend fun getMatch(@Path("id") id: Int): ApiResponse<Match>

    // Chat
    @GET("api/v1/chat/threads")
    suspend fun getChatThreads(): ApiResponse<List<ChatThread>>

    @GET("api/v1/matches/{matchId}/messages")
    suspend fun getChatMessages(@Path("matchId") matchId: Int): ApiResponse<List<ChatMessageDto>>

    @POST("api/v1/matches/{matchId}/messages")
    suspend fun sendChatMessage(@Path("matchId") matchId: Int, @Body body: SendMessageRequest): ApiResponse<ChatMessageDto>

    // Tournaments
    @GET("api/v1/tournaments/public")
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

    @GET("api/v1/societies/{id}/leaderboard")
    suspend fun getSocietyLeaderboard(@Path("id") id: Int): ApiResponse<List<LeaderboardEntry>>

    @POST("api/v1/societies")
    suspend fun createSociety(@Body request: CreateSocietyRequest): ApiResponse<Society>

    // Wallet
    @GET("api/v1/wallet/transactions")
    suspend fun getWalletTransactions(): ApiResponse<List<WalletTransaction>>

    @GET("api/v1/wallet/balance")
    suspend fun getWalletBalance(): ApiResponse<Map<String, Int>>

    // Support / Disputes
    @GET("api/v1/disputes/mine")
    suspend fun getMyDisputes(): ApiResponse<List<Dispute>>

    @POST("api/v1/disputes/mine")
    suspend fun createDispute(@Body request: CreateDisputeRequest): ApiResponse<Dispute>

    @GET("api/v1/disputes/{id}/messages")
    suspend fun getDisputeMessages(@Path("id") id: Int): ApiResponse<List<DisputeMessage>>

    @POST("api/v1/disputes/{id}/messages")
    suspend fun sendDisputeMessage(@Path("id") id: Int, @Body request: SendDisputeMessageRequest): ApiResponse<DisputeMessage>

    // Notifications
    @GET("api/v1/notifications")
    suspend fun getNotifications(): ApiResponse<List<Notification>>

    @PUT("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): ApiResponse<Notification>

    // Captain
    @POST("api/v1/captains/apply")
    suspend fun applyCaptain(@Body body: Map<String, String?>): ApiResponse<CaptainApplication>

    @GET("api/v1/captains/me/stats")
    suspend fun getCaptainStats(): ApiResponse<CaptainStats>

    @Multipart
    @POST("api/v1/captains/me/kyc")
    suspend fun uploadCaptainKyc(
        @Part("document_type") documentType: RequestBody,
        @Part file: MultipartBody.Part,
    ): ApiResponse<CaptainApplication>

    @GET("api/v1/captains/me/application-status")
    suspend fun getCaptainApplicationStatus(): ApiResponse<CaptainApplication>

    @PUT("api/v1/captains/me/payout-upi")
    suspend fun updatePayoutUpi(@Body body: Map<String, String>): ApiResponse<CaptainApplication>
}

package com.example.vmsadmin.network

import com.example.vmsadmin.models.ApiResponse
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.Booking
import com.example.vmsadmin.models.Cart
import com.example.vmsadmin.models.CartType
import com.example.vmsadmin.models.CreateCartRequest
import com.example.vmsadmin.models.CreateUserRequest
import com.example.vmsadmin.models.CreateCartTypeRequest
import com.example.vmsadmin.models.DashboardStats
import com.example.vmsadmin.models.CreateFeeConfigRequest
import com.example.vmsadmin.models.CreateRegionRequest
import com.example.vmsadmin.models.CreateTimeslotRequest
import com.example.vmsadmin.models.FeeConfig
import com.example.vmsadmin.models.LoginRequest
import com.example.vmsadmin.models.LoginResponse
import com.example.vmsadmin.models.Payment
import com.example.vmsadmin.models.PaymentConfig
import com.example.vmsadmin.models.PaymentReportEntry
import com.example.vmsadmin.models.PaymentSummary
import com.example.vmsadmin.models.Region
import com.example.vmsadmin.models.Timeslot
import com.example.vmsadmin.models.UpdateCartRequest
import com.example.vmsadmin.models.UpdateCartTypeRequest
import com.example.vmsadmin.models.UpdateFeeConfigRequest
import com.example.vmsadmin.models.UpdateRegionRequest
import com.example.vmsadmin.models.UpdateTimeslotRequest
import com.example.vmsadmin.models.UpdateUserRequest
import com.example.vmsadmin.models.CreateItemRequest
import com.example.vmsadmin.models.Item
import com.example.vmsadmin.models.UpdateItemRequest
import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.models.UpdateGroundRequest
import com.example.vmsadmin.models.Match
import com.example.vmsadmin.models.MatchDetail
import com.example.vmsadmin.models.AdminChatMessage
import com.example.vmsadmin.models.AdminWallet
import com.example.vmsadmin.models.CreateVoteRoundRequest
import com.example.vmsadmin.models.VoteRoundState
import com.example.vmsadmin.models.CreateMatchRequest
import com.example.vmsadmin.models.SystemConfigListResponse
import com.example.vmsadmin.models.SystemConfigResponse
import com.example.vmsadmin.models.UpdateConfigRequest
import com.example.vmsadmin.models.QueueStatsResponse
import com.example.vmsadmin.models.Captain
import com.example.vmsadmin.models.CaptainPayoutRequest
import com.example.vmsadmin.models.CreateCaptainRequest
import com.example.vmsadmin.models.ReviewCaptainRequest
import com.example.vmsadmin.models.UpdateCaptainRequest
import com.example.vmsadmin.models.SessionStatus
import com.example.vmsadmin.models.AssignableRolesResponse
import com.example.vmsadmin.models.Tournament
import com.example.vmsadmin.models.CreateTournamentRequest
import com.example.vmsadmin.models.UpdateTournamentRequest
import com.example.vmsadmin.models.TournamentMatch
import com.example.vmsadmin.models.CreateTournamentMatchRequest
import com.example.vmsadmin.models.RecordMatchResultRequest
import com.example.vmsadmin.models.TournamentStanding
import com.example.vmsadmin.models.TournamentRegistration
import com.example.vmsadmin.models.Dispute
import com.example.vmsadmin.models.CreateDisputeRequest
import com.example.vmsadmin.models.UpdateDisputeRequest
import com.example.vmsadmin.models.DisputeMessage
import com.example.vmsadmin.models.SendDisputeMessageRequest
import com.example.vmsadmin.models.AuditLogEntry
import com.example.vmsadmin.models.Society
import com.example.vmsadmin.models.SocietyMember
import com.example.vmsadmin.models.SocietyLeaderboardEntry
import com.example.vmsadmin.models.CreateSocietyRequest
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    // ── Admin Dashboard ──────────────────────────────────────────────
    @GET("/api/v1/admin/dashboard")
    suspend fun getDashboardStats(): ApiResponse<DashboardStats>

    @GET("/api/v1/bookings")
    suspend fun getBookings(): ApiResponse<List<Booking>>

    @POST("/api/v1/bookings/{booking_id}/start")
    suspend fun startBooking(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/bookings/{booking_id}/complete")
    suspend fun completeBooking(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/bookings/{booking_id}/cancel")
    suspend fun cancelBooking(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/bookings/{booking_id}/start-session")
    suspend fun startSession(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/bookings/{booking_id}/end-session")
    suspend fun endSession(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

    // Scaffolding for future server-sync (clock-drift correction); client timer is sufficient for now
    @GET("/api/v1/bookings/{booking_id}/session-status")
    suspend fun getSessionStatus(@Path("booking_id") bookingId: Int): ApiResponse<SessionStatus>

    @GET("/api/v1/payments/config")
    suspend fun getPaymentsConfig(): ApiResponse<PaymentConfig>

    @GET("/api/v1/payments")
    suspend fun getPayments(): ApiResponse<List<Payment>>

    @GET("/api/v1/payments/summary")
    suspend fun getPaymentSummary(): ApiResponse<PaymentSummary>

    @GET("/api/v1/payments/report")
    suspend fun getPaymentReport(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
    ): ApiResponse<List<PaymentReportEntry>>

    @POST("/api/v1/payments/approve/{payment_id}")
    suspend fun approvePayment(@Path("payment_id") paymentId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/payments/reject/{payment_id}")
    suspend fun rejectPayment(@Path("payment_id") paymentId: Int): ApiResponse<JsonElement>

    @POST("/api/v1/payments/refund/{payment_id}")
    suspend fun refundPayment(@Path("payment_id") paymentId: Int): ApiResponse<JsonElement>

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

    // ── Cart Type endpoints ──────────────────────────────────────────
    @GET("/api/v1/cart-types")
    suspend fun getCartTypes(): ApiResponse<List<CartType>>

    @GET("/api/v1/cart-types/{cart_type_id}")
    suspend fun getCartType(@Path("cart_type_id") cartTypeId: Int): ApiResponse<CartType>

    @POST("/api/v1/cart-types")
    suspend fun createCartType(@Body request: CreateCartTypeRequest): ApiResponse<CartType>

    @PUT("/api/v1/cart-types/{cart_type_id}")
    suspend fun updateCartType(
        @Path("cart_type_id") cartTypeId: Int,
        @Body request: UpdateCartTypeRequest
    ): ApiResponse<CartType>

    @DELETE("/api/v1/cart-types/{cart_type_id}")
    suspend fun deleteCartType(@Path("cart_type_id") cartTypeId: Int): ApiResponse<JsonElement>

    @Multipart
    @POST("/api/v1/sports/{sport_id}/image")
    suspend fun uploadSportImage(
        @Path("sport_id") sportId: Int,
        @Part file: MultipartBody.Part,
    ): ApiResponse<CartType>

    // ── Timeslot endpoints ───────────────────────────────────────────
    @GET("/api/v1/timeslots")
    suspend fun getTimeslots(): ApiResponse<List<Timeslot>>

    @GET("/api/v1/timeslots/{timeslot_id}")
    suspend fun getTimeslot(@Path("timeslot_id") timeslotId: Int): ApiResponse<Timeslot>

    @POST("/api/v1/timeslots")
    suspend fun createTimeslot(@Body request: CreateTimeslotRequest): ApiResponse<Timeslot>

    @PUT("/api/v1/timeslots/{timeslot_id}")
    suspend fun updateTimeslot(
        @Path("timeslot_id") timeslotId: Int,
        @Body request: UpdateTimeslotRequest
    ): ApiResponse<Timeslot>

    @DELETE("/api/v1/timeslots/{timeslot_id}")
    suspend fun deleteTimeslot(@Path("timeslot_id") timeslotId: Int): ApiResponse<JsonElement>

    // ── Cart endpoints ────────────────────────────────────────────────
    @GET("/api/v1/carts")
    suspend fun getCarts(): ApiResponse<List<Cart>>

    @GET("/api/v1/carts/{id}")
    suspend fun getCartById(@Path("id") id: Int): ApiResponse<Cart>

    @POST("/api/v1/carts")
    suspend fun createCart(@Body request: CreateCartRequest): ApiResponse<Cart>

    @PUT("/api/v1/carts/{id}")
    suspend fun updateCart(
        @Path("id") id: Int,
        @Body request: UpdateCartRequest
    ): ApiResponse<Cart>

    @DELETE("/api/v1/carts/{id}")
    suspend fun deleteCart(@Path("id") id: Int): ApiResponse<JsonElement>

    // ── Fee Config endpoints ──────────────────────────────────────────
    @GET("/api/v1/fee-config/all")
    suspend fun getFeeConfigs(): ApiResponse<List<FeeConfig>>

    @GET("/api/v1/fee-config/region/{region_id}/cart-type/{cart_type_id}")
    suspend fun getFeeConfig(
        @Path("region_id") regionId: Int,
        @Path("cart_type_id") cartTypeId: Int
    ): ApiResponse<FeeConfig>

    @POST("/api/v1/fee-config/create")
    suspend fun createFeeConfig(@Body request: CreateFeeConfigRequest): ApiResponse<FeeConfig>

    @PUT("/api/v1/fee-config/{id}")
    suspend fun updateFeeConfig(
        @Path("id") id: Int,
        @Body request: UpdateFeeConfigRequest
    ): ApiResponse<FeeConfig>

    @DELETE("/api/v1/fee-config/{id}")
    suspend fun deleteFeeConfig(@Path("id") id: Int): ApiResponse<JsonElement>

    // ── Item endpoints ────────────────────────────────────────────────
    @GET("/api/v1/items")
    suspend fun getItems(): ApiResponse<List<Item>>

    @GET("/api/v1/items")
    suspend fun getItemsByCartType(
        @Query("cart_type_id") cartTypeId: Int
    ): ApiResponse<List<Item>>

    @POST("/api/v1/items")
    suspend fun createItem(@Body request: CreateItemRequest): ApiResponse<Item>

    @PUT("/api/v1/items/{item_id}")
    suspend fun updateItem(
        @Path("item_id") itemId: Int,
        @Body request: UpdateItemRequest
    ): ApiResponse<Item>

    @DELETE("/api/v1/items/{item_id}")
    suspend fun deleteItem(@Path("item_id") itemId: Int): ApiResponse<JsonElement>

    @Multipart
    @POST("/api/v1/items/{item_id}/image")
    suspend fun uploadItemImage(
        @Path("item_id") itemId: Int,
        @Part file: MultipartBody.Part,
    ): ApiResponse<Item>

    // ── Ground endpoints ─────────────────────────────────────────────
    @GET("/api/v1/grounds")
    suspend fun getGrounds(): ApiResponse<List<Ground>>

    @GET("/api/v1/grounds")
    suspend fun getGroundsByRegion(@Query("region_id") regionId: Int): ApiResponse<List<Ground>>

    @PUT("/api/v1/grounds/{id}")
    suspend fun updateGround(
        @Path("id") id: Int,
        @Body request: UpdateGroundRequest
    ): ApiResponse<Ground>

    @Multipart
    @POST("/api/v1/grounds/{id}/image")
    suspend fun uploadGroundImage(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part,
    ): ApiResponse<Ground>

    // ── Match endpoints ───────────────────────────────────────────────
    @GET("/api/v1/admin/matches")
    suspend fun getMatches(): ApiResponse<List<Match>>

    @POST("/api/v1/matches/{match_id}/cancel")
    suspend fun cancelMatch(@Path("match_id") matchId: Int): ApiResponse<Match>

    @POST("/api/v1/matches/{match_id}/complete")
    suspend fun completeMatch(@Path("match_id") matchId: Int): ApiResponse<Match>

    @POST("/api/v1/admin/matches/reap-abandoned")
    suspend fun reapAbandonedSessions(): ApiResponse<Map<String, Int>>

    @GET("/api/v1/admin/matches/{match_id}/detail")
    suspend fun getAdminMatchDetail(@Path("match_id") matchId: Int): ApiResponse<MatchDetail>

    @GET("/api/v1/admin/matches/{match_id}/messages")
    suspend fun getAdminMatchMessages(@Path("match_id") matchId: Int): ApiResponse<List<AdminChatMessage>>

    @GET("/api/v1/admin/wallet/{user_id}")
    suspend fun getAdminWallet(@Path("user_id") userId: Int): ApiResponse<AdminWallet>

    @GET("/api/v1/admin/vote-rounds/current")
    suspend fun getCurrentVoteRound(): ApiResponse<VoteRoundState>

    @POST("/api/v1/admin/vote-rounds")
    suspend fun createVoteRound(@Body body: CreateVoteRoundRequest): ApiResponse<VoteRoundState>

    @POST("/api/v1/admin/vote-rounds/{round_id}/close")
    suspend fun closeVoteRound(@Path("round_id") roundId: Int): ApiResponse<VoteRoundState>

    // ── User Management endpoints (super_admin only) ─────────────────
    @GET("/api/v1/users")
    suspend fun getUsers(): ApiResponse<List<AppUser>>

    @GET("users/assignable-roles")
    suspend fun getAssignableRoles(): ApiResponse<AssignableRolesResponse>

    @POST("/api/v1/users")
    suspend fun createUser(@Body request: CreateUserRequest): ApiResponse<AppUser>

    @PUT("/api/v1/users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body request: UpdateUserRequest
    ): ApiResponse<AppUser>

    // ── System Configuration endpoints ──────────────────────────────
    @GET("/api/v1/admin/config")
    suspend fun getSystemConfigs(): SystemConfigListResponse

    @PUT("/api/v1/admin/config/{key}")
    suspend fun updateSystemConfig(
        @Path("key") key: String,
        @Body request: UpdateConfigRequest
    ): SystemConfigResponse

    // ── Queue Overview endpoints ────────────────────────────────────
    @GET("/api/v1/admin/queue-stats")
    suspend fun getQueueStats(): QueueStatsResponse

    // ── User search (support) ──────────────────────────────────────
    @GET("/api/v1/users/search")
    suspend fun searchUserByPhone(@Query("phone") phone: String): ApiResponse<AppUser>

    // ── Captain endpoints ──────────────────────────────────────────
    @GET("/api/v1/captains")
    suspend fun getCaptains(@Query("status") status: String? = null): ApiResponse<List<Captain>>

    @POST("/api/v1/captains")
    suspend fun createCaptain(@Body request: CreateCaptainRequest): ApiResponse<Captain>

    @PUT("/api/v1/captains/{id}")
    suspend fun updateCaptain(
        @Path("id") id: Int,
        @Body request: UpdateCaptainRequest
    ): ApiResponse<Captain>

    @PUT("/api/v1/captains/{id}/review")
    suspend fun reviewCaptain(
        @Path("id") id: Int,
        @Body request: ReviewCaptainRequest
    ): ApiResponse<Captain>

    @Streaming
    @GET("/api/v1/captains/{id}/kyc-document")
    suspend fun getKycDocument(@Path("id") id: Int): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/v1/captains/payouts/pending")
    suspend fun getPendingPayouts(): ApiResponse<List<Captain>>

    @POST("/api/v1/captains/{id}/payout")
    suspend fun markCaptainPaid(
        @Path("id") id: Int,
        @Body request: CaptainPayoutRequest
    ): ApiResponse<Map<String, Int>>

    @DELETE("/api/v1/captains/{id}")
    suspend fun deleteCaptain(@Path("id") id: Int): ApiResponse<Unit>

    // ── Tournament endpoints ───────────────────────────────────────────
    @GET("/api/v1/tournaments")
    suspend fun getTournaments(): ApiResponse<List<Tournament>>

    @GET("/api/v1/tournaments/csr/mine")
    suspend fun getMySponsoredTournaments(): ApiResponse<List<Tournament>>

    @POST("/api/v1/tournaments")
    suspend fun createTournament(@Body request: CreateTournamentRequest): ApiResponse<Tournament>

    @PUT("/api/v1/tournaments/{id}")
    suspend fun updateTournament(
        @Path("id") id: Int,
        @Body request: UpdateTournamentRequest
    ): ApiResponse<Tournament>

    @DELETE("/api/v1/tournaments/{id}")
    suspend fun deleteTournament(@Path("id") id: Int): ApiResponse<JsonElement>

    @GET("/api/v1/tournaments/{id}/matches")
    suspend fun getTournamentMatches(@Path("id") id: Int): ApiResponse<List<TournamentMatch>>

    @POST("/api/v1/tournaments/{id}/matches")
    suspend fun createTournamentMatch(
        @Path("id") id: Int,
        @Body request: CreateTournamentMatchRequest
    ): ApiResponse<TournamentMatch>

    @PUT("/api/v1/tournaments/{id}/matches/{matchId}/result")
    suspend fun recordMatchResult(
        @Path("id") id: Int,
        @Path("matchId") matchId: Int,
        @Body request: RecordMatchResultRequest
    ): ApiResponse<TournamentMatch>

    @GET("/api/v1/tournaments/{id}/standings")
    suspend fun getTournamentStandings(@Path("id") id: Int): ApiResponse<List<TournamentStanding>>

    @GET("/api/v1/tournaments/{id}/registrations")
    suspend fun getTournamentRegistrations(@Path("id") id: Int): ApiResponse<List<TournamentRegistration>>

    // ── Dispute endpoints ──────────────────────────────────────────────
    @GET("/api/v1/disputes")
    suspend fun getDisputes(): ApiResponse<List<Dispute>>

    @POST("/api/v1/disputes")
    suspend fun createDispute(@Body request: CreateDisputeRequest): ApiResponse<Dispute>

    @PUT("/api/v1/disputes/{id}")
    suspend fun updateDispute(
        @Path("id") id: Int,
        @Body request: UpdateDisputeRequest
    ): ApiResponse<Dispute>

    @GET("/api/v1/disputes/{id}/messages")
    suspend fun getDisputeMessages(@Path("id") id: Int): ApiResponse<List<DisputeMessage>>

    @POST("/api/v1/disputes/{id}/messages")
    suspend fun sendDisputeMessage(
        @Path("id") id: Int,
        @Body request: SendDisputeMessageRequest
    ): ApiResponse<DisputeMessage>

    // ── Audit Log endpoints ────────────────────────────────────────────
    @GET("/api/v1/audit-logs")
    suspend fun getAuditLogs(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("action") action: String? = null,
        @Query("actor_user_id") actorUserId: Int? = null,
        @Query("target_resource_type") targetResourceType: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
    ): ApiResponse<List<AuditLogEntry>>

    // ── Society endpoints ──────────────────────────────────────────────
    @GET("/api/v1/societies")
    suspend fun getSocieties(): ApiResponse<List<Society>>

    @POST("/api/v1/societies")
    suspend fun createSociety(@Body request: CreateSocietyRequest): ApiResponse<Society>

    @DELETE("/api/v1/societies/{id}")
    suspend fun deleteSociety(@Path("id") id: Int): ApiResponse<JsonElement>

    @POST("/api/v1/societies/{id}/deactivate")
    suspend fun deactivateSociety(@Path("id") id: Int): ApiResponse<Society>

    @GET("/api/v1/societies/{id}/members")
    suspend fun getSocietyMembers(@Path("id") id: Int): ApiResponse<List<SocietyMember>>

    @GET("/api/v1/societies/{id}/leaderboard")
    suspend fun getSocietyLeaderboard(@Path("id") id: Int): ApiResponse<List<SocietyLeaderboardEntry>>
}

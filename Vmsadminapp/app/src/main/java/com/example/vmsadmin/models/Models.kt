package com.example.vmsadmin.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class LoginRequest(
    val phone: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val role: String? = null,
    val user_id: Int? = null
)

@Serializable
data class AppUser(
    val id: Int,
    val name: String,
    val phone: String,
    val role: String,
    val is_active: Boolean,
    val region_id: Int? = null,
    val ghost_strikes: Int = 0,
    val can_create_society: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UpdateUserRequest(
    val role: String? = null,
    val is_active: Boolean? = null,
    val can_create_society: Boolean? = null,
)

@Serializable
data class AssignableRolesResponse(
    val assignable_roles: List<String>
)

@Serializable
data class Booking(
    val id: Int,
    val region_id: Int? = null,
    val cart_type_id: Int? = null,
    val timeslot_id: Int? = null,
    val status: String,
    val assigned_cart_id: Int? = null,
    val address: String? = null,
    val date: String? = null,
    val created_at: String? = null,
    // Display-friendly names
    val region_name: String? = null,
    val cart_type_name: String? = null,
    val timeslot_label: String? = null,
    val cart_label: String? = null,
    // Session / time-billing fields (null when session not started)
    val session_started_at: String? = null,
    val session_ended_at: String? = null,
    val session_minutes: Int? = null,
    val session_blocks: Int? = null,
    val time_bill_amount: Double? = null,
    val surge_multiplier_snapshot: Double? = null
)

@Serializable
data class PaymentSummary(
    val total_revenue: Double,
    val total_refunded: Double,
    val pending_count: Int,
    val refunded_count: Int
)

@Serializable
data class Payment(
    val id: Int,
    val booking_id: Int? = null,
    val provider: String? = null,
    val amount: Double? = null,
    val reference_code: String? = null,
    val transaction_id: String? = null,
    val status: String? = null,
    val payment_type: String? = null,   // "MATCHING_FEE" or "TIME_BILL"
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class PaymentConfig(
    val upi_id: String? = null,
    val merchant_name: String? = null
)

@Serializable
data class Region(
    val id: Int,
    val name: String,
    val is_serviceable: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateRegionRequest(
    val name: String
)

@Serializable
data class UpdateRegionRequest(
    val name: String? = null,
    val is_serviceable: Boolean? = null
)

@Serializable
data class CartType(
    val id: Int,
    val name: String,
    val description: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateCartTypeRequest(
    val name: String
)

@Serializable
data class UpdateCartTypeRequest(
    val name: String? = null,
    val is_active: Boolean? = null
)

@Serializable
data class Timeslot(
    val id: Int,
    val start_time: String,
    val end_time: String,
    val capacity: Int,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateTimeslotRequest(
    val start_time: String,
    val end_time: String,
    val capacity: Int
)

@Serializable
data class UpdateTimeslotRequest(
    val start_time: String? = null,
    val end_time: String? = null,
    val capacity: Int? = null,
    val is_active: Boolean? = null
)

@Serializable
data class Cart(
    val id: Int,
    val label: String? = null,
    val region_id: Int,
    val region_name: String? = null,
    val cart_type_id: Int,
    val cart_type_name: String? = null,
    val status: String,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateCartRequest(
    val label: String? = null,
    val region_id: Int,
    val cart_type_id: Int,
    val is_active: Boolean = true
)

@Serializable
data class UpdateCartRequest(
    val label: String? = null,
    val region_id: Int? = null,
    val cart_type_id: Int? = null,
    val is_active: Boolean? = null
)

@Serializable
data class Ground(
    val id: Int,
    val name: String,
    val sport_id: Int,
    val location_id: Int,
    val status: String,
    val is_active: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val owner_user_id: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UpdateGroundRequest(
    val is_active: Boolean? = null,
    val owner_user_id: Int? = null
)

@Serializable
data class FeeConfig(
    val id: Int,
    val region_id: Int,
    val cart_type_id: Int,
    val booking_fee: Double,
    val cancellation_fee_pct: Double,
    val platform_fee_pct: Double,
    val is_active: Boolean = true,
    val region_name: String? = null,
    val cart_type_name: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    // Time-based billing fields
    val matching_fee: Double = 0.0,
    val rate_per_block: Double = 0.0,
    val block_duration_minutes: Int = 45,
    val max_duration_minutes: Int = 180,
    val surge_enabled: Boolean = false,
    val surge_multiplier: Double = 1.0
)

@Serializable
data class CreateFeeConfigRequest(
    val region_id: Int,
    val cart_type_id: Int,
    val booking_fee: Double,
    val cancellation_fee_pct: Double,
    val platform_fee_pct: Double,
    val matching_fee: Double = 0.0,
    val rate_per_block: Double = 0.0,
    val block_duration_minutes: Int = 45,
    val max_duration_minutes: Int = 180
)

@Serializable
data class UpdateFeeConfigRequest(
    val booking_fee: Double? = null,
    val cancellation_fee_pct: Double? = null,
    val platform_fee_pct: Double? = null,
    val is_active: Boolean? = null,
    val matching_fee: Double? = null,
    val rate_per_block: Double? = null,
    val block_duration_minutes: Int? = null,
    val max_duration_minutes: Int? = null,
    val surge_enabled: Boolean? = null,
    val surge_multiplier: Double? = null
)

@Serializable
data class DashboardStats(
    val active_services: Int = 0,
    val payments_under_review: Int = 0,
    val completed_today: Int = 0,
    val total_bookings: Int = 0
)

@Serializable
data class Item(
    val id: Int,
    val name: String,
    val price: Double,
    val cart_type_id: Int,
    val cart_type_name: String? = null,
    val description: String? = null,
    val image_url: String? = null,
    val is_available: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val price: Double,
    val cart_type_id: Int,
    val description: String? = null,
    val image_url: String? = null
)

@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val price: Double? = null,
    val is_available: Boolean? = null,
    val cart_type_id: Int? = null,
    val description: String? = null,
    val image_url: String? = null
)

@Serializable
data class Match(
    val id: Int,
    val created_by: Int? = null,
    val region_id: Int,
    val cart_type_id: Int,
    val cart_id: Int? = null,
    val timeslot_id: Int? = null,
    val skill_level: String? = null,
    val max_players: Int,
    val joined_players: Int,
    val status: String,
    val sport_name: String? = null,
    val ground_label: String? = null,
    val timeslot_label: String? = null,
    val region_name: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateMatchRequest(
    val cart_type_id: Int,
    val timeslot_id: Int,
    val region_id: Int,
    val max_players: Int,
    val skill_level: String? = null
)

// --- System Configuration Models ---

@Serializable
data class SystemConfig(
    val id: Int,
    val key: String,
    val value: String,
    val updated_at: String? = null
)

@Serializable
data class UpdateConfigRequest(
    val value: String
)

@Serializable
data class SystemConfigResponse(
    val success: Boolean,
    val data: SystemConfig? = null,
    val message: String? = null
)

@Serializable
data class SystemConfigListResponse(
    val success: Boolean,
    val data: List<SystemConfig>? = null,
    val message: String? = null
)

// --- Queue Stats Models ---

@Serializable
data class QueueStat(
    val sport_id: Int,
    val region_id: Int,
    val count: Int
)

@Serializable
data class QueueStatsResponse(
    val success: Boolean,
    val data: List<QueueStat>? = null,
    val message: String? = null
)

// --- Captain Models ---

@Serializable
data class Captain(
    val id: Int,
    val user_id: Int,
    val region_id: Int? = null,
    val status: String,
    val rating: Float = 0f,
    val total_trips: Int = 0,
    val bio: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val name: String? = null,
    val phone: String? = null
)

@Serializable
data class CreateCaptainRequest(
    val user_id: Int,
    val region_id: Int? = null,
    val bio: String? = null
)

@Serializable
data class UpdateCaptainRequest(
    val status: String? = null,
    val region_id: Int? = null,
    val bio: String? = null
)

// --- Tournament Models ---

@Serializable
data class Tournament(
    val id: Int,
    val name: String,
    val sport_id: Int? = null,
    val region_id: Int? = null,
    val organizer: String,
    val start_date: String,
    val end_date: String,
    val max_teams: Int = 8,
    val status: String = "UPCOMING",
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateTournamentRequest(
    val name: String,
    val organizer: String,
    val start_date: String,
    val end_date: String,
    val max_teams: Int = 8,
    val sport_id: Int? = null,
    val region_id: Int? = null
)

@Serializable
data class UpdateTournamentRequest(
    val status: String? = null,
    val name: String? = null,
    val organizer: String? = null
)

@Serializable
data class SessionStatus(
    val booking_id: Int,
    val status: String,
    val running: Boolean,
    val elapsed_minutes: Int,
    val current_blocks: Int,
    val estimated_time_bill: Double
)

// --- Dispute Models ---

@Serializable
data class Dispute(
    val id: Int,
    val booking_id: Int? = null,
    val user_id: Int? = null,
    val raised_by: Int? = null,
    val title: String,
    val description: String,
    val status: String = "OPEN",
    val resolution_note: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateDisputeRequest(
    val title: String,
    val description: String,
    val booking_id: Int? = null,
    val user_id: Int? = null
)

@Serializable
data class UpdateDisputeRequest(
    val status: String? = null,
    val resolution_note: String? = null
)

// --- Audit Log Models ---

@Serializable
data class AuditLogEntry(
    val id: Int,
    val action: String,
    val actor_user_id: Int? = null,
    val target_resource_type: String? = null,
    val target_resource_id: Int? = null,
    val details: String? = null,
    val created_at: String? = null
)

// --- Society Models ---

@Serializable
data class Society(
    val id: Int,
    val name: String,
    val description: String? = null,
    val owner_user_id: Int,
    val region_id: Int,
    val sport_id: Int,
    val is_public: Boolean = true,
    val max_members: Int = 50,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SocietyMember(
    val id: Int,
    val society_id: Int,
    val user_id: Int,
    val role: String,
    val joined_at: String? = null
)

@Serializable
data class SocietyLeaderboardEntry(
    val user_id: Int,
    val society_member_role: String,
    val total_points: Int = 0,
    val matches_played: Int = 0
)

@Serializable
data class CreateSocietyRequest(
    val name: String,
    val description: String? = null,
    val region_id: Int,
    val sport_id: Int,
    val is_public: Boolean = true,
    val max_members: Int = 50,
    val owner_user_id: Int? = null
)

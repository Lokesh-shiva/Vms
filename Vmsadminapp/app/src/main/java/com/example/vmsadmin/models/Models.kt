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
    val token_type: String
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
    // Display-friendly names (will be populated when backend supports them)
    val region_name: String? = null,
    val cart_type_name: String? = null,
    val timeslot_label: String? = null,
    val cart_label: String? = null
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
    val updated_at: String? = null
)

@Serializable
data class CreateFeeConfigRequest(
    val region_id: Int,
    val cart_type_id: Int,
    val booking_fee: Double,
    val cancellation_fee_pct: Double,
    val platform_fee_pct: Double
)

@Serializable
data class UpdateFeeConfigRequest(
    val booking_fee: Double? = null,
    val cancellation_fee_pct: Double? = null,
    val platform_fee_pct: Double? = null,
    val is_active: Boolean? = null
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
    val is_available: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val price: Double,
    val cart_type_id: Int
)

@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val price: Double? = null,
    val is_available: Boolean? = null,
    val cart_type_id: Int? = null
)

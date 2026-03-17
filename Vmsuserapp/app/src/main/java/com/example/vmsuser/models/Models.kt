package com.example.vmsuser.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

// ── Auth ──────────────────────────────────────────────────────────────

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
data class RegisterRequest(
    val name: String,
    val phone: String,
    val password: String
)

@Serializable
data class User(
    val id: Int,
    val name: String,
    val phone: String,
    val role: String = "user",
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

// ── Config ────────────────────────────────────────────────────────────

@Serializable
data class Region(
    val id: Int,
    val name: String,
    val is_serviceable: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
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
data class Timeslot(
    val id: Int,
    val start_time: String,
    val end_time: String,
    val capacity: Int,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

// ── Booking ───────────────────────────────────────────────────────────

@Serializable
data class Booking(
    val id: Int,
    val user_id: Int? = null,
    val region_id: Int? = null,
    val cart_type_id: Int? = null,
    val timeslot_id: Int? = null,
    val assigned_cart_id: Int? = null,
    val address: String? = null,
    val date: String? = null,
    val status: String,
    val payment_status: String? = null,
    val booking_fee: Double? = null,
    val estimated_total: Double? = null,
    val refund_status: String? = null,
    val refund_amount: Double? = null,
    val cancellation_fee_pct_snapshot: Double? = null,
    val platform_fee_pct_snapshot: Double? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    // Display-friendly names populated by backend
    val region_name: String? = null,
    val cart_type_name: String? = null,
    val timeslot_label: String? = null,
    val cart_label: String? = null
)

@Serializable
data class CreateBookingRequest(
    val region_id: Int,
    val cart_type_id: Int,
    val timeslot_id: Int,
    val address: String
)

// ── Payment ───────────────────────────────────────────────────────────

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
data class InitiatePaymentResponse(
    val booking_id: Int,
    val amount: Double,
    val reference_code: String,
    val upi_id: String,
    val upi_link: String,
    val payment: Payment? = null
)

@Serializable
data class ConfirmPaymentRequest(
    val transaction_id: String
)

// ── Address (local model only — not sent to backend directly) ─────────

data class Address(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val pincode: String = ""
)

// ── Item ──────────────────────────────────────────────────────────────

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

// ── Shared UI State ───────────────────────────────────────────────────

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

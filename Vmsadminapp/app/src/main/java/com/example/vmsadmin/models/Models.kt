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

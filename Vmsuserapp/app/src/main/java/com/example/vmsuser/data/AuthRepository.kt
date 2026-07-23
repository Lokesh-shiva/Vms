package com.example.vmsuser.data

import com.example.vmsuser.models.CompleteProfileRequest
import com.example.vmsuser.models.OtpVerifyResponse
import com.example.vmsuser.models.User
import com.example.vmsuser.network.RetrofitClient

class AuthRepository {
    private val api = RetrofitClient.api

    suspend fun sendOtp(phone: String): Result<Unit> = runCatching {
        val resp = api.sendOtp(mapOf("phone" to phone))
        if (!resp.success) error(resp.message ?: "Failed to send OTP.")
    }

    suspend fun verifyOtp(phone: String, otp: String): Result<OtpVerifyResponse> = runCatching {
        val resp = api.verifyOtp(mapOf("phone" to phone, "otp" to otp))
        resp.data ?: error(resp.message ?: "OTP verification failed.")
    }

    suspend fun completeProfile(
        name: String,
        username: String,
        email: String?,
        dateOfBirth: String?,
        city: String,
        sportPreferences: List<String>,
        profilePhotoUrl: String?,
    ): Result<User> = runCatching {
        val resp = api.completeProfile(
            CompleteProfileRequest(
                name = name,
                username = username,
                email = email,
                dateOfBirth = dateOfBirth,
                city = city,
                sportPreferences = sportPreferences,
                profilePhotoUrl = profilePhotoUrl,
            )
        )
        resp.data ?: error(resp.message ?: "Failed to save profile.")
    }

    suspend fun getMe(): Result<User> = runCatching {
        val resp = api.getMe()
        resp.data ?: error(resp.message ?: "Failed to load profile.")
    }
}

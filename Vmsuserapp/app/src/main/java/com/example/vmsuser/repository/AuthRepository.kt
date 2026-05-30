package com.example.vmsuser.repository

import com.example.vmsuser.data.TokenManager
import com.example.vmsuser.models.LoginRequest
import com.example.vmsuser.models.RegisterRequest
import com.example.vmsuser.models.User
import com.example.vmsuser.network.ApiService
import retrofit2.HttpException

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : BaseRepository() {

    suspend fun login(phone: String, password: String): String {
        try {
            val response = apiService.login(LoginRequest(phone = phone, password = password))
            if (response.success && response.data != null) {
                tokenManager.saveToken(response.data.access_token)
                return response.data.access_token
            }
            throw Exception(response.message ?: "Login failed")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Login failed")
        }
    }

    suspend fun register(name: String, phone: String, password: String): User {
        try {
            val response = apiService.register(RegisterRequest(name = name, phone = phone, password = password))
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Registration failed")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Registration failed")
        }
    }

    suspend fun getCurrentUser(): User {
        val response = apiService.getMe()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch user profile")
    }

    suspend fun logout() {
        tokenManager.clearToken()
    }
}

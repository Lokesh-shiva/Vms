package com.example.vmsadmin.data

import com.example.vmsadmin.models.AdminWallet
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.CreateUserRequest
import com.example.vmsadmin.models.UpdateUserRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class UserManagementRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUsers(): List<AppUser> {
        val response = apiService.getUsers()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch users")
    }

    suspend fun createUser(request: CreateUserRequest): AppUser {
        try {
            val response = apiService.createUser(request)
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to create user")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create user")
        }
    }

    suspend fun getAssignableRoles(): List<String> {
        return try {
            val response = apiService.getAssignableRoles()
            if (response.success && response.data != null) {
                response.data.assignable_roles
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()  // Non-fatal — ViewModel will show empty state
        }
    }

    suspend fun updateRole(id: Int, role: String): AppUser {
        try {
            val response = apiService.updateUser(id, UpdateUserRequest(role = role))
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to update role")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update role")
        }
    }

    suspend fun setActive(id: Int, active: Boolean): AppUser {
        try {
            val response = apiService.updateUser(id, UpdateUserRequest(is_active = active))
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to update status")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update status")
        }
    }

    suspend fun getWallet(userId: Int): AdminWallet {
        try {
            val response = apiService.getAdminWallet(userId)
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to fetch wallet")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to fetch wallet")
        }
    }

    suspend fun searchByPhone(phone: String): AppUser? {
        return try {
            val response = apiService.searchUserByPhone(phone)
            if (response.success) response.data else null
        } catch (e: HttpException) {
            if (e.code() == 404) null
            else throw Exception(parseErrorDetail(e) ?: "Search failed")
        }
    }

    suspend fun setSocietyPermission(id: Int, allowed: Boolean): AppUser {
        try {
            val response = apiService.updateUser(id, UpdateUserRequest(can_create_society = allowed))
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to update society permission")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update society permission")
        }
    }

    private fun parseErrorDetail(e: HttpException): String? {
        return try {
            val errorBody = e.response()?.errorBody()?.string() ?: return null
            val jsonObj = json.parseToJsonElement(errorBody).jsonObject
            jsonObj["detail"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }
}

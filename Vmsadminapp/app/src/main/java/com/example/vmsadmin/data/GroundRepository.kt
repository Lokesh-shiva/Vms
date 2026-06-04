package com.example.vmsadmin.data

import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.models.UpdateGroundRequest
import com.example.vmsadmin.network.ApiService
import retrofit2.HttpException

class GroundRepository(private val apiService: ApiService) {

    suspend fun getGrounds(): List<Ground> {
        val response = apiService.getGrounds()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch grounds")
    }

    suspend fun toggleGround(id: Int, isActive: Boolean): Ground {
        val response = apiService.updateGround(id, UpdateGroundRequest(is_active = isActive))
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to update ground")
    }

    suspend fun assignOwner(id: Int, ownerUserId: Int): Ground {
        val response = apiService.updateGround(id, UpdateGroundRequest(owner_user_id = ownerUserId))
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to assign owner")
    }

    suspend fun searchUserByPhone(phone: String): AppUser? {
        return try {
            val response = apiService.searchUserByPhone(phone)
            if (response.success) response.data else null
        } catch (e: HttpException) {
            if (e.code() == 404) null
            else throw Exception("Search failed")
        }
    }
}

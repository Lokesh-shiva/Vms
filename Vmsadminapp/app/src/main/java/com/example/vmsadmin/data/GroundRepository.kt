package com.example.vmsadmin.data

import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.models.UpdateGroundRequest
import com.example.vmsadmin.network.ApiService

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
}

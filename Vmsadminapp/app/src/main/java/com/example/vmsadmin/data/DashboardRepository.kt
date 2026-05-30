package com.example.vmsadmin.data

import com.example.vmsadmin.models.DashboardStats
import com.example.vmsadmin.network.ApiService

class DashboardRepository(private val apiService: ApiService) {

    suspend fun getDashboardStats(): DashboardStats {
        val response = apiService.getDashboardStats()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(parseErrorDetail(response.message))
    }

    private fun parseErrorDetail(message: String?): String {
        return message ?: "Failed to fetch dashboard stats"
    }
}

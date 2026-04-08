package com.example.vmsadmin.data

import com.example.vmsadmin.models.SystemConfig
import com.example.vmsadmin.models.UpdateConfigRequest
import com.example.vmsadmin.network.ApiService

class SystemConfigRepository(private val apiService: ApiService) {

    suspend fun fetchConfigs(): List<SystemConfig> {
        val response = apiService.getSystemConfigs()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch configurations")
    }

    suspend fun updateConfig(key: String, value: String): SystemConfig {
        val request = UpdateConfigRequest(value)
        val response = apiService.updateSystemConfig(key, request)
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to update configuration")
    }
}

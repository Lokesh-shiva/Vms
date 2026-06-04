package com.example.vmsadmin.data

import com.example.vmsadmin.models.AuditLogEntry
import com.example.vmsadmin.network.ApiService

class AuditLogRepository(private val apiService: ApiService) {

    suspend fun getAuditLogs(): List<AuditLogEntry> {
        val response = apiService.getAuditLogs()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch audit logs")
    }
}

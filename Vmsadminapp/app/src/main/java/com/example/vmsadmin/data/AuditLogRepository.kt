package com.example.vmsadmin.data

import com.example.vmsadmin.models.AuditLogEntry
import com.example.vmsadmin.network.ApiService

class AuditLogRepository(private val apiService: ApiService) {

    suspend fun getAuditLogs(
        limit: Int = 50,
        offset: Int = 0,
        action: String? = null,
        actorUserId: Int? = null,
        targetResourceType: String? = null,
        startDate: String? = null,
        endDate: String? = null,
    ): List<AuditLogEntry> {
        val response = apiService.getAuditLogs(
            limit = limit,
            offset = offset,
            action = action,
            actorUserId = actorUserId,
            targetResourceType = targetResourceType,
            startDate = startDate,
            endDate = endDate,
        )
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch audit logs")
    }
}

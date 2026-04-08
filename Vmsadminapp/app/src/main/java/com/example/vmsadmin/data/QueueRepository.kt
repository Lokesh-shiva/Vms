package com.example.vmsadmin.data

import com.example.vmsadmin.models.QueueStat
import com.example.vmsadmin.network.ApiService

class QueueRepository(private val apiService: ApiService) {

    suspend fun fetchQueueStats(): List<QueueStat> {
        val response = apiService.getQueueStats()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch queue stats")
    }
}

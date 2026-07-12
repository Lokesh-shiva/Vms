package com.example.vmsadmin.data

import com.example.vmsadmin.models.CreateDisputeRequest
import com.example.vmsadmin.models.Dispute
import com.example.vmsadmin.models.DisputeMessage
import com.example.vmsadmin.models.SendDisputeMessageRequest
import com.example.vmsadmin.models.UpdateDisputeRequest
import com.example.vmsadmin.network.ApiService

class DisputeRepository(private val apiService: ApiService) {

    suspend fun getDisputes(): List<Dispute> {
        val response = apiService.getDisputes()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch disputes")
    }

    suspend fun createDispute(request: CreateDisputeRequest): Dispute {
        val response = apiService.createDispute(request)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to create dispute")
    }

    suspend fun updateDispute(id: Int, request: UpdateDisputeRequest): Dispute {
        val response = apiService.updateDispute(id, request)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to update dispute")
    }

    suspend fun getMessages(disputeId: Int): List<DisputeMessage> {
        val response = apiService.getDisputeMessages(disputeId)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch messages")
    }

    suspend fun sendMessage(disputeId: Int, body: String): DisputeMessage {
        val response = apiService.sendDisputeMessage(disputeId, SendDisputeMessageRequest(body))
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to send message")
    }
}

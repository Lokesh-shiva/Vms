package com.example.vmsadmin.data

import com.example.vmsadmin.models.CreateVoteRoundRequest
import com.example.vmsadmin.models.VoteRoundState
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class VoteRoundRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getCurrentRound(): VoteRoundState {
        try {
            val response = apiService.getCurrentVoteRound()
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to fetch vote round")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to fetch vote round")
        }
    }

    suspend fun createRound(options: List<String>, closesAt: String): VoteRoundState {
        try {
            val response = apiService.createVoteRound(CreateVoteRoundRequest(options, closesAt))
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to start vote round")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to start vote round")
        }
    }

    suspend fun closeRound(roundId: Int): VoteRoundState {
        try {
            val response = apiService.closeVoteRound(roundId)
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to close vote round")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to close vote round")
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

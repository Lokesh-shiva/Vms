package com.example.vmsadmin.data

import com.example.vmsadmin.models.AdminChatMessage
import com.example.vmsadmin.models.Match
import com.example.vmsadmin.models.MatchDetail
import com.example.vmsadmin.network.ApiService
import retrofit2.HttpException

class MatchRepository(private val apiService: ApiService) {

    suspend fun getMatches(): List<Match> {
        return try {
            val response = apiService.getMatches()
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to fetch matches")
        } catch (e: HttpException) {
            throw Exception("Failed to fetch matches: ${e.message()}")
        }
    }

    suspend fun cancelMatch(matchId: Int): Match {
        return try {
            val response = apiService.cancelMatch(matchId)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to cancel match")
        } catch (e: HttpException) {
            throw Exception("Failed to cancel match: ${e.message()}")
        }
    }

    suspend fun completeMatch(matchId: Int): Match {
        return try {
            val response = apiService.completeMatch(matchId)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to complete match")
        } catch (e: HttpException) {
            throw Exception("Failed to complete match: ${e.message()}")
        }
    }

    suspend fun reapAbandonedSessions(): Int {
        return try {
            val response = apiService.reapAbandonedSessions()
            if (response.success && response.data != null) response.data["reaped_count"] ?: 0
            else throw Exception(response.message ?: "Failed to reap abandoned sessions")
        } catch (e: HttpException) {
            throw Exception("Failed to reap abandoned sessions: ${e.message()}")
        }
    }

    suspend fun getMatchDetail(matchId: Int): MatchDetail {
        return try {
            val response = apiService.getAdminMatchDetail(matchId)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to fetch match detail")
        } catch (e: HttpException) {
            throw Exception("Failed to fetch match detail: ${e.message()}")
        }
    }

    suspend fun getMatchMessages(matchId: Int): List<AdminChatMessage> {
        return try {
            val response = apiService.getAdminMatchMessages(matchId)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to fetch match chat")
        } catch (e: HttpException) {
            throw Exception("Failed to fetch match chat: ${e.message()}")
        }
    }
}

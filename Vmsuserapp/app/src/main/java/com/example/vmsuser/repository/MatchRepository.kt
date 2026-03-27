package com.example.vmsuser.repository

import com.example.vmsuser.models.CreateMatchRequest
import com.example.vmsuser.models.Match
import com.example.vmsuser.network.ApiService
import retrofit2.HttpException

class MatchRepository(private val apiService: ApiService) {

    suspend fun getMatches(sportId: Int? = null, regionId: Int? = null): List<Match> {
        return try {
            val response = apiService.getMatches(sportId, regionId)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to fetch matches")
        } catch (e: HttpException) {
            throw Exception("Failed to fetch matches: ${e.message()}")
        }
    }

    suspend fun createMatch(request: CreateMatchRequest): Match {
        return try {
            val response = apiService.createMatch(request)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to create match")
        } catch (e: HttpException) {
            throw Exception("Failed to create match: ${e.message()}")
        }
    }

    suspend fun joinMatch(matchId: Int): Match {
        return try {
            val response = apiService.joinMatch(matchId)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to join match")
        } catch (e: HttpException) {
            throw Exception("Failed to join match: ${e.message()}")
        }
    }

    suspend fun leaveMatch(matchId: Int): Match {
        return try {
            val response = apiService.leaveMatch(matchId)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message ?: "Failed to leave match")
        } catch (e: HttpException) {
            throw Exception("Failed to leave match: ${e.message()}")
        }
    }
}

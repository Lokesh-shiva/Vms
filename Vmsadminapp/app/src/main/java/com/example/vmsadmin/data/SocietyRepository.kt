package com.example.vmsadmin.data

import com.example.vmsadmin.models.Society
import com.example.vmsadmin.models.SocietyLeaderboardEntry
import com.example.vmsadmin.models.SocietyMember
import com.example.vmsadmin.network.ApiService

class SocietyRepository(private val apiService: ApiService) {

    suspend fun getSocieties(): List<Society> {
        val response = apiService.getSocieties()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch societies")
    }

    suspend fun deactivateSociety(id: Int): Society {
        val response = apiService.deactivateSociety(id)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to deactivate society")
    }

    suspend fun deleteSociety(id: Int) {
        val response = apiService.deleteSociety(id)
        if (!response.success) throw Exception(response.message ?: "Failed to delete society")
    }

    suspend fun getSocietyMembers(id: Int): List<SocietyMember> {
        val response = apiService.getSocietyMembers(id)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch members")
    }

    suspend fun getSocietyLeaderboard(id: Int): List<SocietyLeaderboardEntry> {
        val response = apiService.getSocietyLeaderboard(id)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch leaderboard")
    }
}

package com.example.vmsadmin.data

import com.example.vmsadmin.models.CreateTournamentRequest
import com.example.vmsadmin.models.Tournament
import com.example.vmsadmin.models.UpdateTournamentRequest
import com.example.vmsadmin.network.ApiService

class TournamentRepository(private val apiService: ApiService) {

    suspend fun getTournaments(): List<Tournament> {
        val response = apiService.getTournaments()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch tournaments")
    }

    suspend fun createTournament(request: CreateTournamentRequest): Tournament {
        val response = apiService.createTournament(request)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to create tournament")
    }

    suspend fun updateTournament(id: Int, request: UpdateTournamentRequest): Tournament {
        val response = apiService.updateTournament(id, request)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to update tournament")
    }

    suspend fun deleteTournament(id: Int) {
        val response = apiService.deleteTournament(id)
        if (!response.success) throw Exception(response.message ?: "Failed to delete tournament")
    }
}

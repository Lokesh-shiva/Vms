package com.example.vmsadmin.data

import com.example.vmsadmin.models.CreateTournamentMatchRequest
import com.example.vmsadmin.models.CreateTournamentRequest
import com.example.vmsadmin.models.RecordMatchResultRequest
import com.example.vmsadmin.models.Tournament
import com.example.vmsadmin.models.TournamentMatch
import com.example.vmsadmin.models.TournamentRegistration
import com.example.vmsadmin.models.TournamentStanding
import com.example.vmsadmin.models.UpdateTournamentRequest
import com.example.vmsadmin.network.ApiService

class TournamentRepository(private val apiService: ApiService) {

    suspend fun getTournaments(): List<Tournament> {
        val response = apiService.getTournaments()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch tournaments")
    }

    suspend fun getMySponsoredTournaments(): List<Tournament> {
        val response = apiService.getMySponsoredTournaments()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch sponsored tournaments")
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

    suspend fun getMatches(tournamentId: Int): List<TournamentMatch> {
        val response = apiService.getTournamentMatches(tournamentId)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch matches")
    }

    suspend fun createMatch(tournamentId: Int, request: CreateTournamentMatchRequest): TournamentMatch {
        val response = apiService.createTournamentMatch(tournamentId, request)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to schedule match")
    }

    suspend fun recordMatchResult(tournamentId: Int, matchId: Int, homeScore: Int, awayScore: Int): TournamentMatch {
        val response = apiService.recordMatchResult(tournamentId, matchId, RecordMatchResultRequest(homeScore, awayScore))
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to record result")
    }

    suspend fun getStandings(tournamentId: Int): List<TournamentStanding> {
        val response = apiService.getTournamentStandings(tournamentId)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch standings")
    }

    suspend fun getRegistrations(tournamentId: Int): List<TournamentRegistration> {
        val response = apiService.getTournamentRegistrations(tournamentId)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch registrations")
    }
}

package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.SportVotesResponse
import com.example.vmsuser.models.Tournament
import com.example.vmsuser.models.TournamentStanding
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.toUserMessage
import retrofit2.HttpException

class TournamentRepository {
    private val api = RetrofitClient.api

    suspend fun getTournaments(): Result<List<Tournament>> = try {
        val res = api.getTournaments()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("TournamentRepo", "getTournaments", e); Result.failure(e) }

    suspend fun getTournament(id: Int): Result<Tournament> = try {
        val res = api.getTournament(id)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Not found"))
    } catch (e: Exception) { Log.e("TournamentRepo", "getTournament", e); Result.failure(e) }

    suspend fun register(id: Int): Result<Unit> = try {
        val res = api.registerTournament(id)
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed to register."))
    } catch (e: HttpException) {
        Log.e("TournamentRepo", "register", e)
        Result.failure(Exception(e.toUserMessage("Failed to register.")))
    } catch (e: Exception) {
        Log.e("TournamentRepo", "register", e)
        Result.failure(Exception(e.message ?: "Failed to register."))
    }

    suspend fun withdraw(id: Int): Result<Unit> = try {
        val res = api.withdrawTournament(id)
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed to withdraw."))
    } catch (e: HttpException) {
        Log.e("TournamentRepo", "withdraw", e)
        Result.failure(Exception(e.toUserMessage("Failed to withdraw.")))
    } catch (e: Exception) {
        Log.e("TournamentRepo", "withdraw", e)
        Result.failure(Exception(e.message ?: "Failed to withdraw."))
    }

    suspend fun getStandings(id: Int): Result<List<TournamentStanding>> = try {
        val res = api.getTournamentStandings(id)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to load standings."))
    } catch (e: HttpException) {
        Log.e("TournamentRepo", "getStandings", e)
        Result.failure(Exception(e.toUserMessage("Failed to load standings.")))
    } catch (e: Exception) {
        Log.e("TournamentRepo", "getStandings", e)
        Result.failure(Exception(e.message ?: "Failed to load standings."))
    }

    suspend fun getVotes(): Result<SportVotesResponse> = try {
        val res = api.getSportVotes()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to load votes."))
    } catch (e: HttpException) {
        Log.e("TournamentRepo", "getVotes", e)
        Result.failure(Exception(e.toUserMessage("Failed to load votes.")))
    } catch (e: Exception) {
        Log.e("TournamentRepo", "getVotes", e)
        Result.failure(Exception(e.message ?: "Failed to load votes."))
    }

    suspend fun castVote(sport: String): Result<SportVotesResponse> = try {
        val res = api.castSportVote(mapOf("sport" to sport))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to cast vote."))
    } catch (e: HttpException) {
        Log.e("TournamentRepo", "castVote", e)
        Result.failure(Exception(e.toUserMessage("Failed to cast vote.")))
    } catch (e: Exception) {
        Log.e("TournamentRepo", "castVote", e)
        Result.failure(Exception(e.message ?: "Failed to cast vote."))
    }
}

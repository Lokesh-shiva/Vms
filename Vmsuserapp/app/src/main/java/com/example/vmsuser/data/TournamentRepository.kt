package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.Tournament
import com.example.vmsuser.network.RetrofitClient

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
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("TournamentRepo", "register", e); Result.failure(e) }
}

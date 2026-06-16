package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.Match
import com.example.vmsuser.models.OpenMatch
import com.example.vmsuser.models.QueueStatus
import com.example.vmsuser.network.RetrofitClient

class MatchRepository {
    private val api = RetrofitClient.api

    suspend fun getQueueStatus(): Result<QueueStatus> = try {
        val res = api.getQueueStatus()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("MatchRepo", "getQueueStatus", e); Result.failure(e) }

    suspend fun joinQueue(sport: String, skillLevel: String): Result<QueueStatus> = try {
        val res = api.joinQueue(mapOf("sport" to sport, "skill_level" to skillLevel))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("MatchRepo", "joinQueue", e); Result.failure(e) }

    suspend fun getOpenMatches(sport: String? = null): Result<List<OpenMatch>> = try {
        val res = api.getOpenMatches(sport)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("MatchRepo", "getOpenMatches", e); Result.failure(e) }

    suspend fun joinOpenMatch(matchId: Int): Result<Match> = try {
        val res = api.joinOpenMatch(matchId)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("MatchRepo", "joinOpenMatch", e); Result.failure(e) }

    suspend fun leaveQueue(): Result<Unit> = try {
        val res = api.leaveQueue()
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("MatchRepo", "leaveQueue", e); Result.failure(e) }

    suspend fun getActiveMatch(): Result<Match?> = try {
        val res = api.getActiveMatch()
        if (res.success) Result.success(res.data) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("MatchRepo", "getActiveMatch", e); Result.failure(e) }

    suspend fun getMatch(id: Int): Result<Match> = try {
        val res = api.getMatch(id)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("MatchRepo", "getMatch", e); Result.failure(e) }

    suspend fun getMyMatches(): Result<List<Match>> = try {
        val res = api.getMyMatches()
        if (res.success && res.data != null) Result.success(res.data) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("MatchRepo", "getMyMatches", e); Result.failure(e) }
}

package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.CaptainStats
import com.example.vmsuser.network.RetrofitClient

class CaptainRepository {
    private val api = RetrofitClient.api

    suspend fun getStats(): Result<CaptainStats> = try {
        val res = api.getCaptainStats()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("CaptainRepo", "getStats", e); Result.failure(e) }

    suspend fun apply(bio: String, sports: List<String>): Result<Unit> = try {
        val res = api.applyCaptain(mapOf("bio" to bio, "sports" to sports.joinToString(",")))
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("CaptainRepo", "apply", e); Result.failure(e) }
}

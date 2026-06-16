package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.CreateSocietyRequest
import com.example.vmsuser.models.Society
import com.example.vmsuser.models.SocietyMember
import com.example.vmsuser.network.RetrofitClient

class SocialRepository {
    private val api = RetrofitClient.api

    suspend fun getSocieties(): Result<List<Society>> = try {
        val res = api.getSocieties()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("SocialRepo", "getSocieties", e); Result.failure(e) }

    suspend fun getSociety(id: Int): Result<Society> = try {
        val res = api.getSociety(id)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Not found"))
    } catch (e: Exception) { Log.e("SocialRepo", "getSociety", e); Result.failure(e) }

    suspend fun getMembers(id: Int): Result<List<SocietyMember>> = try {
        val res = api.getSocietyMembers(id)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("SocialRepo", "getMembers", e); Result.failure(e) }

    suspend fun join(id: Int): Result<Unit> = try {
        val res = api.joinSociety(id)
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("SocialRepo", "join", e); Result.failure(e) }

    suspend fun leave(id: Int): Result<Unit> = try {
        val res = api.leaveSociety(id)
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("SocialRepo", "leave", e); Result.failure(e) }

    suspend fun create(name: String, sport: String, description: String): Result<Society> = try {
        val res = api.createSociety(CreateSocietyRequest(name, sport, description))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("SocialRepo", "create", e); Result.failure(e) }
}

package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.Notification
import com.example.vmsuser.models.WalletTransaction
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.UserSession

class ProfileRepository {
    private val api = RetrofitClient.api

    suspend fun updateProfile(name: String, city: String): Result<Unit> = try {
        val res = api.updateProfile(mapOf("name" to name, "city" to city))
        if (res.success && res.data != null) { UserSession.setUser(res.data); Result.success(Unit) }
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("ProfileRepo", "updateProfile", e); Result.failure(e) }

    suspend fun getNotifications(): Result<List<Notification>> = try {
        val res = api.getNotifications()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("ProfileRepo", "getNotifications", e); Result.failure(e) }

    suspend fun markNotificationRead(id: Int): Result<Unit> = try {
        val res = api.markNotificationRead(id)
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("ProfileRepo", "markNotificationRead", e); Result.failure(e) }

    suspend fun getWalletTransactions(): Result<List<WalletTransaction>> = try {
        val res = api.getWalletTransactions()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("ProfileRepo", "getWalletTransactions", e); Result.failure(e) }
}

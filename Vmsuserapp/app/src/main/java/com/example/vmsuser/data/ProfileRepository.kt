package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.Notification
import com.example.vmsuser.models.WalletTransaction
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.network.toUserMessage
import retrofit2.HttpException

class ProfileRepository {
    private val api = RetrofitClient.api

    suspend fun updateProfile(
        name: String,
        city: String,
        username: String? = null,
        email: String? = null,
        dateOfBirth: String? = null,
    ): Result<Unit> = try {
        val body = mutableMapOf("name" to name, "city" to city)
        username?.let { body["username"] = it }
        email?.let { body["email"] = it }
        dateOfBirth?.let { body["date_of_birth"] = it }
        val res = api.updateProfile(body)
        if (res.success && res.data != null) { UserSession.setUser(res.data); Result.success(Unit) }
        else Result.failure(Exception(res.message ?: "Failed to save profile."))
    } catch (e: HttpException) {
        Log.e("ProfileRepo", "updateProfile", e)
        Result.failure(Exception(e.toUserMessage("Failed to save profile.")))
    } catch (e: Exception) {
        Log.e("ProfileRepo", "updateProfile", e)
        Result.failure(Exception(e.message ?: "Failed to save profile."))
    }

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

    suspend fun getWalletBalance(): Result<Int> = try {
        val res = api.getWalletBalance()
        if (res.success && res.data != null) Result.success(res.data["balance"] ?: 0)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("ProfileRepo", "getWalletBalance", e); Result.failure(e) }
}

package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.CreateDisputeRequest
import com.example.vmsuser.models.Dispute
import com.example.vmsuser.models.DisputeMessage
import com.example.vmsuser.models.SendDisputeMessageRequest
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.toUserMessage

class SupportRepository {
    private val api = RetrofitClient.api

    suspend fun getMyDisputes(): Result<List<Dispute>> = try {
        val res = api.getMyDisputes()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("SupportRepo", "getMyDisputes", e); Result.failure(e) }

    suspend fun createDispute(title: String, description: String): Result<Dispute> = try {
        val res = api.createDispute(CreateDisputeRequest(title, description))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("SupportRepo", "createDispute", e); Result.failure(e) }

    suspend fun getMessages(disputeId: Int): Result<List<DisputeMessage>> = try {
        val res = api.getDisputeMessages(disputeId)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) {
        Log.e("SupportRepo", "getMessages", e)
        Result.failure(Exception(e.toUserMessage("Couldn't load messages.")))
    }

    suspend fun sendMessage(disputeId: Int, body: String): Result<DisputeMessage> = try {
        val res = api.sendDisputeMessage(disputeId, SendDisputeMessageRequest(body))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) {
        Log.e("SupportRepo", "sendMessage", e)
        Result.failure(Exception(e.toUserMessage("Couldn't send message.")))
    }
}

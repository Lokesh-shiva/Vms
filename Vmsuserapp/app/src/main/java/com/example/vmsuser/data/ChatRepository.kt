package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.ChatMessage
import com.example.vmsuser.models.ChatMessageDto
import com.example.vmsuser.models.ChatThread
import com.example.vmsuser.models.SendMessageRequest
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.UserSession

class ChatRepository {
    private val api = RetrofitClient.api

    private fun ChatMessageDto.toUi(otherName: String): ChatMessage {
        val selfId = UserSession.currentUser?.id
        return ChatMessage(
            id = id.toString(),
            fromId = senderId ?: 0,
            fromName = if (senderId == selfId) "You" else otherName,
            text = body,
            timestamp = createdAt?.takeLast(8)?.take(5) ?: "",
            isSelf = senderId != null && senderId == selfId,
        )
    }

    suspend fun getThreads(): Result<List<ChatThread>> = try {
        val res = api.getChatThreads()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("ChatRepo", "getThreads", e); Result.failure(e) }

    suspend fun getMessages(matchId: Int, otherName: String): Result<List<ChatMessage>> = try {
        val res = api.getChatMessages(matchId)
        if (res.success && res.data != null) Result.success(res.data.map { it.toUi(otherName) })
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("ChatRepo", "getMessages", e); Result.failure(e) }

    suspend fun sendMessage(matchId: Int, body: String, otherName: String): Result<ChatMessage> = try {
        val res = api.sendChatMessage(matchId, SendMessageRequest(body))
        if (res.success && res.data != null) Result.success(res.data.toUi(otherName))
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("ChatRepo", "sendMessage", e); Result.failure(e) }
}

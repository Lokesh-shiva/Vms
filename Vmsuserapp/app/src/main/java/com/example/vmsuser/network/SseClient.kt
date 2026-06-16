package com.example.vmsuser.network

import android.util.Log
import com.example.vmsuser.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

sealed class SseEvent {
    data class QueueUpdate(val position: Int, val total: Int) : SseEvent()
    data class MatchFound(val matchId: Int) : SseEvent()
    object Disconnected : SseEvent()
}

class SseClient(private val tokenManager: AuthTokenManager) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun queueEvents(): Flow<SseEvent> = callbackFlow {
        val token = runBlocking { tokenManager.getToken() }
        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}api/v1/matchmaking/events")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(source: EventSource, id: String?, type: String?, data: String) {
                when (type) {
                    "queue_update" -> {
                        val parts = data.split(",")
                        if (parts.size >= 2) {
                            trySend(SseEvent.QueueUpdate(parts[0].trim().toIntOrNull() ?: 0, parts[1].trim().toIntOrNull() ?: 0))
                        }
                    }
                    "match_found" -> {
                        trySend(SseEvent.MatchFound(data.trim().toIntOrNull() ?: 0))
                    }
                }
            }
            override fun onFailure(source: EventSource, t: Throwable?, response: Response?) {
                Log.w("SseClient", "SSE disconnected: ${t?.message}")
                trySend(SseEvent.Disconnected)
                close()
            }
        }

        val factory = EventSources.createFactory(client)
        val source = factory.newEventSource(request, listener)
        awaitClose { source.cancel() }
    }
}

package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.ChatRepository
import com.example.vmsuser.models.ChatMessage
import com.example.vmsuser.models.ChatThread
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 3000L

class ChatViewModel : ViewModel() {
    private val repo = ChatRepository()

    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var pollJob: Job? = null

    private suspend fun fetchThreads() {
        _loading.value = true
        repo.getThreads()
            .onSuccess { _threads.value = it }
            .onFailure { Log.e("ChatVM", "loadThreads: ${it.message}") }
        _loading.value = false
    }

    fun loadThreads() {
        viewModelScope.launch { fetchThreads() }
    }

    /**
     * Start (or restart) polling messages for a thread. threadId is the match_id as a string.
     * Loads the thread list first if empty — ChatThreadScreen may be opened with a fresh
     * ViewModel instance (nav-scoped) that never called loadThreads().
     */
    fun openThread(threadId: String) {
        val matchId = threadId.toIntOrNull() ?: return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            if (_threads.value.isEmpty()) fetchThreads()
            val otherName = _threads.value.find { it.id == threadId }?.name ?: "Player"
            while (true) {
                repo.getMessages(matchId, otherName).onSuccess { _messages.value = it }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun sendMessage(threadId: String, text: String) {
        val matchId = threadId.toIntOrNull() ?: return
        if (text.isBlank()) return
        val otherName = _threads.value.find { it.id == threadId }?.name ?: "Player"
        viewModelScope.launch {
            repo.sendMessage(matchId, text, otherName)
                .onSuccess { _messages.value = _messages.value + it }
                .onFailure { Log.e("ChatVM", "sendMessage: ${it.message}") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}

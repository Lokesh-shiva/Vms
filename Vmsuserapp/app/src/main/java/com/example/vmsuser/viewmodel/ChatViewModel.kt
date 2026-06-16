package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vmsuser.data.ChatRepository
import com.example.vmsuser.models.ChatMessage
import com.example.vmsuser.models.ChatThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {
    private val repo = ChatRepository()
    private val _threads = MutableStateFlow<List<ChatThread>>(repo.getMockThreads())
    val threads: StateFlow<List<ChatThread>> = _threads
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun loadThread(threadId: String) {
        _messages.value = repo.getMockMessages(threadId)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val msg = ChatMessage(id = System.currentTimeMillis().toString(), fromId = 1, fromName = "You", text = text, timestamp = "now", isSelf = true)
        _messages.value = _messages.value + msg
    }
}

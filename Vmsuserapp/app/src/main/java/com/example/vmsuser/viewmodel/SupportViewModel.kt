package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.SupportRepository
import com.example.vmsuser.models.Dispute
import com.example.vmsuser.models.DisputeMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val MESSAGE_POLL_INTERVAL_MS = 3000L

class SupportViewModel : ViewModel() {
    private val repo = SupportRepository()

    private val _tickets = MutableStateFlow<List<Dispute>>(emptyList())
    val tickets: StateFlow<List<Dispute>> = _tickets

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _messages = MutableStateFlow<List<DisputeMessage>>(emptyList())
    val messages: StateFlow<List<DisputeMessage>> = _messages

    private val _messagesError = MutableStateFlow<String?>(null)
    val messagesError: StateFlow<String?> = _messagesError

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private var pollJob: Job? = null

    fun openThread(disputeId: Int) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                repo.getMessages(disputeId)
                    .onSuccess { _messages.value = it; _messagesError.value = null }
                    .onFailure { _messagesError.value = it.message }
                delay(MESSAGE_POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun sendMessage(disputeId: Int, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            _sending.value = true
            repo.sendMessage(disputeId, body)
                .onSuccess { _messages.value = _messages.value + it }
                .onFailure { _messagesError.value = it.message }
            _sending.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    fun loadTickets() {
        viewModelScope.launch {
            _loading.value = true
            repo.getMyDisputes()
                .onSuccess { _tickets.value = it }
                .onFailure { Log.e("SupportVM", "loadTickets", it) }
            _loading.value = false
        }
    }

    fun raiseTicket(title: String, description: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _submitting.value = true
            _error.value = null
            repo.createDispute(title, description)
                .onSuccess {
                    _tickets.value = listOf(it) + _tickets.value
                    _submitting.value = false
                    onDone()
                }
                .onFailure {
                    _error.value = it.message ?: "Failed to raise ticket"
                    _submitting.value = false
                }
        }
    }

    fun clearError() { _error.value = null }
}

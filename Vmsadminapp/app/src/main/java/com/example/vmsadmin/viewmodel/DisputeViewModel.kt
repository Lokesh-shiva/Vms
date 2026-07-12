package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.DisputeRepository
import com.example.vmsadmin.models.CreateDisputeRequest
import com.example.vmsadmin.models.Dispute
import com.example.vmsadmin.models.DisputeMessage
import com.example.vmsadmin.models.UpdateDisputeRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MESSAGE_POLL_INTERVAL_MS = 3000L

data class DisputeUiState(
    val disputes: List<Dispute> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val updatingIds: Set<Int> = emptySet(),
    val messages: List<DisputeMessage> = emptyList(),
    val messagesError: String? = null,
    val sending: Boolean = false,
    val selectedDisputeId: Int? = null,
)

class DisputeViewModel(private val repository: DisputeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DisputeUiState())
    val uiState: StateFlow<DisputeUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init { loadDisputes() }

    fun openThread(disputeId: Int) {
        _uiState.value = _uiState.value.copy(selectedDisputeId = disputeId, messages = emptyList(), messagesError = null)
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                try {
                    val messages = repository.getMessages(disputeId)
                    _uiState.value = _uiState.value.copy(messages = messages, messagesError = null)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(messagesError = e.message)
                }
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
            _uiState.value = _uiState.value.copy(sending = true)
            try {
                val sent = repository.sendMessage(disputeId, body)
                _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + sent, sending = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(sending = false, messagesError = e.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    fun loadDisputes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(
                    disputes = repository.getDisputes(), isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun refreshDisputes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                _uiState.value = _uiState.value.copy(
                    disputes = repository.getDisputes(), isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, error = e.message)
            }
        }
    }

    fun createDispute(request: CreateDisputeRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.createDispute(request)
                loadDisputes()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create dispute"
                )
            }
        }
    }

    fun resolve(id: Int, note: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds + id)
            try {
                val updated = repository.updateDispute(
                    id,
                    UpdateDisputeRequest(status = "RESOLVED", resolution_note = note.ifBlank { null })
                )
                _uiState.value = _uiState.value.copy(
                    disputes = _uiState.value.disputes.map { if (it.id == id) updated else it },
                    updatingIds = _uiState.value.updatingIds - id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    updatingIds = _uiState.value.updatingIds - id,
                    error = e.message ?: "Failed to resolve dispute"
                )
            }
        }
    }
}

class DisputeViewModelFactory(
    private val repository: DisputeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DisputeViewModel(repository) as T
    }
}

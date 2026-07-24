package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.MatchRepository
import com.example.vmsadmin.models.AdminChatMessage
import com.example.vmsadmin.models.Match
import com.example.vmsadmin.models.MatchDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val MESSAGE_POLL_INTERVAL_MS = 5000L

class MatchViewModel(private val repository: MatchRepository) : ViewModel() {

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _reapMessage = MutableStateFlow<String?>(null)
    val reapMessage: StateFlow<String?> = _reapMessage

    private val _matchDetail = MutableStateFlow<MatchDetail?>(null)
    val matchDetail: StateFlow<MatchDetail?> = _matchDetail

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError

    private val _matchMessages = MutableStateFlow<List<AdminChatMessage>>(emptyList())
    val matchMessages: StateFlow<List<AdminChatMessage>> = _matchMessages

    private var messagePollJob: Job? = null

    init {
        loadMatches()
    }

    fun openMatchDetail(matchId: Int) {
        _matchDetail.value = null
        _matchMessages.value = emptyList()
        _detailError.value = null
        viewModelScope.launch {
            _detailLoading.value = true
            try {
                _matchDetail.value = repository.getMatchDetail(matchId)
            } catch (e: Exception) {
                _detailError.value = e.message ?: "Failed to load match detail"
            } finally {
                _detailLoading.value = false
            }
        }

        messagePollJob?.cancel()
        messagePollJob = viewModelScope.launch {
            while (true) {
                try {
                    _matchMessages.value = repository.getMatchMessages(matchId)
                } catch (_: Exception) {
                    // Non-fatal — chat is a secondary view; keep polling.
                }
                delay(MESSAGE_POLL_INTERVAL_MS)
            }
        }
    }

    fun stopMatchDetailPolling() {
        messagePollJob?.cancel()
        messagePollJob = null
    }

    fun reapAbandonedSessions() {
        viewModelScope.launch {
            try {
                val count = repository.reapAbandonedSessions()
                _reapMessage.value = if (count == 0) "No abandoned sessions found." else "Cleaned up $count abandoned session(s)."
                loadMatches()
            } catch (e: Exception) {
                _reapMessage.value = e.message ?: "Failed to clean up abandoned sessions."
            }
        }
    }

    fun clearReapMessage() { _reapMessage.value = null }

    fun loadMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _matches.value = repository.getMatches()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelMatch(matchId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.cancelMatch(matchId)
                loadMatches()
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun completeMatch(matchId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.completeMatch(matchId)
                loadMatches()
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagePollJob?.cancel()
    }
}

class MatchViewModelFactory(private val repository: MatchRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

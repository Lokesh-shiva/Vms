package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.VoteRoundRepository
import com.example.vmsadmin.models.VoteRoundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoteRoundViewModel(private val repository: VoteRoundRepository) : ViewModel() {

    private val _round = MutableStateFlow<VoteRoundState?>(null)
    val round: StateFlow<VoteRoundState?> = _round.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _round.value = repository.getCurrentRound()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load vote round"
            } finally {
                _loading.value = false
            }
        }
    }

    fun startRound(options: List<String>, closesAt: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _submitting.value = true
            _error.value = null
            try {
                _round.value = repository.createRound(options, closesAt)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start vote round"
            } finally {
                _submitting.value = false
            }
        }
    }

    fun closeRound(roundId: Int) {
        viewModelScope.launch {
            _submitting.value = true
            _error.value = null
            try {
                _round.value = repository.closeRound(roundId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to close vote round"
            } finally {
                _submitting.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}

class VoteRoundViewModelFactory(
    private val repository: VoteRoundRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoteRoundViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VoteRoundViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.SupportRepository
import com.example.vmsuser.models.Dispute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

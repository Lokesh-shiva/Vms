package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.QueueRepository
import com.example.vmsadmin.models.QueueStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QueueOverviewUiState(
    val stats: List<QueueStat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class QueueOverviewViewModel(
    private val repository: QueueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueueOverviewUiState())
    val uiState: StateFlow<QueueOverviewUiState> = _uiState.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val stats = repository.fetchQueueStats()
                _uiState.value = _uiState.value.copy(
                    stats = stats,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load queue stats"
                )
            }
        }
    }
}

class QueueOverviewViewModelFactory(
    private val repository: QueueRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QueueOverviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QueueOverviewViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

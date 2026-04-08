package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.GroundRepository
import com.example.vmsadmin.models.Ground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroundUiState(
    val grounds: List<Ground> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val updatingIds: Set<Int> = emptySet()
)

class GroundViewModel(private val repository: GroundRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GroundUiState())
    val uiState: StateFlow<GroundUiState> = _uiState.asStateFlow()

    init {
        loadGrounds()
    }

    fun loadGrounds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val grounds = repository.getGrounds()
                _uiState.value = _uiState.value.copy(grounds = grounds, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load grounds"
                )
            }
        }
    }

    fun refreshGrounds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val grounds = repository.getGrounds()
                _uiState.value = _uiState.value.copy(grounds = grounds, isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Failed to refresh grounds"
                )
            }
        }
    }

    fun toggleGround(id: Int, isActive: Boolean) {
        val original = _uiState.value.grounds
        // Optimistic update
        _uiState.value = _uiState.value.copy(
            grounds = original.map { if (it.id == id) it.copy(is_active = isActive) else it },
            updatingIds = _uiState.value.updatingIds + id
        )
        viewModelScope.launch {
            try {
                repository.toggleGround(id, isActive)
                _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds - id)
            } catch (e: Exception) {
                // Rollback optimistic update
                _uiState.value = _uiState.value.copy(
                    grounds = original,
                    updatingIds = _uiState.value.updatingIds - id,
                    error = e.message ?: "Failed to update ground"
                )
            }
        }
    }
}

class GroundViewModelFactory(private val repository: GroundRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroundViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroundViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

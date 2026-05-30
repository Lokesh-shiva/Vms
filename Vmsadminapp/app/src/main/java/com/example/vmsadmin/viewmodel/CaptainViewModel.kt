package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.CaptainRepository
import com.example.vmsadmin.models.Captain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CaptainUiState(
    val captains: List<Captain> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val snackbar: String? = null
)

class CaptainViewModel(private val repository: CaptainRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptainUiState())
    val uiState: StateFlow<CaptainUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(captains = repository.getCaptains(), isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load captains")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                _uiState.value = _uiState.value.copy(captains = repository.getCaptains(), isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, snackbar = e.message ?: "Refresh failed")
            }
        }
    }

    fun addCaptain(userId: Int, regionId: Int?) {
        viewModelScope.launch {
            try {
                val captain = repository.createCaptain(userId, regionId, null)
                _uiState.value = _uiState.value.copy(
                    captains = _uiState.value.captains + captain,
                    snackbar = "Captain added"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(snackbar = e.message ?: "Failed to add captain")
            }
        }
    }

    fun updateStatus(id: Int, status: String) {
        viewModelScope.launch {
            try {
                val updated = repository.updateCaptain(id, status = status)
                _uiState.value = _uiState.value.copy(
                    captains = _uiState.value.captains.map { if (it.id == id) updated else it },
                    snackbar = "Status updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(snackbar = e.message ?: "Failed to update status")
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteCaptain(id)
                _uiState.value = _uiState.value.copy(
                    captains = _uiState.value.captains.filter { it.id != id },
                    snackbar = "Captain removed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(snackbar = e.message ?: "Failed to remove captain")
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbar = null)
    }
}

class CaptainViewModelFactory(private val repository: CaptainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaptainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CaptainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

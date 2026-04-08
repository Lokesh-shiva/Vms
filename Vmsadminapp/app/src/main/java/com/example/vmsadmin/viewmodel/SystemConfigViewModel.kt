package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.SystemConfigRepository
import com.example.vmsadmin.models.SystemConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SystemConfigUiState(
    val configs: List<SystemConfig> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SystemConfigViewModel(
    private val repository: SystemConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemConfigUiState())
    val uiState: StateFlow<SystemConfigUiState> = _uiState.asStateFlow()

    fun loadConfigs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val configs = repository.fetchConfigs()
                _uiState.value = _uiState.value.copy(
                    configs = configs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load configurations"
                )
            }
        }
    }

    fun updateConfig(key: String, value: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.updateConfig(key, value)
                loadConfigs()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update configuration"
                )
            }
        }
    }
}

class SystemConfigViewModelFactory(
    private val repository: SystemConfigRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SystemConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SystemConfigViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

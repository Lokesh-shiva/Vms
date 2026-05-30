package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.FeeConfigRepository
import com.example.vmsadmin.models.FeeConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeeConfigUiState(
    val feeConfigs: List<FeeConfig> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingConfig: FeeConfig? = null,
    val showDeleteConfirm: Boolean = false,
    val deletingConfig: FeeConfig? = null
)

class FeeConfigViewModel(
    private val repository: FeeConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeeConfigUiState())
    val uiState: StateFlow<FeeConfigUiState> = _uiState.asStateFlow()

    fun loadConfigs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val configs = repository.getFeeConfigs()
                _uiState.value = _uiState.value.copy(
                    feeConfigs = sortConfigs(configs),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun refreshConfigs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val configs = repository.getFeeConfigs()
                _uiState.value = _uiState.value.copy(
                    feeConfigs = sortConfigs(configs),
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun addConfig(
        regionId: Int,
        cartTypeId: Int,
        bookingFee: Double,
        cancellationFeePct: Double,
        platformFeePct: Double,
        matchingFee: Double = 0.0,
        ratePerBlock: Double = 0.0,
        blockDurationMinutes: Int = 45,
        maxDurationMinutes: Int = 180
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.createFeeConfig(
                    regionId, cartTypeId, bookingFee, cancellationFeePct, platformFeePct,
                    matchingFee, ratePerBlock, blockDurationMinutes, maxDurationMinutes
                )
                delay(200)
                loadConfigs()
                _uiState.update { it.copy(successMessage = "Fee config saved", showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add fee config") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun updateConfig(
        id: Int,
        bookingFee: Double,
        cancellationFeePct: Double,
        platformFeePct: Double,
        matchingFee: Double,
        ratePerBlock: Double,
        blockDurationMinutes: Int,
        maxDurationMinutes: Int,
        surgeEnabled: Boolean,
        surgeMultiplier: Double
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.updateFeeConfig(
                    id = id,
                    bookingFee = bookingFee,
                    cancellationFeePct = cancellationFeePct,
                    platformFeePct = platformFeePct,
                    matchingFee = matchingFee,
                    ratePerBlock = ratePerBlock,
                    blockDurationMinutes = blockDurationMinutes,
                    maxDurationMinutes = maxDurationMinutes,
                    surgeEnabled = surgeEnabled,
                    surgeMultiplier = surgeMultiplier
                )
                delay(200)
                loadConfigs()
                _uiState.update {
                    it.copy(
                        successMessage = "Configuration updated",
                        showEditDialog = false,
                        editingConfig = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update fee config") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun deleteConfig(id: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, showDeleteConfirm = false, deletingConfig = null) }
            try {
                repository.deleteFeeConfig(id)
                delay(200)
                loadConfigs()
                _uiState.update { it.copy(successMessage = "Fee config deactivated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete fee config") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun reactivateConfig(id: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.updateFeeConfig(id = id, isActive = true)
                delay(200)
                loadConfigs()
                _uiState.update { it.copy(successMessage = "Fee config reactivated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to reactivate fee config") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun dismissAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun showEditDialog(config: FeeConfig) {
        _uiState.value = _uiState.value.copy(showEditDialog = true, editingConfig = config)
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, editingConfig = null)
    }

    fun showDeleteConfirm(config: FeeConfig) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true, deletingConfig = config)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false, deletingConfig = null)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun sortConfigs(configs: List<FeeConfig>): List<FeeConfig> {
        return configs.sortedWith(
            compareBy<FeeConfig> { !it.is_active }
                .thenBy { it.region_name ?: "" }
                .thenBy { it.cart_type_name ?: "" }
        )
    }
}

class FeeConfigViewModelFactory(
    private val repository: FeeConfigRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeeConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeeConfigViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

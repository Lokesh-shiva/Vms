package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.RegionRepository
import com.example.vmsadmin.models.Region
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegionUiState(
    val regions: List<Region> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingRegion: Region? = null,
    val showDeleteConfirm: Boolean = false,
    val deletingRegion: Region? = null,
    val updatingIds: Set<Int> = emptySet()
)

class RegionViewModel(
    private val regionRepository: RegionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegionUiState())
    val uiState: StateFlow<RegionUiState> = _uiState.asStateFlow()

    fun loadRegions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val regions = regionRepository.getRegions()
                _uiState.value = _uiState.value.copy(
                    regions = regions,
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

    fun refreshRegions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val regions = regionRepository.getRegions()
                _uiState.value = _uiState.value.copy(
                    regions = regions,
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

    fun addRegion(name: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                regionRepository.createRegion(name)
                delay(200)
                loadRegions()
                _uiState.update { it.copy(successMessage = "Region saved", showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add region") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun updateRegion(id: Int, name: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                regionRepository.updateRegion(id, name)
                delay(200)
                loadRegions()
                _uiState.update { it.copy(successMessage = "Region updated", showEditDialog = false, editingRegion = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update region") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun toggleRegion(id: Int, isActive: Boolean) {
        val originalList = _uiState.value.regions
        val updatedList = originalList.map { 
            if (it.id == id) it.copy(is_serviceable = isActive) else it 
        }
        _uiState.value = _uiState.value.copy(
            regions = updatedList,
            updatingIds = _uiState.value.updatingIds + id,
            error = null
        )
        viewModelScope.launch {
            try {
                regionRepository.toggleRegionActive(id, isActive)
                _uiState.value = _uiState.value.copy(
                    updatingIds = _uiState.value.updatingIds - id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    regions = originalList,
                    updatingIds = _uiState.value.updatingIds - id,
                    error = e.message ?: "Failed to toggle region"
                )
            }
        }
    }

    fun deleteRegion(id: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, showDeleteConfirm = false, deletingRegion = null) }
            try {
                regionRepository.deleteRegion(id)
                delay(200)
                loadRegions()
                _uiState.update { it.copy(successMessage = "Region deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete region") }
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

    fun showEditDialog(region: Region) {
        _uiState.value = _uiState.value.copy(showEditDialog = true, editingRegion = region)
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, editingRegion = null)
    }

    fun showDeleteConfirm(region: Region) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true, deletingRegion = region)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false, deletingRegion = null)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

class RegionViewModelFactory(
    private val regionRepository: RegionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegionViewModel(regionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

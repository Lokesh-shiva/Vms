package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.RegionRepository
import com.example.vmsadmin.models.Region
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegionUiState(
    val regions: List<Region> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingRegion: Region? = null,
    val showDeleteConfirm: Boolean = false,
    val deletingRegion: Region? = null
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                regionRepository.createRegion(name)
                _uiState.value = _uiState.value.copy(showAddDialog = false)
                loadRegions()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add region"
                )
            }
        }
    }

    fun updateRegion(id: Int, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                regionRepository.updateRegion(id, name)
                _uiState.value = _uiState.value.copy(showEditDialog = false, editingRegion = null)
                loadRegions()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update region"
                )
            }
        }
    }

    fun toggleRegion(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
            try {
                regionRepository.toggleRegionActive(id, isActive)
                loadRegions()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to toggle region"
                )
            }
        }
    }

    fun deleteRegion(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, error = null,
                showDeleteConfirm = false, deletingRegion = null
            )
            try {
                regionRepository.deleteRegion(id)
                loadRegions()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete region"
                )
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
        _uiState.value = _uiState.value.copy(error = null)
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

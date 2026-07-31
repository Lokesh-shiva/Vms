package com.example.vmsadmin.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.GroundRepository
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.Ground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroundUiState(
    val grounds: List<Ground> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val updatingIds: Set<Int> = emptySet(),
    val ownerSearchResult: AppUser? = null,
    val ownerSearchLoading: Boolean = false,
    val ownerSearchError: String? = null,
    val uploadingImageIds: Set<Int> = emptySet(),
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

    fun searchOwnerByPhone(phone: String) {
        if (phone.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(ownerSearchLoading = true, ownerSearchError = null, ownerSearchResult = null) }
            try {
                val user = repository.searchUserByPhone(phone)
                _uiState.update {
                    it.copy(
                        ownerSearchLoading = false,
                        ownerSearchResult = user,
                        ownerSearchError = if (user == null) "User not found" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(ownerSearchLoading = false, ownerSearchError = e.message ?: "Search failed") }
            }
        }
    }

    fun assignOwner(groundId: Int, ownerUserId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingIds = it.updatingIds + groundId) }
            try {
                val updated = repository.assignOwner(groundId, ownerUserId)
                _uiState.update { state ->
                    state.copy(
                        grounds = state.grounds.map { if (it.id == groundId) updated else it },
                        updatingIds = state.updatingIds - groundId,
                        ownerSearchResult = null,
                        ownerSearchError = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(updatingIds = it.updatingIds - groundId, error = e.message ?: "Failed to assign owner") }
            }
        }
    }

    fun clearOwnerSearch() {
        _uiState.update { it.copy(ownerSearchResult = null, ownerSearchError = null) }
    }

    fun uploadGroundImage(contentResolver: ContentResolver, groundId: Int, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingImageIds = it.uploadingImageIds + groundId, error = null) }
            try {
                val updated = repository.uploadGroundImage(contentResolver, groundId, imageUri)
                _uiState.update { state ->
                    state.copy(
                        grounds = state.grounds.map { if (it.id == groundId) updated else it },
                        uploadingImageIds = state.uploadingImageIds - groundId,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(uploadingImageIds = it.uploadingImageIds - groundId, error = e.message ?: "Failed to upload image")
                }
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

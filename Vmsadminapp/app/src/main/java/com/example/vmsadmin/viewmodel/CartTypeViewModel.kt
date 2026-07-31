package com.example.vmsadmin.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.CartTypeRepository
import com.example.vmsadmin.models.CartType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartTypeUiState(
    val cartTypes: List<CartType> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingCartType: CartType? = null,
    val showDeleteConfirm: Boolean = false,
    val deletingCartType: CartType? = null,
    val updatingIds: Set<Int> = emptySet(),
    val uploadingImageIds: Set<Int> = emptySet(),
)

class CartTypeViewModel(
    private val cartTypeRepository: CartTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartTypeUiState())
    val uiState: StateFlow<CartTypeUiState> = _uiState.asStateFlow()

    fun loadCartTypes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val cartTypes = cartTypeRepository.getCartTypes()
                _uiState.value = _uiState.value.copy(
                    cartTypes = cartTypes,
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

    fun refreshCartTypes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val cartTypes = cartTypeRepository.getCartTypes()
                _uiState.value = _uiState.value.copy(
                    cartTypes = cartTypes,
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

    fun addCartType(name: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                cartTypeRepository.createCartType(name)
                delay(200)
                loadCartTypes()
                _uiState.update { it.copy(successMessage = "Cart type saved", showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add cart type") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun updateCartType(id: Int, name: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                cartTypeRepository.updateCartType(id, name)
                delay(200)
                loadCartTypes()
                _uiState.update { it.copy(successMessage = "Cart type updated", showEditDialog = false, editingCartType = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update cart type") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun toggleCartType(id: Int, isActive: Boolean) {
        val originalList = _uiState.value.cartTypes
        val updatedList = originalList.map { 
            if (it.id == id) it.copy(is_active = isActive) else it 
        }
        _uiState.value = _uiState.value.copy(
            cartTypes = updatedList,
            updatingIds = _uiState.value.updatingIds + id,
            error = null
        )
        viewModelScope.launch {
            try {
                cartTypeRepository.toggleCartTypeActive(id, isActive)
                _uiState.value = _uiState.value.copy(
                    updatingIds = _uiState.value.updatingIds - id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cartTypes = originalList,
                    updatingIds = _uiState.value.updatingIds - id,
                    error = e.message ?: "Failed to toggle cart type"
                )
            }
        }
    }

    fun uploadCartTypeImage(contentResolver: ContentResolver, id: Int, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingImageIds = it.uploadingImageIds + id, error = null) }
            try {
                val updated = cartTypeRepository.uploadCartTypeImage(contentResolver, id, imageUri)
                _uiState.update { state ->
                    state.copy(
                        cartTypes = state.cartTypes.map { if (it.id == id) updated else it },
                        uploadingImageIds = state.uploadingImageIds - id,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(uploadingImageIds = it.uploadingImageIds - id, error = e.message ?: "Failed to upload image")
                }
            }
        }
    }

    fun deleteCartType(id: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, showDeleteConfirm = false, deletingCartType = null) }
            try {
                cartTypeRepository.deleteCartType(id)
                delay(200)
                loadCartTypes()
                _uiState.update { it.copy(successMessage = "Cart type deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete cart type") }
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

    fun showEditDialog(cartType: CartType) {
        _uiState.value = _uiState.value.copy(showEditDialog = true, editingCartType = cartType)
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, editingCartType = null)
    }

    fun showDeleteConfirm(cartType: CartType) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true, deletingCartType = cartType)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false, deletingCartType = null)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

class CartTypeViewModelFactory(
    private val cartTypeRepository: CartTypeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartTypeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartTypeViewModel(cartTypeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.CartRepository
import com.example.vmsadmin.models.Cart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val carts: List<Cart> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingCart: Cart? = null,
    val showDeleteConfirm: Boolean = false,
    val deletingCart: Cart? = null,
    val selectedCart: Cart? = null,
    val updatingCartIds: Set<Int> = emptySet()
)

class CartViewModel(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    fun loadCarts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val carts = cartRepository.getCarts()
                _uiState.value = _uiState.value.copy(
                    carts = carts,
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

    fun refreshCarts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val carts = cartRepository.getCarts()
                _uiState.value = _uiState.value.copy(
                    carts = carts,
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

    fun addCart(label: String, regionId: Int, cartTypeId: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                cartRepository.createCart(
                    label = label,
                    regionId = regionId,
                    cartTypeId = cartTypeId
                )
                delay(200)
                loadCarts()
                _uiState.update { it.copy(successMessage = "Cart saved", showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add cart") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun updateCart(id: Int, label: String, regionId: Int, cartTypeId: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                cartRepository.updateCart(
                    id = id,
                    label = label,
                    regionId = regionId,
                    cartTypeId = cartTypeId
                )
                delay(200)
                loadCarts()
                _uiState.update { it.copy(successMessage = "Cart updated", showEditDialog = false, editingCart = null, selectedCart = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update cart") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun toggleCart(id: Int, isActive: Boolean) {
        val originalList = _uiState.value.carts
        val updatedList = originalList.map {
            if (it.id == id) {
                it.copy(is_active = isActive)
            } else {
                it
            }
        }
        _uiState.value = _uiState.value.copy(
            carts = updatedList,
            updatingCartIds = _uiState.value.updatingCartIds + id,
            error = null
        )
        viewModelScope.launch {
            try {
                cartRepository.toggleCartActive(id, isActive)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    carts = originalList,
                    error = e.message ?: "Failed to toggle cart"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    updatingCartIds = _uiState.value.updatingCartIds - id
                )
            }
        }
    }

    fun deleteCart(id: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, showDeleteConfirm = false, deletingCart = null) }
            try {
                cartRepository.deleteCart(id)
                delay(200)
                loadCarts()
                _uiState.update { it.copy(successMessage = "Cart deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete cart") }
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

    fun showEditDialog(cart: Cart) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            editingCart = cart,
            selectedCart = cart
        )
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            editingCart = null,
            selectedCart = null
        )
    }

    fun showDeleteConfirm(cart: Cart) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            deletingCart = cart,
            selectedCart = cart
        )
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            deletingCart = null,
            selectedCart = null
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

class CartViewModelFactory(
    private val cartRepository: CartRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(cartRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

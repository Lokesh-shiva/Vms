package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.models.CartType
import com.example.vmsuser.models.Item
import com.example.vmsuser.repository.ItemRepository
import com.example.vmsuser.repository.LocationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val items: List<Item> = emptyList(),
    val groupedItems: Map<Int, List<Item>> = emptyMap(),
    val cartTypes: List<CartType> = emptyList(),
    val cart: Map<Int, Int> = emptyMap(),   // itemId → quantity
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val locationRepository: LocationRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHome() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val cartTypesDeferred = async { locationRepository.getCartTypes() }
                val itemsDeferred = async { itemRepository.getItems() }

                val cartTypes = cartTypesDeferred.await().sortedBy { it.name }
                val allItems = itemsDeferred.await()

                // Filter available items and sort by name
                val availableItems = allItems
                    .filter { it.is_available }
                    .sortedBy { it.name }

                // Group by cart_type_id; only include groups with at least one item
                val grouped = availableItems
                    .groupBy { it.cart_type_id }
                    .filter { it.value.isNotEmpty() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cartTypes = cartTypes,
                        items = availableItems,
                        groupedItems = grouped,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load home data")
                }
            }
        }
    }

    fun addItem(itemId: Int) {
        _uiState.update { state ->
            val updated = state.cart.toMutableMap()
            updated[itemId] = (updated[itemId] ?: 0) + 1
            state.copy(cart = updated)
        }
    }

    fun removeItem(itemId: Int) {
        _uiState.update { state ->
            val updated = state.cart.toMutableMap()
            val current = updated[itemId] ?: 0
            if (current <= 1) {
                updated.remove(itemId)
            } else {
                updated[itemId] = current - 1
            }
            state.copy(cart = updated)
        }
    }

    fun getQuantity(itemId: Int): Int = _uiState.value.cart[itemId] ?: 0
}

class HomeViewModelFactory(
    private val locationRepository: LocationRepository,
    private val itemRepository: ItemRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(locationRepository, itemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

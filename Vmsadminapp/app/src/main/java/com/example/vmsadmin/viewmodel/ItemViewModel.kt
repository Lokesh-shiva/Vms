package com.example.vmsadmin.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.CartTypeRepository
import com.example.vmsadmin.data.ItemRepository
import com.example.vmsadmin.models.CartType
import com.example.vmsadmin.models.Item
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemUiState(
    val items: List<Item> = emptyList(),
    val cartTypes: List<CartType> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingItem: Item? = null,
    val showDeleteConfirm: Boolean = false,
    val deletingItem: Item? = null,
    val updatingIds: Set<Int> = emptySet(),
    val updatingCartTypeIds: Set<Int> = emptySet(),
    val uploadingImageIds: Set<Int> = emptySet(),
)

class ItemViewModel(
    private val itemRepository: ItemRepository,
    private val cartTypeRepository: CartTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemUiState())
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val itemsDeferred = async { itemRepository.getItems() }
                val cartTypesDeferred = async { cartTypeRepository.getCartTypes() }
                val rawItems = itemsDeferred.await()
                val cartTypes = cartTypesDeferred.await()
                val cartTypeMap = cartTypes.associate { it.id to it.name }
                val enrichedItems = rawItems
                    .map { item -> item.copy(cart_type_name = cartTypeMap[item.cart_type_id]) }
                    .sortedWith(compareBy({ cartTypeMap[it.cart_type_id] ?: "" }, { it.name }))
                _uiState.value = _uiState.value.copy(
                    items = enrichedItems,
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

    fun refreshItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val itemsDeferred = async { itemRepository.getItems() }
                val cartTypesDeferred = async { cartTypeRepository.getCartTypes() }
                val rawItems = itemsDeferred.await()
                val cartTypes = cartTypesDeferred.await()
                val cartTypeMap = cartTypes.associate { it.id to it.name }
                val enrichedItems = rawItems
                    .map { item -> item.copy(cart_type_name = cartTypeMap[item.cart_type_id]) }
                    .sortedWith(compareBy({ cartTypeMap[it.cart_type_id] ?: "" }, { it.name }))
                _uiState.value = _uiState.value.copy(
                    items = enrichedItems,
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

    fun addItem(
        name: String,
        price: Double,
        cartTypeId: Int,
        description: String? = null,
        imageUrl: String? = null
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                itemRepository.createItem(
                    name = name,
                    price = price,
                    cartTypeId = cartTypeId,
                    description = description?.ifBlank { null },
                    imageUrl = imageUrl?.ifBlank { null }
                )
                delay(200)
                loadItems()
                _uiState.update { it.copy(successMessage = "Item added", showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add item") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun updateItem(
        id: Int,
        name: String,
        price: Double,
        cartTypeId: Int,
        description: String? = null,
        imageUrl: String? = null
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                itemRepository.updateItem(
                    id = id,
                    name = name,
                    price = price,
                    cartTypeId = cartTypeId,
                    description = description?.ifBlank { null },
                    imageUrl = imageUrl?.ifBlank { null }
                )
                delay(200)
                loadItems()
                _uiState.update {
                    it.copy(
                        successMessage = "Item updated",
                        showEditDialog = false,
                        editingItem = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update item") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun toggleItem(id: Int, isAvailable: Boolean) {
        val originalList = _uiState.value.items
        val updatedList = originalList.map {
            if (it.id == id) it.copy(is_available = isAvailable) else it
        }
        _uiState.value = _uiState.value.copy(
            items = updatedList,
            updatingIds = _uiState.value.updatingIds + id,
            error = null
        )
        viewModelScope.launch {
            try {
                itemRepository.toggleItem(id, isAvailable)
                _uiState.value = _uiState.value.copy(
                    updatingIds = _uiState.value.updatingIds - id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    items = originalList,
                    updatingIds = _uiState.value.updatingIds - id,
                    error = e.message ?: "Failed to toggle item"
                )
            }
        }
    }

    fun toggleItemsByCartType(cartTypeId: Int, isAvailable: Boolean) {
        if (cartTypeId in _uiState.value.updatingCartTypeIds) return
        val originalList = _uiState.value.items
        val affectedIds = originalList.filter { it.cart_type_id == cartTypeId }.map { it.id }.toSet()
        val updatedList = originalList.map {
            if (it.cart_type_id == cartTypeId) it.copy(is_available = isAvailable) else it
        }
        _uiState.value = _uiState.value.copy(
            items = updatedList,
            updatingCartTypeIds = _uiState.value.updatingCartTypeIds + cartTypeId,
            updatingIds = _uiState.value.updatingIds + affectedIds,
            error = null
        )
        viewModelScope.launch {
            var hadFailure = false
            for (id in affectedIds) {
                try {
                    itemRepository.toggleItem(id, isAvailable)
                } catch (_: Exception) {
                    hadFailure = true
                }
            }
            if (hadFailure) {
                _uiState.value = _uiState.value.copy(
                    items = originalList,
                    updatingCartTypeIds = _uiState.value.updatingCartTypeIds - cartTypeId,
                    updatingIds = _uiState.value.updatingIds - affectedIds,
                    error = "Some items failed to update"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    updatingCartTypeIds = _uiState.value.updatingCartTypeIds - cartTypeId,
                    updatingIds = _uiState.value.updatingIds - affectedIds
                )
            }
        }
    }

    fun uploadItemImage(contentResolver: ContentResolver, id: Int, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingImageIds = it.uploadingImageIds + id, error = null) }
            try {
                itemRepository.uploadItemImage(contentResolver, id, imageUri)
                loadItems()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to upload image") }
            } finally {
                _uiState.update { it.copy(uploadingImageIds = it.uploadingImageIds - id) }
            }
        }
    }

    fun deleteItem(id: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true, error = null, showDeleteConfirm = false, deletingItem = null)
            }
            try {
                itemRepository.deleteItem(id)
                delay(200)
                loadItems()
                _uiState.update { it.copy(successMessage = "Item deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete item") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun showAddDialog()    { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun dismissAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }

    fun showEditDialog(item: Item)  { _uiState.value = _uiState.value.copy(showEditDialog = true, editingItem = item) }
    fun dismissEditDialog()         { _uiState.value = _uiState.value.copy(showEditDialog = false, editingItem = null) }

    fun showDeleteConfirm(item: Item) { _uiState.value = _uiState.value.copy(showDeleteConfirm = true, deletingItem = item) }
    fun dismissDeleteConfirm()        { _uiState.value = _uiState.value.copy(showDeleteConfirm = false, deletingItem = null) }

    fun clearError()   { _uiState.update { it.copy(error = null) } }
    fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}

class ItemViewModelFactory(
    private val itemRepository: ItemRepository,
    private val cartTypeRepository: CartTypeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(itemRepository, cartTypeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

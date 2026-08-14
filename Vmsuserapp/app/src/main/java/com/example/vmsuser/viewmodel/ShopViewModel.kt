package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.ShopRepository
import com.example.vmsuser.models.OrderDto
import com.example.vmsuser.models.ShopItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShopViewModel : ViewModel() {
    private val repo = ShopRepository()

    private val _items = MutableStateFlow<List<ShopItem>>(emptyList())
    val items: StateFlow<List<ShopItem>> = _items
    private val _itemsLoading = MutableStateFlow(true)
    val itemsLoading: StateFlow<Boolean> = _itemsLoading
    private val _itemsError = MutableStateFlow<String?>(null)
    val itemsError: StateFlow<String?> = _itemsError

    // itemId -> quantity
    private val _cart = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cart: StateFlow<Map<Int, Int>> = _cart

    private val _placingOrder = MutableStateFlow(false)
    val placingOrder: StateFlow<Boolean> = _placingOrder
    private val _orderError = MutableStateFlow<String?>(null)
    val orderError: StateFlow<String?> = _orderError
    private val _lastOrder = MutableStateFlow<OrderDto?>(null)
    val lastOrder: StateFlow<OrderDto?> = _lastOrder

    private val _orders = MutableStateFlow<List<OrderDto>>(emptyList())
    val orders: StateFlow<List<OrderDto>> = _orders
    private val _ordersLoading = MutableStateFlow(false)
    val ordersLoading: StateFlow<Boolean> = _ordersLoading

    init { loadItems() }

    fun loadItems(cartTypeId: Int? = null) {
        viewModelScope.launch {
            _itemsLoading.value = true
            _itemsError.value = null
            repo.getItems(cartTypeId)
                .onSuccess { _items.value = it.filter { i -> i.isAvailable } }
                .onFailure { e -> _itemsError.value = e.message ?: "Failed to load items." }
            _itemsLoading.value = false
        }
    }

    fun addToCart(itemId: Int) {
        _cart.value = _cart.value.toMutableMap().apply { this[itemId] = (this[itemId] ?: 0) + 1 }
    }

    fun removeFromCart(itemId: Int) {
        _cart.value = _cart.value.toMutableMap().apply {
            val qty = (this[itemId] ?: 0) - 1
            if (qty <= 0) remove(itemId) else this[itemId] = qty
        }
    }

    fun clearCart() { _cart.value = emptyMap() }

    fun cartCount(): Int = _cart.value.values.sum()

    fun cartTotal(): Double = _cart.value.entries.sumOf { (itemId, qty) ->
        (_items.value.find { it.id == itemId }?.price ?: 0.0) * qty
    }

    fun placeOrder(onSuccess: (OrderDto) -> Unit) {
        val cartSnapshot = _cart.value
        if (cartSnapshot.isEmpty()) return
        viewModelScope.launch {
            _placingOrder.value = true
            _orderError.value = null
            repo.createOrder(cartSnapshot)
                .onSuccess { order ->
                    _lastOrder.value = order
                    _cart.value = emptyMap()
                    onSuccess(order)
                }
                .onFailure { e -> _orderError.value = e.message ?: "Failed to place order." }
            _placingOrder.value = false
        }
    }

    fun loadOrder(orderId: Int) {
        viewModelScope.launch {
            repo.getOrder(orderId).onSuccess { _lastOrder.value = it }
        }
    }

    fun submitPayment(orderId: Int, transactionId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _placingOrder.value = true
            _orderError.value = null
            repo.submitPayment(orderId, transactionId)
                .onSuccess { _lastOrder.value = it; onSuccess() }
                .onFailure { e -> _orderError.value = e.message ?: "Failed to submit payment." }
            _placingOrder.value = false
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _ordersLoading.value = true
            repo.getMyOrders().onSuccess { _orders.value = it }
            _ordersLoading.value = false
        }
    }

    fun clearOrderError() { _orderError.value = null }
}

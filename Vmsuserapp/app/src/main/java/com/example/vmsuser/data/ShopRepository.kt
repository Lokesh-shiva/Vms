package com.example.vmsuser.data

import android.util.Log
import com.example.vmsuser.models.CreateOrderRequest
import com.example.vmsuser.models.OrderDto
import com.example.vmsuser.models.OrderItemRequest
import com.example.vmsuser.models.ShopItem
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.toUserMessage
import retrofit2.HttpException

class ShopRepository {
    private val api = RetrofitClient.api

    suspend fun getItems(cartTypeId: Int? = null): Result<List<ShopItem>> = try {
        val res = api.getShopItems(cartTypeId)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to load items."))
    } catch (e: HttpException) {
        Log.e("ShopRepo", "getItems", e)
        Result.failure(Exception(e.toUserMessage("Failed to load items.")))
    } catch (e: Exception) {
        Log.e("ShopRepo", "getItems", e)
        Result.failure(Exception(e.message ?: "Failed to load items."))
    }

    suspend fun createOrder(cart: Map<Int, Int>): Result<OrderDto> = try {
        val body = CreateOrderRequest(cart.map { (itemId, qty) -> OrderItemRequest(itemId, qty) })
        val res = api.createOrder(body)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to create order."))
    } catch (e: HttpException) {
        Log.e("ShopRepo", "createOrder", e)
        Result.failure(Exception(e.toUserMessage("Failed to create order.")))
    } catch (e: Exception) {
        Log.e("ShopRepo", "createOrder", e)
        Result.failure(Exception(e.message ?: "Failed to create order."))
    }

    suspend fun getMyOrders(): Result<List<OrderDto>> = try {
        val res = api.getMyOrders()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to load orders."))
    } catch (e: HttpException) {
        Log.e("ShopRepo", "getMyOrders", e)
        Result.failure(Exception(e.toUserMessage("Failed to load orders.")))
    } catch (e: Exception) {
        Log.e("ShopRepo", "getMyOrders", e)
        Result.failure(Exception(e.message ?: "Failed to load orders."))
    }

    suspend fun getOrder(orderId: Int): Result<OrderDto> = try {
        val res = api.getOrder(orderId)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Order not found."))
    } catch (e: HttpException) {
        Log.e("ShopRepo", "getOrder", e)
        Result.failure(Exception(e.toUserMessage("Order not found.")))
    } catch (e: Exception) {
        Log.e("ShopRepo", "getOrder", e)
        Result.failure(Exception(e.message ?: "Order not found."))
    }

    suspend fun submitPayment(orderId: Int, transactionId: String): Result<OrderDto> = try {
        val res = api.submitOrderPayment(orderId, mapOf("transaction_id" to transactionId))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed to submit payment."))
    } catch (e: HttpException) {
        Log.e("ShopRepo", "submitPayment", e)
        Result.failure(Exception(e.toUserMessage("Failed to submit payment.")))
    } catch (e: Exception) {
        Log.e("ShopRepo", "submitPayment", e)
        Result.failure(Exception(e.message ?: "Failed to submit payment."))
    }
}

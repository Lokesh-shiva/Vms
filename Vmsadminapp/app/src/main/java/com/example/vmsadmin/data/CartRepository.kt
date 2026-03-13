package com.example.vmsadmin.data

import com.example.vmsadmin.models.Cart
import com.example.vmsadmin.models.CreateCartRequest
import com.example.vmsadmin.models.UpdateCartRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class CartRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getCarts(): List<Cart> {
        val response = apiService.getCarts()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch carts")
    }

    suspend fun getCartById(id: Int): Cart {
        val response = apiService.getCartById(id)
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Cart not found")
    }

    suspend fun createCart(
        label: String,
        regionId: Int,
        cartTypeId: Int
    ): Cart {
        try {
            val response = apiService.createCart(
                CreateCartRequest(
                    label = label,
                    region_id = regionId,
                    cart_type_id = cartTypeId,
                    is_active = true
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to create cart")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create cart")
        }
    }

    suspend fun updateCart(
        id: Int,
        label: String? = null,
        regionId: Int? = null,
        cartTypeId: Int? = null,
        isActive: Boolean? = null
    ): Cart {
        try {
            val response = apiService.updateCart(
                id = id,
                request = UpdateCartRequest(
                    label = label,
                    region_id = regionId,
                    cart_type_id = cartTypeId,
                    is_active = isActive
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to update cart")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update cart")
        }
    }

    suspend fun toggleCartActive(id: Int, isActive: Boolean) {
        try {
            val response = apiService.updateCart(
                id = id,
                request = UpdateCartRequest(
                    is_active = isActive
                )
            )
            if (!response.success) {
                throw Exception(response.message ?: "Failed to toggle cart status")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to toggle cart status")
        }
    }

    suspend fun deleteCart(id: Int) {
        try {
            val response = apiService.deleteCart(id)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to delete cart")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to delete cart")
        }
    }

    private fun parseErrorDetail(e: HttpException): String? {
        return try {
            val errorBody = e.response()?.errorBody()?.string() ?: return null
            val jsonObj = json.parseToJsonElement(errorBody).jsonObject
            jsonObj["detail"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }
}

package com.example.vmsadmin.data

import com.example.vmsadmin.models.CartType
import com.example.vmsadmin.models.CreateCartTypeRequest
import com.example.vmsadmin.models.UpdateCartTypeRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class CartTypeRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getCartTypes(): List<CartType> {
        val response = apiService.getCartTypes()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch cart types")
    }

    suspend fun getCartTypeById(id: Int): CartType {
        val response = apiService.getCartType(id)
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Cart type not found")
    }

    suspend fun createCartType(name: String): CartType {
        try {
            val response = apiService.createCartType(CreateCartTypeRequest(name = name))
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to create cart type")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create cart type")
        }
    }

    suspend fun updateCartType(id: Int, name: String): CartType {
        try {
            val response = apiService.updateCartType(id, UpdateCartTypeRequest(name = name))
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to update cart type")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update cart type")
        }
    }

    suspend fun toggleCartTypeActive(id: Int, isActive: Boolean) {
        try {
            val response = apiService.updateCartType(id, UpdateCartTypeRequest(is_active = isActive))
            if (!response.success) {
                throw Exception(response.message ?: "Failed to toggle cart type status")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to toggle cart type status")
        }
    }

    suspend fun deleteCartType(id: Int) {
        try {
            val response = apiService.deleteCartType(id)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to delete cart type")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to delete cart type")
        }
    }

    /**
     * Parse the "detail" field from a FastAPI HTTPException error body.
     * FastAPI returns: {"detail": "A cart type with this name already exists."}
     */
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

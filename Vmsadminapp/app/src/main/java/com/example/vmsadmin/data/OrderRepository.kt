package com.example.vmsadmin.data

import com.example.vmsadmin.models.AdminOrder
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class OrderRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getOrders(status: String? = null): List<AdminOrder> {
        try {
            val response = apiService.getAdminOrders(status)
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to fetch orders")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to fetch orders")
        }
    }

    suspend fun approveOrder(orderId: Int): AdminOrder {
        try {
            val response = apiService.approveOrder(orderId)
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to approve order")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to approve order")
        }
    }

    suspend fun rejectOrder(orderId: Int): AdminOrder {
        try {
            val response = apiService.rejectOrder(orderId)
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to reject order")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to reject order")
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

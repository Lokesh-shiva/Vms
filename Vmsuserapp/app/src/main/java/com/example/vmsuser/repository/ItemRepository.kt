package com.example.vmsuser.repository

import com.example.vmsuser.models.Item
import com.example.vmsuser.network.ApiService
import retrofit2.HttpException

class ItemRepository(private val apiService: ApiService) : BaseRepository() {

    suspend fun getItems(): List<Item> {
        return try {
            val response = apiService.getItems()
            if (response.success && response.data != null) {
                response.data
            } else {
                throw Exception(response.message ?: "Failed to fetch items")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to fetch items")
        }
    }
}

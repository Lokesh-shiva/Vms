package com.example.vmsadmin.data

import com.example.vmsadmin.models.Captain
import com.example.vmsadmin.models.CreateCaptainRequest
import com.example.vmsadmin.models.UpdateCaptainRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class CaptainRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getCaptains(): List<Captain> {
        val response = apiService.getCaptains()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch captains")
    }

    suspend fun createCaptain(userId: Int, regionId: Int?, bio: String?): Captain {
        try {
            val response = apiService.createCaptain(CreateCaptainRequest(userId, regionId, bio))
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to create captain")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create captain")
        }
    }

    suspend fun updateCaptain(id: Int, status: String? = null, regionId: Int? = null): Captain {
        try {
            val response = apiService.updateCaptain(id, UpdateCaptainRequest(status = status, region_id = regionId))
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to update captain")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update captain")
        }
    }

    suspend fun deleteCaptain(id: Int) {
        try {
            apiService.deleteCaptain(id)
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to delete captain")
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

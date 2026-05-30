package com.example.vmsadmin.data

import com.example.vmsadmin.models.CreateRegionRequest
import com.example.vmsadmin.models.Region
import com.example.vmsadmin.models.UpdateRegionRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class RegionRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getRegions(): List<Region> {
        val response = apiService.getRegions()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch regions")
    }

    suspend fun createRegion(name: String): Region {
        try {
            val response = apiService.createRegion(CreateRegionRequest(name = name))
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to create region")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create region")
        }
    }

    suspend fun updateRegion(id: Int, name: String): Region {
        try {
            val response = apiService.updateRegion(id, UpdateRegionRequest(name = name))
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to update region")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update region")
        }
    }

    suspend fun toggleRegionActive(id: Int, isServiceable: Boolean) {
        try {
            val response = apiService.updateRegion(id, UpdateRegionRequest(is_serviceable = isServiceable))
            if (!response.success) {
                throw Exception(response.message ?: "Failed to toggle region status")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to toggle region status")
        }
    }

    suspend fun deleteRegion(id: Int) {
        try {
            val response = apiService.deleteRegion(id)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to delete region")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to delete region")
        }
    }

    /**
     * Parse the "detail" field from a FastAPI HTTPException error body.
     * FastAPI returns: {"detail": "A location with this name already exists."}
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

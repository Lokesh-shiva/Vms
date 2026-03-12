package com.example.vmsadmin.data

import com.example.vmsadmin.models.CreateTimeslotRequest
import com.example.vmsadmin.models.Timeslot
import com.example.vmsadmin.models.UpdateTimeslotRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class TimeslotRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getTimeslots(): List<Timeslot> {
        val response = apiService.getTimeslots()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch timeslots")
    }

    suspend fun getTimeslotById(id: Int): Timeslot {
        val response = apiService.getTimeslot(id)
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Timeslot not found")
    }

    suspend fun createTimeslot(startTime: String, endTime: String, capacity: Int): Timeslot {
        try {
            val response = apiService.createTimeslot(
                CreateTimeslotRequest(
                    start_time = startTime,
                    end_time = endTime,
                    capacity = capacity
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to create timeslot")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create timeslot")
        }
    }

    suspend fun updateTimeslot(
        id: Int,
        startTime: String? = null,
        endTime: String? = null,
        capacity: Int? = null,
        isActive: Boolean? = null
    ): Timeslot {
        try {
            val response = apiService.updateTimeslot(
                id,
                UpdateTimeslotRequest(
                    start_time = startTime,
                    end_time = endTime,
                    capacity = capacity,
                    is_active = isActive
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to update timeslot")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update timeslot")
        }
    }

    suspend fun toggleTimeslotActive(id: Int, isActive: Boolean) {
        try {
            val response = apiService.updateTimeslot(
                id,
                UpdateTimeslotRequest(is_active = isActive)
            )
            if (!response.success) {
                throw Exception(response.message ?: "Failed to toggle timeslot status")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to toggle timeslot status")
        }
    }

    suspend fun deleteTimeslot(id: Int) {
        try {
            val response = apiService.deleteTimeslot(id)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to delete timeslot")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to delete timeslot")
        }
    }

    /**
     * Parse the "detail" field from a FastAPI HTTPException error body.
     * FastAPI returns: {"detail": "..."}
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

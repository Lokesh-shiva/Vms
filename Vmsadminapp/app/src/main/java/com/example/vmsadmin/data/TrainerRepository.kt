package com.example.vmsadmin.data

import com.example.vmsadmin.models.AdminTrainerBooking
import com.example.vmsadmin.models.CreateTrainerRequest
import com.example.vmsadmin.models.Trainer
import com.example.vmsadmin.models.UpdateTrainerRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class TrainerRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getTrainers(): List<Trainer> {
        val response = apiService.getTrainers()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch trainers")
    }

    suspend fun createTrainer(name: String, bio: String, specialties: String, rate: Double): Trainer {
        try {
            val response = apiService.createTrainer(CreateTrainerRequest(name, bio, specialties, rate))
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to create trainer")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create trainer")
        }
    }

    suspend fun updateTrainer(id: Int, name: String, bio: String, specialties: String, rate: Double): Trainer {
        try {
            val response = apiService.updateTrainer(id, UpdateTrainerRequest(name, bio, specialties, rate))
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to update trainer")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update trainer")
        }
    }

    suspend fun toggleTrainerActive(id: Int, isActive: Boolean) {
        try {
            val response = apiService.updateTrainer(id, UpdateTrainerRequest(is_active = isActive))
            if (!response.success) throw Exception(response.message ?: "Failed to toggle trainer status")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to toggle trainer status")
        }
    }

    suspend fun deleteTrainer(id: Int) {
        try {
            val response = apiService.deleteTrainer(id)
            if (!response.success) throw Exception(response.message ?: "Failed to delete trainer")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to delete trainer")
        }
    }

    suspend fun getBookings(status: String? = null): List<AdminTrainerBooking> {
        try {
            val response = apiService.getAdminTrainerBookings(status)
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to fetch bookings")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to fetch bookings")
        }
    }

    suspend fun approveBooking(id: Int): AdminTrainerBooking {
        try {
            val response = apiService.approveTrainerBooking(id)
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to approve booking")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to approve booking")
        }
    }

    suspend fun rejectBooking(id: Int): AdminTrainerBooking {
        try {
            val response = apiService.rejectTrainerBooking(id)
            if (response.success && response.data != null) return response.data
            throw Exception(response.message ?: "Failed to reject booking")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to reject booking")
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

package com.example.vmsadmin.data

import com.example.vmsadmin.models.CreateFeeConfigRequest
import com.example.vmsadmin.models.FeeConfig
import com.example.vmsadmin.models.UpdateFeeConfigRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class FeeConfigRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getFeeConfigs(): List<FeeConfig> {
        val response = apiService.getFeeConfigs()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch fee configs")
    }

    suspend fun createFeeConfig(
        regionId: Int,
        cartTypeId: Int,
        bookingFee: Double,
        cancellationFeePct: Double,
        platformFeePct: Double,
        matchingFee: Double = 0.0,
        ratePerBlock: Double = 0.0,
        blockDurationMinutes: Int = 45,
        maxDurationMinutes: Int = 180
    ): FeeConfig {
        try {
            val response = apiService.createFeeConfig(
                CreateFeeConfigRequest(
                    region_id = regionId,
                    cart_type_id = cartTypeId,
                    booking_fee = bookingFee,
                    cancellation_fee_pct = cancellationFeePct,
                    platform_fee_pct = platformFeePct,
                    matching_fee = matchingFee,
                    rate_per_block = ratePerBlock,
                    block_duration_minutes = blockDurationMinutes,
                    max_duration_minutes = maxDurationMinutes
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to create fee config")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create fee config")
        }
    }

    suspend fun updateFeeConfig(
        id: Int,
        bookingFee: Double? = null,
        cancellationFeePct: Double? = null,
        platformFeePct: Double? = null,
        isActive: Boolean? = null,
        matchingFee: Double? = null,
        ratePerBlock: Double? = null,
        blockDurationMinutes: Int? = null,
        maxDurationMinutes: Int? = null,
        surgeEnabled: Boolean? = null,
        surgeMultiplier: Double? = null
    ): FeeConfig {
        try {
            val response = apiService.updateFeeConfig(
                id = id,
                request = UpdateFeeConfigRequest(
                    booking_fee = bookingFee,
                    cancellation_fee_pct = cancellationFeePct,
                    platform_fee_pct = platformFeePct,
                    is_active = isActive,
                    matching_fee = matchingFee,
                    rate_per_block = ratePerBlock,
                    block_duration_minutes = blockDurationMinutes,
                    max_duration_minutes = maxDurationMinutes,
                    surge_enabled = surgeEnabled,
                    surge_multiplier = surgeMultiplier
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to update fee config")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update fee config")
        }
    }

    suspend fun deleteFeeConfig(id: Int) {
        try {
            val response = apiService.deleteFeeConfig(id)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to delete fee config")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to delete fee config")
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

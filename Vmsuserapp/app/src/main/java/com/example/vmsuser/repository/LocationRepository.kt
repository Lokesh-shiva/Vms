package com.example.vmsuser.repository

import com.example.vmsuser.models.CartType
import com.example.vmsuser.models.Region
import com.example.vmsuser.models.Timeslot
import com.example.vmsuser.network.ApiService

class LocationRepository(private val apiService: ApiService) : BaseRepository() {

    suspend fun getRegions(): List<Region> {
        val response = apiService.getRegions()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch regions")
    }

    suspend fun getCartTypes(): List<CartType> {
        val response = apiService.getCartTypes()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch cart types")
    }

    suspend fun getTimeslots(): List<Timeslot> {
        val response = apiService.getTimeslots()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch timeslots")
    }
}

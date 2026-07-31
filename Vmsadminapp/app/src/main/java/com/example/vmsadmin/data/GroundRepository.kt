package com.example.vmsadmin.data

import android.content.ContentResolver
import android.net.Uri
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.models.UpdateGroundRequest
import com.example.vmsadmin.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

class GroundRepository(private val apiService: ApiService) {

    suspend fun getGrounds(): List<Ground> {
        val response = apiService.getGrounds()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch grounds")
    }

    suspend fun toggleGround(id: Int, isActive: Boolean): Ground {
        val response = apiService.updateGround(id, UpdateGroundRequest(is_active = isActive))
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to update ground")
    }

    suspend fun assignOwner(id: Int, ownerUserId: Int): Ground {
        val response = apiService.updateGround(id, UpdateGroundRequest(owner_user_id = ownerUserId))
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to assign owner")
    }

    suspend fun uploadGroundImage(contentResolver: ContentResolver, groundId: Int, imageUri: Uri): Ground {
        val tempFile = File.createTempFile("ground_", ".jpg")
        contentResolver.openInputStream(imageUri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        val filePart = MultipartBody.Part.createFormData(
            "file", tempFile.name, tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        val response = apiService.uploadGroundImage(groundId, filePart)
        tempFile.delete()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to upload ground image")
    }

    suspend fun searchUserByPhone(phone: String): AppUser? {
        return try {
            val response = apiService.searchUserByPhone(phone)
            if (response.success) response.data else null
        } catch (e: HttpException) {
            if (e.code() == 404) null
            else throw Exception("Search failed")
        }
    }
}

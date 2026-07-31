package com.example.vmsadmin.data

import android.content.ContentResolver
import android.net.Uri
import com.example.vmsadmin.models.CreateItemRequest
import com.example.vmsadmin.models.Item
import com.example.vmsadmin.models.UpdateItemRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

class ItemRepository(private val apiService: ApiService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getItems(): List<Item> {
        val response = apiService.getItems()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch items")
    }

    suspend fun createItem(
        name: String,
        price: Double,
        cartTypeId: Int,
        description: String? = null,
        imageUrl: String? = null
    ): Item {
        try {
            val response = apiService.createItem(
                CreateItemRequest(
                    name = name,
                    price = price,
                    cart_type_id = cartTypeId,
                    description = description,
                    image_url = imageUrl
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to create item")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to create item")
        }
    }

    suspend fun updateItem(
        id: Int,
        name: String? = null,
        price: Double? = null,
        cartTypeId: Int? = null,
        description: String? = null,
        imageUrl: String? = null
    ): Item {
        try {
            val response = apiService.updateItem(
                itemId = id,
                request = UpdateItemRequest(
                    name = name,
                    price = price,
                    cart_type_id = cartTypeId,
                    description = description,
                    image_url = imageUrl
                )
            )
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to update item")
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to update item")
        }
    }

    suspend fun toggleItem(id: Int, isAvailable: Boolean) {
        try {
            val response = apiService.updateItem(
                itemId = id,
                request = UpdateItemRequest(is_available = isAvailable)
            )
            if (!response.success) {
                throw Exception(response.message ?: "Failed to toggle item status")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to toggle item status")
        }
    }

    suspend fun uploadItemImage(contentResolver: ContentResolver, id: Int, imageUri: Uri): Item {
        val tempFile = File.createTempFile("item_", ".jpg")
        contentResolver.openInputStream(imageUri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        val filePart = MultipartBody.Part.createFormData(
            "file", tempFile.name, tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        try {
            val response = apiService.uploadItemImage(id, filePart)
            tempFile.delete()
            if (response.success && response.data != null) {
                return response.data
            }
            throw Exception(response.message ?: "Failed to upload item image")
        } catch (e: HttpException) {
            tempFile.delete()
            throw Exception(parseErrorDetail(e) ?: "Failed to upload item image")
        }
    }

    suspend fun deleteItem(id: Int) {
        try {
            val response = apiService.deleteItem(id)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to delete item")
            }
        } catch (e: HttpException) {
            throw Exception(parseErrorDetail(e) ?: "Failed to delete item")
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

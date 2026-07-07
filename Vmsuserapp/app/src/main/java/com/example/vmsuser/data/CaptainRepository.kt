package com.example.vmsuser.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.example.vmsuser.models.CaptainApplication
import com.example.vmsuser.models.CaptainStats
import com.example.vmsuser.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CaptainRepository {
    private val api = RetrofitClient.api

    suspend fun getStats(): Result<CaptainStats> = try {
        val res = api.getCaptainStats()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("CaptainRepo", "getStats", e); Result.failure(e) }

    suspend fun apply(bio: String): Result<CaptainApplication> = try {
        val res = api.applyCaptain(mapOf("bio" to bio))
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("CaptainRepo", "apply", e); Result.failure(e) }

    suspend fun uploadKyc(contentResolver: ContentResolver, documentType: String, imageUri: Uri): Result<CaptainApplication> = try {
        val tempFile = File.createTempFile("kyc_", ".jpg")
        contentResolver.openInputStream(imageUri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        val filePart = MultipartBody.Part.createFormData(
            "file", tempFile.name, tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        val typePart = documentType.toRequestBody("text/plain".toMediaTypeOrNull())
        val res = api.uploadCaptainKyc(typePart, filePart)
        tempFile.delete()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("CaptainRepo", "uploadKyc", e); Result.failure(e) }

    suspend fun getApplicationStatus(): Result<CaptainApplication> = try {
        val res = api.getCaptainApplicationStatus()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("CaptainRepo", "getApplicationStatus", e); Result.failure(e) }

    suspend fun updatePayoutUpi(upiId: String): Result<Unit> = try {
        val res = api.updatePayoutUpi(mapOf("upi_id" to upiId))
        if (res.success) Result.success(Unit) else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("CaptainRepo", "updatePayoutUpi", e); Result.failure(e) }
}

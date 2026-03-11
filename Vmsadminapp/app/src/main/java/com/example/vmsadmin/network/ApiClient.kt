package com.example.vmsadmin.network

import android.util.Log
import com.example.vmsadmin.data.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiClient {
    // private const val BASE_URL = "http://10.0.2.2:8000" // Emulator
    // private const val BASE_URL = "http://192.168.1.4:8000"
    // private const val BASE_URL = "http://192.168.0.105:8000"
    // private const val BASE_URL = "http://192.168.1.7:8000"
    private const val BASE_URL = "http://192.168.1.3:8000"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val _logoutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    fun create(tokenManager: TokenManager): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(tokenManager)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 401 && !chain.request().url.encodedPath.contains("/auth/login")) {
                    Log.e("AUTH", "401 Unauthorized — forcing logout")
                    CoroutineScope(Dispatchers.IO).launch {
                        tokenManager.clearToken()
                        _logoutEvent.emit(Unit)
                    }
                }
                response
            }
            .build()

        val contentType = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}

package com.example.vmsadmin.network

import android.util.Log
import com.example.vmsadmin.data.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            tokenManager.tokenFlow.firstOrNull()
        }

        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d("AUTH", "Attaching token: ${token.take(10)}...")
        } else {
            Log.e("AUTH", "No JWT token found")
        }

        return chain.proceed(requestBuilder.build())
    }
}

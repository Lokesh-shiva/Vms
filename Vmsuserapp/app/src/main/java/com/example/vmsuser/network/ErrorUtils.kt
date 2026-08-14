package com.example.vmsuser.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Retrofit throws HttpException for any non-2xx response before the body
 * converter ever runs, so `res.message` in a repo's success/failure branch
 * is never reached for a 4xx/5xx — only this exception's generic
 * "HTTP 400 Bad Request" is visible unless the error body is parsed
 * explicitly. The backend's error shape is usually {"detail": "..."},
 * but some routes (e.g. schema validators) raise {"detail": [...]}, a
 * list of per-field messages — handle both.
 */
fun HttpException.backendDetail(): String? = try {
    val errorBody = response()?.errorBody()?.string() ?: return null
    when (val detail = errorJson.parseToJsonElement(errorBody).jsonObject["detail"]) {
        is JsonArray -> detail.mapNotNull { (it as? JsonPrimitive)?.content }
            .joinToString(" ").ifBlank { null }
        else -> detail?.jsonPrimitive?.content
    }
} catch (_: Exception) {
    null
}

/** Prefer the backend's detail message over Retrofit's generic exception text. */
fun Exception.toUserMessage(fallback: String): String =
    if (this is HttpException) backendDetail() ?: fallback else message ?: fallback

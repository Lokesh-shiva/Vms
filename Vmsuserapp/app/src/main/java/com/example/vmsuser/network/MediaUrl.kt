package com.example.vmsuser.network

import com.example.vmsuser.BuildConfig

/** Backend media routes (profile photos, etc.) return a relative path — resolve it against
 * the configured API host so AsyncImage/Coil can load it directly. */
fun absoluteMediaUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return BuildConfig.BASE_URL.trimEnd('/') + path
}

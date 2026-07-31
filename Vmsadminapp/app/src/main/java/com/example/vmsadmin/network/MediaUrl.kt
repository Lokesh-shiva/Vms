package com.example.vmsadmin.network

/** Backend media routes (ground/sport/item images, etc.) return a relative path — resolve
 * it against the API host so AsyncImage/Coil can load it directly. */
fun absoluteMediaUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return ApiClient.BASE_URL.trimEnd('/') + path
}

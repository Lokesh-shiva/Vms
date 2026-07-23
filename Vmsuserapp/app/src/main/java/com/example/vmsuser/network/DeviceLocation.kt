package com.example.vmsuser.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager

/**
 * Best-effort last-known GPS/network fix, no Play Services dependency.
 * Returns null if location is off, no provider has a cached fix yet, or permission is missing.
 */
@SuppressLint("MissingPermission")
fun lastKnownLocation(context: Context): Pair<Double, Double>? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    for (provider in providers) {
        try {
            if (!manager.isProviderEnabled(provider)) continue
            val location = manager.getLastKnownLocation(provider) ?: continue
            return location.latitude to location.longitude
        } catch (_: Exception) {
            continue
        }
    }
    return null
}

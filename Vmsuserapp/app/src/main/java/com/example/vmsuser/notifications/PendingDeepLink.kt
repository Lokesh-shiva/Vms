package com.example.vmsuser.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Holds a navigation route captured from a notification tap (cold-start or
 * background) until AppNavigation is ready to consume it. Compose-observable
 * so a read inside a @Composable recomposes when it's set from MainActivity.
 */
object PendingDeepLink {
    var route: String? by mutableStateOf(null)
}

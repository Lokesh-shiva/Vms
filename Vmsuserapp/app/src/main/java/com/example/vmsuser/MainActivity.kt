package com.example.vmsuser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.vmsuser.navigation.AppNavigation
import com.example.vmsuser.network.EXTRA_NOTIF_ROUTE
import com.example.vmsuser.notifications.PendingDeepLink
import com.example.vmsuser.ui.theme.PlixoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        consumeNotificationIntent(intent)
        setContent {
            PlixoTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNotificationIntent(intent)
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_NOTIF_ROUTE)?.let { route ->
            PendingDeepLink.route = route
        }
    }
}

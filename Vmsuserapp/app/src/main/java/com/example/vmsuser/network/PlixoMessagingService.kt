package com.example.vmsuser.network

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.vmsuser.MainActivity
import com.example.vmsuser.PLIXO_NOTIFICATION_CHANNEL_ID
import com.example.vmsuser.R
import com.example.vmsuser.notifications.notificationDeepLinkRoute
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val EXTRA_NOTIF_ROUTE = "notif_route"

class PlixoMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Message received: ${message.data}")

        val title = message.notification?.title ?: message.data["title"] ?: "Plixo"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: "GENERAL"
        val matchId = message.data["match_id"]?.toIntOrNull()
        val route = notificationDeepLinkRoute(type, matchId)

        showNotification(title, body, route)
    }

    private fun showNotification(title: String, body: String, route: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIF_ROUTE, route)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, PLIXO_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("FCM", "POST_NOTIFICATIONS not granted — skipping system notification")
            return
        }
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "New token: $token")
        registerFcmToken(token)
    }
}

/** Push the current device token to the backend. Safe to call repeatedly (idempotent upsert). */
fun registerFcmToken(token: String) {
    if (!UserSession.isLoggedIn) return
    CoroutineScope(Dispatchers.IO).launch {
        try {
            RetrofitClient.api.updateFcmToken(mapOf("token" to token, "platform" to "android"))
        } catch (e: Exception) {
            Log.e("FCM", "registerFcmToken failed", e)
        }
    }
}

package com.example.vmsuser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.example.vmsuser.network.RetrofitClient

const val PLIXO_NOTIFICATION_CHANNEL_ID = "plixo_default"

class PlixoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        RetrofitClient.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            PLIXO_NOTIFICATION_CHANNEL_ID,
            "Plixo updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Match, captain, and session updates"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}

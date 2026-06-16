package com.example.vmsuser

import android.app.Application
import com.google.firebase.FirebaseApp
import com.example.vmsuser.network.RetrofitClient

class PlixoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        RetrofitClient.init(this)
    }
}

package com.example.vmsuser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.vmsuser.navigation.AppNavigation
import com.example.vmsuser.ui.theme.PlixoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PlixoTheme {
                AppNavigation()
            }
        }
    }
}

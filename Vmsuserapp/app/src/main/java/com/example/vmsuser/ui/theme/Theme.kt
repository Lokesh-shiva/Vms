package com.example.vmsuser.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PlixoColorScheme = lightColorScheme(
    primary = PlixoPrimary,
    onPrimary = PlixoSurface,
    primaryContainer = PlixoPrimaryLight,
    onPrimaryContainer = PlixoPrimaryDark,
    secondary = PlixoInk,
    onSecondary = PlixoSurface,
    background = PlixoBg,
    onBackground = PlixoText,
    surface = PlixoSurface,
    onSurface = PlixoText,
    surfaceVariant = PlixoSurface2,
    onSurfaceVariant = PlixoText2,
    outline = PlixoBorder,
    error = PlixoDanger,
    onError = PlixoSurface,
    errorContainer = PlixoDangerLight,
)

@Composable
fun PlixoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PlixoBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = PlixoColorScheme,
        typography = PlixoTypography,
        content = content,
    )
}

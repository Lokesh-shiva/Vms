package com.example.vmsuser.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.data.AuthRepository
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.AuthTokenManager
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(80)
        visible = true

        val tokenManager = AuthTokenManager(context)
        val token = tokenManager.getToken()

        // Kick off the session-restore call in parallel with the minimum branding
        // delay below, instead of after it — avoids adding the two waits together
        // (matters most on a cold backend, where getMe() alone can take seconds).
        val getMeDeferred = if (token != null) async { AuthRepository().getMe() } else null
        delay(1800)

        val destination = if (token != null) {
            val result = getMeDeferred!!.await()
            if (result.isSuccess) {
                UserSession.setUser(result.getOrNull())
                if (result.getOrNull()?.isProfileComplete == true) {
                    Screen.Home.route
                } else {
                    Screen.ProfileSetup.route
                }
            } else {
                // Token expired or invalid — clear it and send to login
                tokenManager.clearToken()
                Screen.Phone.route
            }
        } else {
            Screen.Phone.route
        }

        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "splashScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "splashAlpha",
    )

    Box(
        modifier = Modifier.fillMaxSize().background(PlixoInk),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .size(240.dp)
                .clip(RoundedCornerShape(50))
                .background(PlixoPrimary.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = (-60).dp)
                .size(180.dp)
                .clip(RoundedCornerShape(50))
                .background(PlixoLime.copy(alpha = 0.18f))
        )

        Column(
            modifier = Modifier.scale(scale).alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(PlixoLime),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.FlashOn, null, tint = PlixoLimeFg, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "Plixo",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 52.sp,
                color = Color.White,
                letterSpacing = (-2.5).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "GET PLAYING",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            repeat(3) { i ->
                val infiniteTransition = rememberInfiniteTransition(label = "dot$i")
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(700, delayMillis = i * 200),
                        RepeatMode.Reverse,
                    ),
                    label = "dotAlpha$i",
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PlixoLime.copy(alpha = dotAlpha))
                )
            }
        }
    }
}

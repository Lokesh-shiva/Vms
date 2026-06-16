package com.example.vmsuser.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.data.AuthRepository
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.AuthTokenManager
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.ui.components.PlixoButton
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OtpScreen(navController: NavController, phone: String) {
    val context = LocalContext.current
    var otp by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(48) }
    var verifying by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository() }
    val tokenManager = remember { AuthTokenManager(context) }

    LaunchedEffect(Unit) {
        while (seconds > 0) { delay(1000); seconds-- }
    }

    fun verify() {
        if (otp.length != 6 || verifying) return
        verifying = true
        errorMsg = null
        scope.launch {
            repo.verifyOtp(phone, otp)
                .onSuccess { result ->
                    tokenManager.saveToken(result.token)
                    // Fetch full profile and seed UserSession
                    repo.getMe().onSuccess { user -> UserSession.setUser(user) }

                    val dest = if (result.isNewUser) Screen.ProfileSetup.route else Screen.Home.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Phone.route) { inclusive = true }
                    }
                }
                .onFailure { e ->
                    errorMsg = e.message ?: "Invalid OTP. Please try again."
                    otp = ""
                    verifying = false
                }
        }
    }

    // Auto-verify when all 6 digits entered
    LaunchedEffect(otp) { if (otp.length == 6) verify() }

    Column(modifier = Modifier.fillMaxSize().background(PlixoSurface).statusBarsPadding()) {
        PlixoTopBar(title = "", onBack = { navController.popBackStack() })

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier.size(60.dp).background(PlixoPrimaryLight, PlixoShape.Icon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, null, tint = PlixoPrimary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Verify your number",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = PlixoText,
                letterSpacing = (-0.8).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Code sent to $phone",
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                color = PlixoText2,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(30.dp))

            BasicTextField(
                value = otp,
                onValueChange = {
                    if (it.all { c -> c.isDigit() } && it.length <= 6 && !verifying) {
                        otp = it
                        errorMsg = null
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                decorationBox = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        repeat(6) { i ->
                            val digit = otp.getOrNull(i)?.toString() ?: ""
                            val isFilled = digit.isNotEmpty()
                            val isError = errorMsg != null
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 60.dp)
                                    .background(
                                        when {
                                            isError -> PlixoDangerLight
                                            isFilled -> PlixoPrimaryLight
                                            else -> PlixoSurface2
                                        },
                                        RoundedCornerShape(16.dp),
                                    )
                                    .border(
                                        2.dp,
                                        when {
                                            isError -> PlixoDanger
                                            isFilled -> PlixoPrimary
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(digit, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = PlixoText)
                            }
                        }
                    }
                },
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(12.dp))
                Text(errorMsg!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
            }

            Spacer(Modifier.height(30.dp))

            PlixoButton(
                label = if (verifying) "Verifying…" else "Verify & continue",
                onClick = { verify() },
                enabled = otp.length == 6 && !verifying,
            )
            Spacer(Modifier.height(22.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (seconds > 0) {
                    Text("Resend code in ${seconds}s", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
                } else {
                    TextButton(onClick = {
                        seconds = 48
                        otp = ""
                        errorMsg = null
                        scope.launch { repo.sendOtp(phone) }
                    }) {
                        Text("Resend OTP", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PlixoPrimary)
                    }
                }
            }
        }
    }
}

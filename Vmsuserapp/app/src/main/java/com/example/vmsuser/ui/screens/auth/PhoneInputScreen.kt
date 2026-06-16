package com.example.vmsuser.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.data.AuthRepository
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.PlixoButton
import com.example.vmsuser.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PhoneInputScreen(navController: NavController) {
    var phone by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository() }

    Box(modifier = Modifier.fillMaxSize().background(PlixoSurface)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Hero
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=900&q=80",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color(0x40141228), Color.Transparent, PlixoSurface),
                            startY = 0f, endY = Float.POSITIVE_INFINITY,
                        )
                    )
                )
                Row(
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(PlixoLime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.FlashOn, null, tint = PlixoLimeFg, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Plixo",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.White,
                        letterSpacing = (-1).sp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)
                    .offset(y = (-30).dp),
            ) {
                Text(
                    "Find your\nnext match",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = PlixoText,
                    letterSpacing = (-1).sp,
                    lineHeight = 34.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enter your mobile number and we'll send a one-time password to verify.",
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.5.sp,
                    color = PlixoText2,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(26.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it.filter { c -> c.isDigit() }.take(10)
                        errorMsg = null
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = PlixoShape.Input,
                    isError = errorMsg != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        errorBorderColor = PlixoDanger,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    prefix = {
                        Text("+91  ", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = PlixoText)
                    },
                    placeholder = {
                        Text("98765 43210", color = PlixoText3, fontFamily = PlusJakartaSans, fontSize = 15.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMsg!!, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoDanger)
                }

                Spacer(Modifier.height(16.dp))

                PlixoButton(
                    label = if (loading) "Sending OTP…" else "Send OTP",
                    onClick = {
                        if (phone.length == 10 && !loading) {
                            loading = true
                            errorMsg = null
                            scope.launch {
                                val fullPhone = "+91$phone"
                                repo.sendOtp(fullPhone)
                                    .onSuccess {
                                        navController.navigate(Screen.Otp.create(fullPhone))
                                    }
                                    .onFailure { e ->
                                        errorMsg = e.message ?: "Could not send OTP. Try again."
                                        loading = false
                                    }
                            }
                        }
                    },
                    enabled = phone.length == 10 && !loading,
                )
            }
        }
    }
}

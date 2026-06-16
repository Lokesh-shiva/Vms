package com.example.vmsuser.ui.screens.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*

@Composable
fun KycSubmittedScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize().background(PlixoInk),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier.size(96.dp).background(PlixoLime, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = PlixoLimeFg, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Application\nSubmitted!",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp,
                lineHeight = 36.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "We've received your documents and application. Our team will review within 24–48 hours.",
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                color = Color.White.copy(0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFF3CD), PlixoShape.Pill)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.HourglassTop, null, tint = Color(0xFF856404), modifier = Modifier.size(16.dp))
                    Text(
                        "PENDING REVIEW",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF856404),
                        letterSpacing = 1.sp,
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            PlixoButton(
                "Back to home",
                onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                variant = PlixoButtonVariant.Lime,
            )
            Spacer(Modifier.height(12.dp))
            PlixoButton(
                "Check status",
                onClick = { navController.navigate(Screen.KycStatus.route) },
                variant = PlixoButtonVariant.Ghost,
            )
        }
    }
}

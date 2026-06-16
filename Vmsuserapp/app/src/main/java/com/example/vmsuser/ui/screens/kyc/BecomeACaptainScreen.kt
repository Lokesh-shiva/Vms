package com.example.vmsuser.ui.screens.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*

@Composable
fun BecomeACaptainScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(PlixoInk).statusBarsPadding()) {
        PlixoTopBar(title = "", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Box(
                modifier = Modifier.size(72.dp).background(PlixoLime, PlixoShape.Icon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, null, tint = PlixoLimeFg, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Become a\nPlixo Captain",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                color = Color.White,
                letterSpacing = (-1).sp,
                lineHeight = 38.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Lead matches, build your reputation, and earn money doing what you love.",
                fontFamily = PlusJakartaSans,
                fontSize = 15.sp,
                color = Color.White.copy(0.65f),
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(28.dp))

            Text(
                "What you'll do",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(14.dp))
            listOf(
                Icons.Filled.SportsScore to "Organize & lead sports matches",
                Icons.Filled.Group to "Build and manage your player community",
                Icons.Filled.CurrencyRupee to "Earn ₹400–₹2000 per match",
                Icons.Filled.Star to "Build your captain rating & reputation",
            ).forEach { (icon, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color.White.copy(0.08f), PlixoShape.SmCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = PlixoLime, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(text, fontFamily = PlusJakartaSans, fontSize = 14.sp, color = Color.White.copy(0.8f))
                }
            }
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.06f), PlixoShape.Card)
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        "Requirements",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "Valid government ID",
                        "Clear selfie photo",
                        "Address proof",
                        "Minimum 3 matches played on Plixo",
                    ).forEach { req ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = PlixoLime, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(req, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = Color.White.copy(0.75f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            PlixoButton(
                "Apply to be a Captain",
                onClick = { navController.navigate(Screen.CaptainApplication.route) },
                variant = PlixoButtonVariant.Lime,
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}

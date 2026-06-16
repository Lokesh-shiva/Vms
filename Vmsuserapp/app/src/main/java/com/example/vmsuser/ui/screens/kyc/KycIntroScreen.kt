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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*

@Composable
fun KycIntroScreen(navController: NavController) {
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
                modifier = Modifier.size(64.dp).background(PlixoPrimaryLight, PlixoShape.Icon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.VerifiedUser, null, tint = PlixoPrimary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Identity\nVerification",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = Color.White,
                letterSpacing = (-0.8).sp,
                lineHeight = 34.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "To protect our community, all captains must verify their identity before leading matches.",
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                color = Color.White.copy(0.65f),
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.06f), PlixoShape.Card)
                    .padding(16.dp),
            ) {
                Text(
                    "You'll need",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(14.dp))
                listOf(
                    Icons.Filled.Badge to "Government ID (Aadhaar, PAN, or Passport)",
                    Icons.Filled.CameraAlt to "Clear selfie photo",
                    Icons.Filled.Home to "Address proof",
                ).forEach { (icon, text) ->
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(Color.White.copy(0.08f), PlixoShape.SmCard),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, null, tint = PlixoLime, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text,
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp,
                            color = Color.White.copy(0.8f),
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.background(BlockSkyBg, PlixoShape.Pill).padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Schedule, null, tint = BlockSkyFg, modifier = Modifier.size(16.dp))
                    Text(
                        "Review takes 24–48 hours",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = BlockSkyFg,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            PlixoButton(
                "Start verification",
                onClick = { navController.navigate(Screen.KycUpload.route) },
                variant = PlixoButtonVariant.Lime,
            )
        }
    }
}

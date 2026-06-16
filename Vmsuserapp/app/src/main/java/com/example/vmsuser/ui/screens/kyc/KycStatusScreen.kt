package com.example.vmsuser.ui.screens.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*

@Composable
fun KycStatusScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(PlixoInk).statusBarsPadding()) {
        PlixoTopBar(title = "Verification Status", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.06f), PlixoShape.Card)
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(Color(0xFFFFF3CD), PlixoShape.Icon),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.HourglassTop, null, tint = Color(0xFF856404), modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Under Review",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                        )
                        Text(
                            "Our team is reviewing your documents",
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.sp,
                            color = Color.White.copy(0.6f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "Verification steps",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            TimelineStep("Application submitted", "Received and queued", true, Icons.Filled.CheckCircle)
            TimelineStep("Documents under review", "~24-48 hours", true, Icons.Filled.HourglassTop)
            TimelineStep("Identity verified", "Pending", false, Icons.Filled.VerifiedUser)
            TimelineStep("Captain activated", "Pending", false, Icons.Filled.Star)
            Spacer(Modifier.height(32.dp))
            PlixoButton(
                "Back to profile",
                onClick = { navController.popBackStack() },
                variant = PlixoButtonVariant.Ghost,
            )
        }
    }
}

@Composable
private fun TimelineStep(title: String, subtitle: String, done: Boolean, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (done) PlixoLime else Color.White.copy(0.08f), PlixoShape.Icon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (done) PlixoLimeFg else Color.White.copy(0.4f),
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(if (done) PlixoLime.copy(0.3f) else Color.White.copy(0.1f)),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
            Text(
                title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (done) Color.White else Color.White.copy(0.4f),
            )
            Text(
                subtitle,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                color = if (done) Color.White.copy(0.6f) else Color.White.copy(0.3f),
            )
        }
    }
}

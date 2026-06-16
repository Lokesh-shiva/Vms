package com.example.vmsuser.ui.screens.captain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.CaptainViewModel

@Composable
fun CaptainEarningsScreen(navController: NavController) {
    val vm: CaptainViewModel = viewModel()
    val stats by vm.stats.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().background(PlixoInk).statusBarsPadding()) {
        item {
            PlixoTopBar(title = "Earnings", onBack = { navController.popBackStack() })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .background(Color.White.copy(0.07f), PlixoShape.Card)
                    .padding(24.dp),
            ) {
                Column {
                    Text(
                        "Total earnings",
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        color = Color.White.copy(0.6f),
                    )
                    Text(
                        "₹42,800",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        EarningPeriodCard("₹${stats.todayEarnings}", "Today", Modifier.weight(1f))
                        EarningPeriodCard("₹${stats.weekEarnings}", "This week", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                PlixoButton("Withdraw to UPI", onClick = {}, variant = PlixoButtonVariant.Lime)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EarningPeriodCard(amount: String, period: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(0.06f), PlixoShape.SmCard)
            .padding(14.dp),
    ) {
        Text(
            amount,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
        )
        Text(period, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = Color.White.copy(0.5f))
    }
}

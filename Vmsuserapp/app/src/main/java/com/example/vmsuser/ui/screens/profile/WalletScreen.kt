package com.example.vmsuser.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.models.WalletTransaction
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ProfileViewModel

@Composable
fun WalletScreen(navController: NavController) {
    val vm: ProfileViewModel = viewModel()
    val balance by vm.walletBalance.collectAsState()
    val transactions by vm.transactions.collectAsState()

    LaunchedEffect(Unit) { vm.loadTransactions() }

    LazyColumn(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        item {
            PlixoTopBar(title = "Wallet", onBack = { navController.popBackStack() })
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(PlixoInk, PlixoShape.Card)
                    .padding(24.dp),
            ) {
                Column {
                    Text("Available balance", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = Color.White.copy(0.6f))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Icon(Icons.Filled.MonetizationOn, null, tint = PlixoLime, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$balance",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 42.sp,
                            color = Color.White,
                            letterSpacing = (-1).sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("coins", fontFamily = PlusJakartaSans, fontSize = 16.sp, color = Color.White.copy(0.6f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Earned by completing matches",
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        color = Color.White.copy(0.5f),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            Text(
                "Transactions",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = PlixoText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        if (transactions.isEmpty()) {
            item {
                Text(
                    "No transactions yet — play and complete a match to earn coins.",
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    color = PlixoText2,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        } else {
            items(transactions) { tx ->
                TransactionRow(tx)
            }
        }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    val isCredit = tx.amount > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlixoSurface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isCredit) BlockMintBg else PlixoDangerLight, PlixoShape.Icon),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isCredit) Icons.Filled.Add else Icons.Filled.Remove,
                null,
                tint = if (isCredit) BlockMintFg else PlixoDanger,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.description, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PlixoText)
            Text(tx.createdAt, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText3)
        }
        Text(
            "${if (isCredit) "+" else ""}${tx.amount}",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (isCredit) BlockMintFg else PlixoDanger,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 74.dp)
            .height(1.dp)
            .background(PlixoBorder),
    )
}

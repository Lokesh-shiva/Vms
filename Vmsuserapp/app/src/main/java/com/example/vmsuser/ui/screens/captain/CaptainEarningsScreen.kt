package com.example.vmsuser.ui.screens.captain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
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
    val updatingUpi by vm.updatingUpi.collectAsState()
    val error by vm.error.collectAsState()
    var showUpiDialog by remember { mutableStateOf(false) }

    if (showUpiDialog) {
        EditUpiDialog(
            currentUpiId = stats.payoutUpiId,
            updating = updatingUpi,
            error = error,
            onDismiss = { showUpiDialog = false; vm.clearError() },
            onSave = { upiId -> vm.updatePayoutUpi(upiId) { showUpiDialog = false } },
        )
    }

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
                        "Wallet balance",
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        color = Color.White.copy(0.6f),
                    )
                    Text(
                        "₹${stats.walletBalance}",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        color = Color.White,
                    )
                    Text(
                        "Paid out manually by the Plixo team at day end",
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        color = Color.White.copy(0.45f),
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
                PlixoButton(
                    if (stats.payoutUpiId.isNullOrBlank()) "Add UPI ID for payouts" else "Edit UPI ID",
                    onClick = { showUpiDialog = true },
                    variant = PlixoButtonVariant.Lime,
                )
            }
            if (!stats.payoutUpiId.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Payouts go to: ${stats.payoutUpiId}",
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    color = Color.White.copy(0.5f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EditUpiDialog(
    currentUpiId: String?,
    updating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var upiId by remember { mutableStateOf(currentUpiId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payout UPI ID") },
        text = {
            Column {
                Text(
                    "We'll send your earnings here at day end.",
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    color = PlixoText2,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID") },
                    placeholder = { Text("yourname@bank") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = PlixoDanger, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(upiId.trim()) },
                enabled = upiId.isNotBlank() && !updating,
            ) { Text(if (updating) "Saving…" else "Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !updating) { Text("Cancel") } },
    )
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

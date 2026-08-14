package com.example.vmsuser.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.example.vmsuser.models.OrderDto
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.PlixoPill
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ShopViewModel

@Composable
fun OrdersScreen(navController: NavController) {
    val vm: ShopViewModel = viewModel()
    val orders by vm.orders.collectAsState()
    val loading by vm.ordersLoading.collectAsState()

    LaunchedEffect(Unit) { vm.loadOrders() }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "My Orders", onBack = { navController.popBackStack() })

        when {
            loading && orders.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PlixoPrimary)
                }
            }
            orders.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ReceiptLong, null, tint = PlixoText3, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No orders yet", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoText)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(orders) { order ->
                        OrderCard(
                            order = order,
                            onClick = {
                                if (order.status == "PENDING_PAYMENT") {
                                    navController.navigate(Screen.OrderPayment.create(order.id))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderDto, onClick: () -> Unit) {
    val resumable = order.status == "PENDING_PAYMENT"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlixoSurface, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = resumable,
            ) { onClick() }
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(order.referenceCode, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
            StatusPill(order.status)
        }
        Spacer(Modifier.height(6.dp))
        order.items.forEach {
            Text("${it.quantity}× ${it.name}", fontFamily = PlusJakartaSans, fontSize = 12.5.sp, color = PlixoText2)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("₹${"%.0f".format(order.totalAmount)}", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoPrimary)
            if (resumable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Complete payment", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = PlixoPrimary)
                    Icon(Icons.Filled.ChevronRight, null, tint = PlixoPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bg, fg, label) = when (status) {
        "PAID" -> Triple(BlockMintBg, BlockMintFg, "PAID")
        "UNDER_REVIEW" -> Triple(Color(0xFFFBEFD8), Color(0xFF8A6D1E), "UNDER REVIEW")
        "REJECTED" -> Triple(Color(0xFFF0E3E3), PlixoDanger, "REJECTED")
        else -> Triple(PlixoSurface2, PlixoText2, "AWAITING PAYMENT")
    }
    PlixoPill(label, bg = bg, fg = fg)
}

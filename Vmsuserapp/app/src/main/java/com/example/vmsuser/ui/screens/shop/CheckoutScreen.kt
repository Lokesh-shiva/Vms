package com.example.vmsuser.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.PlixoButton
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ShopViewModel

@Composable
fun CheckoutScreen(navController: NavController) {
    val parentEntry = remember(navController) { navController.getBackStackEntry("shop_graph") }
    val vm: ShopViewModel = viewModel(parentEntry)

    val cart by vm.cart.collectAsState()
    val shopItems by vm.items.collectAsState()
    val placing by vm.placingOrder.collectAsState()
    val error by vm.orderError.collectAsState()

    val lines = cart.mapNotNull { (itemId, qty) ->
        shopItems.find { it.id == itemId }?.let { item -> Triple(item, qty, item.price * qty) }
    }
    val total = lines.sumOf { it.third }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Checkout", onBack = { navController.popBackStack() })

        if (cart.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your cart is empty.", fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText2)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(lines) { (item, qty, subtotal) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlixoSurface, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(item.name, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
                        Text("₹${"%.0f".format(item.price)} × $qty", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                    }
                    Text("₹${"%.0f".format(subtotal)}", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlixoText)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(PlixoSurface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(20.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PlixoText2)
                Text("₹${"%.0f".format(total)}", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PlixoText)
            }
            Spacer(Modifier.height(14.dp))

            if (error != null) {
                Text(error!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
                Spacer(Modifier.height(10.dp))
            }

            PlixoButton(
                if (placing) "Placing order…" else "Place order",
                onClick = {
                    vm.placeOrder { order ->
                        navController.navigate(Screen.OrderPayment.create(order.id)) {
                            popUpTo(Screen.Shop.route)
                        }
                    }
                },
                enabled = !placing,
            )
        }
    }
}

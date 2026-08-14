package com.example.vmsuser.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun OrderPaymentScreen(navController: NavController, orderId: Int) {
    val parentEntry = remember(navController) { navController.getBackStackEntry("shop_graph") }
    val vm: ShopViewModel = viewModel(parentEntry)

    val order by vm.lastOrder.collectAsState()
    val placing by vm.placingOrder.collectAsState()
    val error by vm.orderError.collectAsState()
    var transactionId by remember { mutableStateOf("") }

    LaunchedEffect(orderId) { if (order?.id != orderId) vm.loadOrder(orderId) }

    val submitted = order?.status == "UNDER_REVIEW" || order?.status == "PAID"

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Payment", onBack = { navController.popBackStack() })

        Column(modifier = Modifier.weight(1f).padding(20.dp)) {
            if (submitted) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, null, tint = PlixoPrimary, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Payment submitted", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = PlixoText)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "We'll confirm your order once it's reviewed.",
                        fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2,
                    )
                    Spacer(Modifier.height(24.dp))
                    PlixoButton("View my orders", onClick = {
                        navController.navigate(Screen.Orders.route) { popUpTo(Screen.Shop.route) { inclusive = true } }
                    })
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, RoundedCornerShape(20.dp)).padding(18.dp)) {
                    Text("Order reference", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                    Text(order?.referenceCode ?: "…", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoText)
                    Spacer(Modifier.height(10.dp))
                    Text("Amount", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                    Text("₹${"%.0f".format(order?.totalAmount ?: 0.0)}", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = PlixoPrimary)
                    order?.upiId?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("Pay to UPI ID", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                        Text(it, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PlixoText)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Pay via any UPI app using the details above, then enter the transaction ID below to confirm.",
                    fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("UPI transaction ID") },
                    shape = PlixoShape.Input,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    singleLine = true,
                )
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
                }
                Spacer(Modifier.height(18.dp))
                PlixoButton(
                    if (placing) "Submitting…" else "I've paid — submit",
                    onClick = { vm.submitPayment(orderId, transactionId) {} },
                    enabled = !placing && transactionId.isNotBlank(),
                )
            }
        }
    }
}

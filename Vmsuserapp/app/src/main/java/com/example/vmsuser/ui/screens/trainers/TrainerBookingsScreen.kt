package com.example.vmsuser.ui.screens.trainers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SportsTennis
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
import com.example.vmsuser.models.TrainerBookingDto
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.PlixoPill
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.TrainerViewModel

@Composable
fun TrainerBookingsScreen(navController: NavController) {
    val vm: TrainerViewModel = viewModel()
    val bookings by vm.bookings.collectAsState()
    val loading by vm.bookingsLoading.collectAsState()

    LaunchedEffect(Unit) { vm.loadBookings() }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "My Bookings", onBack = { navController.popBackStack() })

        when {
            loading && bookings.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PlixoPrimary)
                }
            }
            bookings.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SportsTennis, null, tint = PlixoText3, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No bookings yet", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoText)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(bookings) { booking ->
                        BookingCard(
                            booking = booking,
                            onClick = {
                                if (booking.status == "PENDING_PAYMENT") {
                                    navController.navigate(Screen.TrainerBookingPayment.create(booking.id))
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
private fun BookingCard(booking: TrainerBookingDto, onClick: () -> Unit) {
    val resumable = booking.status == "PENDING_PAYMENT"
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
            Text(booking.referenceCode, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
            StatusPill(booking.status)
        }
        Spacer(Modifier.height(6.dp))
        Text("${booking.sessionDate} · ${booking.sessionTime}", fontFamily = PlusJakartaSans, fontSize = 12.5.sp, color = PlixoText2)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("₹${"%.0f".format(booking.amount)}", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoPrimary)
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
        "CONFIRMED" -> Triple(BlockMintBg, BlockMintFg, "CONFIRMED")
        "UNDER_REVIEW" -> Triple(Color(0xFFFBEFD8), Color(0xFF8A6D1E), "UNDER REVIEW")
        "REJECTED" -> Triple(Color(0xFFF0E3E3), PlixoDanger, "REJECTED")
        else -> Triple(PlixoSurface2, PlixoText2, "AWAITING PAYMENT")
    }
    PlixoPill(label, bg = bg, fg = fg)
}

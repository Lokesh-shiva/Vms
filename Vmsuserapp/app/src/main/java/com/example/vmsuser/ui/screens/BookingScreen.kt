package com.example.vmsuser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vmsuser.viewmodel.BookingViewModel
import com.example.vmsuser.viewmodel.HomeViewModel

@Composable
fun BookingScreen(
    homeViewModel: HomeViewModel,
    bookingViewModel: BookingViewModel,
    onNavigateToPayment: (bookingId: Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Booking Screen")
    }
}

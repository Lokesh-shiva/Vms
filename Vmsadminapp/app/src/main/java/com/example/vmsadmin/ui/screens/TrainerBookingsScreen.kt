package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.AdminTrainerBooking
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.viewmodel.TrainerBookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerBookingsScreen(viewModel: TrainerBookingViewModel, onBack: () -> Unit = {}) {
    val bookings by viewModel.bookings.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val pendingIds by viewModel.pendingIds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadBookings() }
    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Coach Bookings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                loading && bookings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                bookings.isEmpty() -> {
                    PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.loadBookings() }, modifier = Modifier.fillMaxSize()) {
                        Text(
                            "No bookings awaiting review.",
                            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                else -> {
                    PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.loadBookings() }, modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(bookings, key = { it.id }) { booking ->
                                BookingCard(
                                    booking = booking,
                                    pending = booking.id in pendingIds,
                                    onApprove = { viewModel.approveBooking(booking.id) },
                                    onReject = { viewModel.rejectBooking(booking.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: AdminTrainerBooking, pending: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(status = booking.status ?: "UNKNOWN")
            Text("Booking #${booking.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        InfoRow(label = "Session", value = "${booking.session_date ?: "-"} · ${booking.session_time ?: "-"}")
        InfoRow(label = "Amount", value = "₹${"%.2f".format(booking.amount ?: 0.0)}")
        InfoRow(label = "Reference", value = booking.reference_code ?: "-")
        InfoRow(label = "UTR", value = booking.transaction_id ?: "-")

        if (booking.status == "UNDER_REVIEW") {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject, enabled = !pending, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Reject") }
                Button(
                    onClick = onApprove, enabled = !pending, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
                ) { Text(if (pending) "…" else "Confirm") }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

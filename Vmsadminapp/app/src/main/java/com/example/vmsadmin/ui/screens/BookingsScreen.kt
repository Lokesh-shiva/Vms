package com.example.vmsadmin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Booking
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(viewModel: BookingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadBookings()
        visible = true
    }

    // Confirmation dialog state
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogAction by remember { mutableStateOf({}) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle, style = MaterialTheme.typography.titleLarge) },
            text = { Text(dialogMessage, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        dialogAction()
                        showDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.bookings.isEmpty() -> {
                // Skeleton Loader
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        AppCard {
                            Box(modifier = Modifier.width(100.dp).height(24.dp).shimmerEffect())
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.width(150.dp).height(20.dp).shimmerEffect())
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp).shimmerEffect())
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp).shimmerEffect())
                        }
                    }
                }
            }
            uiState.error != null && uiState.bookings.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "Something went wrong",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadBookings() }) {
                        Text("Retry")
                    }
                }
            }
            uiState.bookings.isEmpty() -> {
                Text(
                    text = "No bookings available",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshBookings() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.bookings, key = { it.id }) { booking ->
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(400))
                            ) {
                                BookingCard(
                                    booking = booking,
                                    onStart = {
                                        dialogTitle = "Start Service?"
                                        dialogMessage = "Start service for booking #${booking.id}? This will mark it as in progress."
                                        dialogAction = { viewModel.startBooking(booking.id) }
                                        showDialog = true
                                    },
                                    onComplete = {
                                        dialogTitle = "Complete Service?"
                                        dialogMessage = "Mark booking #${booking.id} as completed?"
                                        dialogAction = { viewModel.completeBooking(booking.id) }
                                        showDialog = true
                                    },
                                    onCancel = {
                                        dialogTitle = "Cancel Booking?"
                                        dialogMessage = "Cancel booking #${booking.id}? This action cannot be undone."
                                        dialogAction = { viewModel.cancelBooking(booking.id) }
                                        showDialog = true
                                    }
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
private fun BookingCard(
    booking: Booking,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    AppCard {
        // Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(status = booking.status)
            Text(
                text = "Booking #${booking.id}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoRow(label = "Region", value = booking.region_name ?: "Region ${booking.region_id ?: "-"}")
        InfoRow(label = "Cart Type", value = booking.cart_type_name ?: "Type ${booking.cart_type_id ?: "-"}")
        InfoRow(label = "Timeslot", value = booking.timeslot_label ?: "Slot ${booking.timeslot_id ?: "-"}")
        InfoRow(label = "Date", value = booking.date ?: "-")
        val cartDisplay = booking.cart_label
            ?: if (booking.assigned_cart_id != null) "CART-${booking.assigned_cart_id}" else null
        if (cartDisplay != null) {
            InfoRow(label = "Cart", value = cartDisplay)
        }

        // Action buttons based on status
        val status = booking.status.uppercase()
        if (status in listOf("PENDING_PAYMENT", "CONFIRMED", "IN_PROGRESS")) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (status) {
                    "PENDING_PAYMENT" -> {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel Booking")
                        }
                    }
                    "CONFIRMED" -> {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel Booking")
                        }
                        Button(
                            onClick = onStart,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Start Service")
                        }
                    }
                    "IN_PROGRESS" -> {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Complete Service")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

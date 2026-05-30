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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Payment
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.PaymentFilter
import com.example.vmsadmin.viewmodel.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(viewModel: PaymentViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadPayments()
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

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Revenue summary + filter tabs (always visible) ───────────────
        RevenueSummaryCard(
            totalRevenue = uiState.totalRevenue,
            pendingReviewCount = uiState.pendingReviewCount
        )
        PaymentFilterTabs(
            selected = uiState.filter,
            onSelect = { viewModel.setFilter(it) }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && uiState.payments.isEmpty() -> {
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
                uiState.error != null && uiState.payments.isEmpty() -> {
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
                        Button(onClick = { viewModel.loadPayments() }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.payments.isEmpty() -> {
                    Text(
                        text = when (uiState.filter) {
                            PaymentFilter.PENDING_REVIEW -> "No payments pending review"
                            PaymentFilter.ALL -> "No payments found"
                        },
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { viewModel.loadPayments() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.payments, key = { it.id }) { payment ->
                                PaymentCard(
                                    payment = payment,
                                    onApprove = {
                                        dialogTitle = "Approve Payment?"
                                        dialogMessage = "Approve payment #${payment.id} for ₹${payment.amount ?: "0.00"}?"
                                        dialogAction = { viewModel.approvePayment(payment.id) }
                                        showDialog = true
                                    },
                                    onReject = {
                                        dialogTitle = "Reject Payment?"
                                        dialogMessage = "Reject payment #${payment.id}? This action cannot be undone."
                                        dialogAction = { viewModel.rejectPayment(payment.id) }
                                        showDialog = true
                                    },
                                    onRefund = {
                                        dialogTitle = "Refund Payment?"
                                        dialogMessage = "Refund payment #${payment.id} for ₹${payment.amount ?: "0.00"}? This cannot be undone."
                                        dialogAction = { viewModel.refundPayment(payment.id) }
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
private fun RevenueSummaryCard(totalRevenue: Double, pendingReviewCount: Int) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Collected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${"%,.2f".format(totalRevenue)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Pending Review",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$pendingReviewCount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (pendingReviewCount > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PaymentFilterTabs(
    selected: PaymentFilter,
    onSelect: (PaymentFilter) -> Unit
) {
    val tabs = listOf(
        PaymentFilter.PENDING_REVIEW to "Pending Review",
        PaymentFilter.ALL to "All Payments"
    )
    TabRow(selectedTabIndex = tabs.indexOfFirst { it.first == selected }) {
        tabs.forEach { (filter, label) ->
            Tab(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                text = { Text(label) }
            )
        }
    }
}

@Composable
private fun PaymentCard(
    payment: Payment,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRefund: () -> Unit
) {
    AppCard {
        // Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(status = payment.status ?: "UNKNOWN")
            Text(
                text = "Payment #${payment.id}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoRow(label = "Booking ID", value = "#${payment.booking_id ?: "-"}")
        InfoRow(label = "Amount", value = "₹${payment.amount ?: "0.00"}")
        InfoRow(label = "Reference Code", value = payment.reference_code ?: "-")
        InfoRow(label = "UTR", value = payment.transaction_id ?: "-")

        // Status-aware actions
        when (payment.status) {
            "UNDER_REVIEW" -> {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reject")
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Approve")
                    }
                }
            }
            "SUCCESS" -> {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onRefund,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Refund")
                }
            }
            else -> Unit // terminal/other statuses: no actions
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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

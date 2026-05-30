package com.example.vmsuser.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vmsuser.models.UiState
import com.example.vmsuser.viewmodel.PaymentViewModel

@Composable
fun PaymentScreen(
    bookingId: Int,
    paymentViewModel: PaymentViewModel,
    onNavigateToStatus: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val initiateState by paymentViewModel.initiateState.collectAsState()
    val confirmState by paymentViewModel.confirmState.collectAsState()
    var transactionId by remember { mutableStateOf("") }
    var upiLaunched by remember { mutableStateOf(false) }

    // UPI app launcher — returns after user comes back from UPI app
    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        upiLaunched = true
    }

    // Auto-initiate payment when screen opens
    LaunchedEffect(bookingId) {
        paymentViewModel.initiatePayment(bookingId)
    }

    // Navigate to status when confirmation succeeds
    LaunchedEffect(confirmState) {
        if (confirmState is UiState.Success) {
            paymentViewModel.resetConfirmState()
            onNavigateToStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Payment Screen")
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = initiateState) {
            is UiState.Loading -> CircularProgressIndicator()

            is UiState.Error -> {
                Text("Error: ${state.message}")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { paymentViewModel.initiatePayment(bookingId) }) {
                    Text("Retry")
                }
            }

            is UiState.Success -> {
                val paymentData = state.data
                Text("Amount: ₹${paymentData.amount}")
                Text("Reference: ${paymentData.reference_code}")
                Spacer(modifier = Modifier.height(16.dp))

                if (!upiLaunched) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentData.upi_link))
                            upiLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pay ₹${paymentData.amount} via UPI")
                    }
                } else {
                    OutlinedTextField(
                        value = transactionId,
                        onValueChange = { transactionId = it },
                        label = { Text("Enter UPI Transaction ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isSubmitting = confirmState is UiState.Loading
                    Button(
                        onClick = {
                            paymentViewModel.submitTransactionId(bookingId, transactionId)
                        },
                        enabled = !isSubmitting && transactionId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator()
                        } else {
                            Text("Submit Transaction ID")
                        }
                    }

                    if (confirmState is UiState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error: ${(confirmState as UiState.Error).message}")
                    }
                }
            }

            else -> {}
        }
    }
}

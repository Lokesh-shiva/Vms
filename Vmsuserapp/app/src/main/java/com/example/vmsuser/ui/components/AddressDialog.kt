package com.example.vmsuser.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vmsuser.data.AddressManager
import com.example.vmsuser.models.Address
import kotlinx.coroutines.launch

@Composable
fun AddressDialog(
    currentAddress: Address,
    addressManager: AddressManager,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentAddress.name) }
    var phone by remember { mutableStateOf(currentAddress.phone) }
    var address by remember { mutableStateOf(currentAddress.address) }
    var pincode by remember { mutableStateOf(currentAddress.pincode) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery Address") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = null
                    },
                    label = { Text("Phone") },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        addressError = null
                    },
                    label = { Text("Address") },
                    isError = addressError != null,
                    supportingText = addressError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("Pincode (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validation
                    var valid = true

                    if (name.isBlank()) {
                        nameError = "Name is required"
                        valid = false
                    }
                    if (phone.isBlank() || phone.length < 10) {
                        phoneError = "Enter a valid 10-digit phone number"
                        valid = false
                    }
                    if (address.isBlank()) {
                        addressError = "Address is required"
                        valid = false
                    }

                    if (!valid) return@Button

                    scope.launch {
                        addressManager.saveAddress(
                            Address(
                                name = name.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                pincode = pincode.trim()
                            )
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

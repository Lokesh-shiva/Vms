package com.example.vmsuser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vmsuser.data.AddressManager
import com.example.vmsuser.models.Address
import com.example.vmsuser.models.UiState
import com.example.vmsuser.ui.components.AddressDialog
import com.example.vmsuser.viewmodel.BookingViewModel
import com.example.vmsuser.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    homeViewModel: HomeViewModel,
    bookingViewModel: BookingViewModel,
    addressManager: AddressManager,
    onNavigateToBookingStatus: (bookingId: Int) -> Unit,
    onBack: () -> Unit
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val createState by bookingViewModel.createState.collectAsState()
    val savedAddress by addressManager.addressFlow.collectAsState(initial = Address())

    var showAddressDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cartItems = homeViewModel.getCartItems()
    val totalAmount = homeViewModel.getTotalAmount()
    val isSubmitting = createState is UiState.Loading

    // Handle booking success / error
    LaunchedEffect(createState) {
        when (val state = createState) {
            is UiState.Success -> {
                homeViewModel.clearCart()
                bookingViewModel.resetCreateState()
                onNavigateToBookingStatus(state.data.id)
            }
            is UiState.Error -> {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = state.message,
                        actionLabel = "Retry"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // User can retry manually
                    }
                }
                bookingViewModel.resetCreateState()
            }
            else -> {}
        }
    }

    if (showAddressDialog) {
        AddressDialog(
            currentAddress = savedAddress,
            addressManager = addressManager,
            onDismiss = { showAddressDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        if (cartItems.isEmpty()) {
            // ── Empty state ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your cart is empty 🛒",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add items to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text("Browse Items")
                    }
                }
            }
        } else {
            // ── Cart content ────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cart items
                items(
                    items = cartItems,
                    key = { it.first.id }
                ) { (item, qty) ->
                    CartItemCard(
                        itemName = item.name,
                        price = item.price,
                        quantity = qty,
                        onAdd = { homeViewModel.addItem(item.id) },
                        onRemove = { homeViewModel.removeItem(item.id) }
                    )
                }

                // Divider
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Summary
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${totalAmount.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Divider
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Address section
                item {
                    AddressSection(
                        address = savedAddress,
                        onEditAddress = { showAddressDialog = true }
                    )
                }

                // Spacer
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Place Booking button
                item {
                    Button(
                        onClick = {
                            if (isSubmitting) return@Button

                            // 1. Validate cart not empty
                            if (cartItems.isEmpty()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Cart is empty")
                                }
                                return@Button
                            }

                            // 2. Validate single cart type
                            val cartTypeIds = homeViewModel.getCartTypeIds()
                            if (cartTypeIds.size > 1) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please order items from one category at a time")
                                }
                                return@Button
                            }

                            // 3. Validate address
                            val addr = savedAddress
                            val fullAddress = buildString {
                                if (addr.name.isNotBlank()) append("${addr.name}, ")
                                append(addr.address)
                                if (addr.pincode.isNotBlank()) append(" - ${addr.pincode}")
                                if (addr.phone.isNotBlank()) append(" (Ph: ${addr.phone})")
                            }.trim()

                            if (addr.address.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please add your delivery address")
                                }
                                return@Button
                            }

                            // 4. Get region_id (first available)
                            val regionId = homeState.regions.firstOrNull()?.id
                            if (regionId == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No delivery region available")
                                }
                                return@Button
                            }

                            // 5. Get cart_type_id
                            val cartTypeId = cartTypeIds.firstOrNull()
                            if (cartTypeId == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Could not determine cart type")
                                }
                                return@Button
                            }

                            // 6. Get timeslot_id (first available)
                            val timeslotId = homeState.timeslots.firstOrNull { it.is_active }?.id
                            if (timeslotId == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No timeslots available")
                                }
                                return@Button
                            }

                            // 7. Create booking
                            bookingViewModel.createBooking(
                                regionId = regionId,
                                cartTypeId = cartTypeId,
                                timeslotId = timeslotId,
                                address = fullAddress
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isSubmitting && cartItems.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Placing Booking...")
                        } else {
                            Text(
                                "Place Booking • ₹${totalAmount.toInt()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Cart Item Card ──────────────────────────────────────────────────────────

@Composable
private fun CartItemCard(
    itemName: String,
    price: Double,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Name + price
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${price.toInt()} × $quantity = ₹${(price * quantity).toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantity controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        "−",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        "+",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ── Address Section ─────────────────────────────────────────────────────────

@Composable
private fun AddressSection(
    address: Address,
    onEditAddress: () -> Unit
) {
    val hasAddress = address.address.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delivery Address",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEditAddress) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Address",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (hasAddress) {
                Spacer(modifier = Modifier.height(8.dp))
                if (address.name.isNotBlank()) {
                    Text(
                        text = address.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (address.phone.isNotBlank()) {
                    Text(
                        text = "📞 ${address.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = address.address,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (address.pincode.isNotBlank()) {
                    Text(
                        text = "PIN: ${address.pincode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEditAddress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Address")
                }
            }
        }
    }
}

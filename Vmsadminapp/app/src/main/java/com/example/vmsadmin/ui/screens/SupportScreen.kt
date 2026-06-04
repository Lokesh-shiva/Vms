package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.Booking
import com.example.vmsadmin.models.CreateDisputeRequest
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.DisputeViewModel
import com.example.vmsadmin.viewmodel.SearchStatus
import com.example.vmsadmin.viewmodel.UserManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    userManagementViewModel: UserManagementViewModel,
    bookingViewModel: BookingViewModel,
    disputeViewModel: DisputeViewModel
) {
    val searchStatus by userManagementViewModel.searchStatus.collectAsState()
    val searchResult by userManagementViewModel.searchResult.collectAsState()
    val bookingState by bookingViewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (bookingState.bookings.isEmpty()) bookingViewModel.loadBookings()
    }

    DisposableEffect(Unit) {
        onDispose { userManagementViewModel.clearSearch() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Support", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── User lookup ──────────────────────────────────────────────
            item {
                Text(
                    "User Lookup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        if (it.isBlank()) userManagementViewModel.clearSearch()
                    },
                    label = { Text("Search by phone number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        userManagementViewModel.searchByPhone(phone)
                    }),
                    trailingIcon = {
                        if (searchStatus is SearchStatus.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = {
                                keyboard?.hide()
                                userManagementViewModel.searchByPhone(phone)
                            }) {
                                Icon(Icons.Outlined.Search, contentDescription = "Search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Search result ────────────────────────────────────────────
            when (searchStatus) {
                is SearchStatus.Found -> searchResult?.let { user ->
                    item { UserResultCard(user) }
                }
                is SearchStatus.NotFound -> item {
                    Text(
                        "No user found with that number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                is SearchStatus.Error -> item {
                    Text(
                        (searchStatus as SearchStatus.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                else -> Unit
            }

            // ── Recent bookings ──────────────────────────────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Recent Bookings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            when {
                bookingState.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                bookingState.bookings.isEmpty() -> item {
                    Text(
                        "No bookings found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(bookingState.bookings.take(20)) { booking ->
                    SupportBookingCard(
                        booking = booking,
                        onRaiseTicket = { title, description ->
                            disputeViewModel.createDispute(
                                CreateDisputeRequest(
                                    title = title,
                                    description = description,
                                    booking_id = booking.id,
                                    user_id = null
                                )
                            )
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SupportBookingCard(
    booking: Booking,
    onRaiseTicket: (String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var ticketTitle by remember { mutableStateOf("") }
    var ticketDescription by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Raise Ticket") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Booking #${booking.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = ticketTitle,
                        onValueChange = { ticketTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ticketDescription,
                        onValueChange = { ticketDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRaiseTicket(ticketTitle.trim(), ticketDescription.trim())
                        ticketTitle = ""
                        ticketDescription = ""
                        showDialog = false
                    },
                    enabled = ticketTitle.isNotBlank() && ticketDescription.isNotBlank()
                ) { Text("Submit") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    ticketTitle = ""
                    ticketDescription = ""
                    showDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    AppCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Booking #${booking.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookingStatusBadge(booking.status)
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { showDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "Raise Ticket",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOfNotNull(booking.cart_type_name, booking.region_name)
                    .joinToString(" · ").ifEmpty { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            booking.date?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UserResultCard(user: AppUser) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    user.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    user.role.replace("_", " ").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (user.is_active)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (user.is_active) "ACTIVE" else "INACTIVE",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.is_active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BookingStatusBadge(status: String?) {
    val (bg, fg) = when (status?.uppercase()) {
        "CONFIRMED", "COMPLETED" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.primary
        "PENDING"                -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.tertiary
        "CANCELLED"              -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f) to MaterialTheme.colorScheme.error
        else                     -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = status ?: "—",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Booking
import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.viewmodel.BookingViewModel
import com.example.vmsadmin.viewmodel.GroundViewModel

/**
 * Dedicated panel for GROUND_OWNER role.
 *
 * Shows:
 *  - Grounds in their assigned region (read-only status view)
 *  - Today's bookings for their region
 *
 * Data isolation is enforced server-side: the backend filters by the user's region_id
 * on both GET /bookings and GET /grounds automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroundOwnerScreen(
    groundViewModel: GroundViewModel,
    bookingViewModel: BookingViewModel
) {
    val groundState by groundViewModel.uiState.collectAsState()
    val grounds = groundState.grounds
    val groundsLoading = groundState.isLoading
    val groundsError = groundState.error

    val bookingState by bookingViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        groundViewModel.loadGrounds()
        bookingViewModel.loadBookings()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("My Grounds", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Grounds summary ──────────────────────────────────────────
            item {
                Text(
                    "Grounds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            when {
                groundsLoading && grounds.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                groundsError != null && grounds.isEmpty() -> item {
                    Text(
                        groundsError ?: "Failed to load grounds",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                grounds.isEmpty() -> item {
                    Text(
                        "No grounds assigned to your region.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(grounds, key = { it.id }) { ground ->
                    GroundCard(
                        ground = ground,
                        updating = ground.id in groundState.updatingIds,
                        onToggleActive = { isActive -> groundViewModel.toggleGround(ground.id, isActive) },
                    )
                }
            }

            // ── Region bookings ──────────────────────────────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Region Bookings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            when {
                bookingState.isLoading && bookingState.bookings.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                bookingState.bookings.isEmpty() -> item {
                    Text(
                        "No bookings in your region.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(bookingState.bookings.take(30), key = { it.id }) { booking ->
                    RegionBookingCard(booking)
                }
            }
        }
    }
}

@Composable
private fun GroundCard(
    ground: Ground,
    updating: Boolean,
    onToggleActive: (Boolean) -> Unit,
) {
    val statusColor = when (ground.status.uppercase()) {
        "AVAILABLE" -> Color(0xFF4CAF50)
        "BUSY"      -> Color(0xFFE65100)
        else        -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ground.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (ground.is_active) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = ground.status.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(12.dp))
            if (updating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Switch(checked = ground.is_active, onCheckedChange = onToggleActive)
            }
        }
    }
}

@Composable
private fun RegionBookingCard(booking: Booking) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Booking #${booking.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
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
            StatusBadge(status = booking.status)
        }
    }
}

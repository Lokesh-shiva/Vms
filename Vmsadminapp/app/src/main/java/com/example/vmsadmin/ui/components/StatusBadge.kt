package com.example.vmsadmin.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = statusColors(status)

    Surface(
        color = backgroundColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = friendlyStatusLabel(status),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private fun friendlyStatusLabel(status: String): String {
    return when (status.uppercase()) {
        // Booking statuses
        "PENDING_PAYMENT"    -> "Awaiting Payment"
        "CONFIRMED"          -> "Confirmed"
        "IN_PROGRESS"        -> "In Progress"
        "COMPLETED"          -> "Completed"
        "CANCELLED"          -> "Cancelled"
        "EXPIRED"            -> "Expired"
        // Payment statuses
        "UNDER_REVIEW"       -> "Under Review"
        "SUCCESS"            -> "Paid"
        "FAILED"             -> "Failed"
        "PENDING"            -> "Pending"
        "REJECTED"           -> "Rejected"
        "APPROVED"           -> "Approved"
        "REFUNDED"           -> "Refunded"
        // Match statuses
        "OPEN"               -> "Open"
        "FULL"               -> "Full"
        "MATCHED"            -> "Matched"
        "CANCELLED_NO_SHOW"  -> "No Show"
        else                 -> status.replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

private fun statusColors(status: String): Pair<Color, Color> {
    return when (status.uppercase()) {
        "PENDING_PAYMENT" -> Color(0xFFFF9800) to Color(0xFFFF9800)      // Orange
        "UNDER_REVIEW"    -> Color(0xFF9C27B0) to Color(0xFF9C27B0)      // Purple
        "CONFIRMED"       -> Color(0xFF4CAF50) to Color(0xFF4CAF50)      // Green
        "IN_PROGRESS"     -> Color(0xFF2196F3) to Color(0xFF2196F3)      // Blue
        "COMPLETED"       -> Color(0xFF9E9E9E) to Color(0xFF9E9E9E)      // Grey
        "CANCELLED"       -> Color(0xFFF44336) to Color(0xFFF44336)      // Red
        "EXPIRED"         -> Color(0xFF616161) to Color(0xFF616161)      // Dark Grey
        "SUCCESS"         -> Color(0xFF4CAF50) to Color(0xFF4CAF50)      // Green
        "FAILED"          -> Color(0xFFF44336) to Color(0xFFF44336)      // Red
        "PENDING"         -> Color(0xFFFF9800) to Color(0xFFFF9800)      // Orange
        "REJECTED"           -> Color(0xFFF44336) to Color(0xFFF44336)   // Red
        "APPROVED"           -> Color(0xFF4CAF50) to Color(0xFF4CAF50)   // Green
        "REFUNDED"           -> Color(0xFF9C27B0) to Color(0xFF9C27B0)   // Purple
        "UNDER_REVIEW"       -> Color(0xFF9C27B0) to Color(0xFF9C27B0)   // Purple
        // Match statuses
        "OPEN"               -> Color(0xFF00BCD4) to Color(0xFF00BCD4)   // Cyan
        "FULL"               -> Color(0xFFFF9800) to Color(0xFFFF9800)   // Orange
        "MATCHED"            -> Color(0xFF4CAF50) to Color(0xFF4CAF50)   // Green
        "CANCELLED_NO_SHOW"  -> Color(0xFFF44336) to Color(0xFFF44336)   // Red
        else                 -> Color(0xFF9E9E9E) to Color(0xFF9E9E9E)   // Default
    }
}
